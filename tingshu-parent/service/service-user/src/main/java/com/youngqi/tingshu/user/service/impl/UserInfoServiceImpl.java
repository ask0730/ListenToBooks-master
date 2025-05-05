package com.youngqi.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.youngqi.tingshu.album.AlbumFeignClient;
import com.youngqi.tingshu.common.cache.YoungQiCache;
import com.youngqi.tingshu.common.constant.KafkaConstant;
import com.youngqi.tingshu.common.constant.RedisConstant;
import com.youngqi.tingshu.common.constant.SystemConstant;
import com.youngqi.tingshu.common.service.KafkaService;
import com.youngqi.tingshu.common.util.AuthContextHolder;
import com.youngqi.tingshu.common.util.MongoUtil;
import com.youngqi.tingshu.model.album.AlbumInfo;
import com.youngqi.tingshu.model.album.TrackInfo;
import com.youngqi.tingshu.model.user.*;
import com.youngqi.tingshu.user.mapper.*;
import com.youngqi.tingshu.user.service.UserInfoService;
import com.youngqi.tingshu.user.service.VipServiceConfigService;
import com.youngqi.tingshu.user.strategy.ItemTypeStrategy;
import com.youngqi.tingshu.user.strategy.StrategyFactory;
import com.youngqi.tingshu.vo.base.PageVo;
import com.youngqi.tingshu.vo.user.UserInfoVo;
import com.youngqi.tingshu.vo.user.UserPaidRecordVo;
import com.youngqi.tingshu.vo.user.UserSubscribeVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youngqi.tingshu.model.user.*;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private WxMaService wxMaService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private KafkaService kafkaService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;
    @Autowired
    private UserVipServiceMapper userVipServiceMapper;




    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<String, String> wxLogin(String code) {
        try {
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
            if (sessionInfo != null) { //是真用户
                String openid = sessionInfo.getOpenid(); //拿到唯一标识openId
                //根据微信标识查看是否注册
                QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("wx_open_id",openid);
                UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
                if (userInfo==null){//没注册过
                    userInfo = new UserInfo();
                    userInfo.setWxOpenId(openid);
                    userInfo.setNickname("听众"+ IdUtil.getSnowflakeNextIdStr());
                    userInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
                    userInfo.setIsVip(0);
                    userInfoMapper.insert(userInfo);
                    //发送kafka消息，提醒余额微服务账户初始化
                    kafkaService.sendMessage(KafkaConstant.QUEUE_USER_REGISTER,userInfo.getId().toString());

                }
                //注册过
                String  token=IdUtil.fastUUID();
                String  loginKey= RedisConstant.USER_LOGIN_KEY_PREFIX+token;
                UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
                redisTemplate.opsForValue().set(loginKey,userInfoVo,RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);
                Map<String,String> resultMap=new HashMap<>();
                resultMap.put("token",token);
                return resultMap;
            }

        } catch (WxErrorException e) {
           log.error("[用户服务]微信登录异常：{}", e);
           throw new RuntimeException();
        }
        return null;

    }

    @Override
    @YoungQiCache(prefix = "userInfoVo:")
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVo getUserInfo(Long userId) {
        UserInfo userInfo = userInfoMapper.selectById(userId);
        UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
        return userInfoVo;
    }

    /**
     * 修改用户基本信息
     *
     * @param userInfoVo
     *使用延迟双删除:保证缓存一致性问题
     */
    @Override
    public void updateUser(UserInfoVo userInfoVo) {
        //删缓存
        String key="userInfoVo:"+userInfoVo.getId();
        redisTemplate.delete(key);
        //更新
        UserInfo userInfo = new UserInfo();
        userInfo.setId(userInfoVo.getId());
        userInfo.setAvatarUrl(userInfoVo.getAvatarUrl());
        userInfo.setNickname(userInfoVo.getNickname());
        userInfoMapper.updateById(userInfo);
        //睡眠一段时间(确保并发读操作把db"脏"数据放入缓存)，删缓存
        try {
            TimeUnit.SECONDS.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        redisTemplate.delete(key);
    }



    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;
    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;
    /*
    *
    * 判断当前用户某一页中声音列表购买情况
    * */
    @Override
    public Map<Long, Integer> getUserIsPaidTrack(Long userId, Long albumId, List<Long> trackCheckIdList) {
        //查已购专辑表      购买过-->全部为 1    没购买--> 查声音列表
        QueryWrapper<UserPaidAlbum> userPaidAlbumQueryWrapper = new QueryWrapper<>();
        userPaidAlbumQueryWrapper.eq("user_id",userId).eq("album_id",albumId);
        Long paidAlbumCount = userPaidAlbumMapper.selectCount(userPaidAlbumQueryWrapper); //查数量就可
        if (paidAlbumCount>0){
           Map<Long, Integer> mapResult = new HashMap<>();
            for (Long  trackId: trackCheckIdList) {
                mapResult.put(trackId,1);
            }
            return mapResult;
        }
        //没买过专辑，查已购声音表
        QueryWrapper<UserPaidTrack> userPaidTrackQueryWrapper = new QueryWrapper<>();
        userPaidTrackQueryWrapper.eq("user_id",userId);
        userPaidTrackQueryWrapper.in("track_id",trackCheckIdList);
        List<UserPaidTrack> userPaidTracks = userPaidTrackMapper.selectList(userPaidTrackQueryWrapper);//获取本页中已购买声音列表

        if (CollectionUtil.isNotEmpty(userPaidTracks)){
            //查到了---->循环修改
            List<Long> userPaidTrackList = userPaidTracks.stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
            Map<Long, Integer> resMap = new HashMap<>();
            for (Long needCheckTrackId : trackCheckIdList) {
                if (userPaidTrackList.contains(needCheckTrackId)){
                    resMap.put(needCheckTrackId,1);
                }else {
                    resMap.put(needCheckTrackId,0);
                }
            }
            return resMap;
        }

        //都没查到--->将待检查的列表全设为0
        Map<Long, Integer> mapResult = new HashMap<>();
        for (Long  trackId: trackCheckIdList) {
            mapResult.put(trackId,0);
        }




        return mapResult;
    }

    @Override
    public Boolean subscribe(Long userId, Long albumId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId).and("albumId").is(albumId));
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_SUBSCRIBE, userId);
        UserSubscribe userSubscribe = mongoTemplate
                .findOne(query, UserSubscribe.class, collectionName);
        if (userSubscribe == null){
            userSubscribe = new UserSubscribe();
            userSubscribe.setUserId(userId);
            userSubscribe.setAlbumId(albumId);
            userSubscribe.setCreateTime(new Date());
            mongoTemplate.save(userSubscribe,collectionName);
            return true;
        }
        mongoTemplate.remove(userSubscribe,collectionName);
        return false;


    }

    @Override
    public Boolean isSubscribe(Long userId, Long albumId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId).and("albumId").is(albumId));
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_SUBSCRIBE, userId);
        UserSubscribe userSubscribe = mongoTemplate
                .findOne(query, UserSubscribe.class, collectionName);
        if (userSubscribe!=null){
            return true;
        }
        return false;
    }


    /*
     * 收藏声音
     * */
    @Override
    public Boolean collect(Long userId, Long trackId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId).and("trackId").is(trackId));
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId);
        UserCollect userCollect = mongoTemplate.findOne(query, UserCollect.class,collectionName);

        if (userCollect == null){
            userCollect=new UserCollect();
            userCollect.setUserId(userId);
            userCollect.setTrackId(trackId);
            userCollect.setCreateTime(new Date());
            mongoTemplate.save(userCollect,collectionName);
            return true;
        }
        mongoTemplate.remove(userCollect,collectionName);
        return false;
    }

    /*
     * 查询是否收藏声音
     * */
    @Override
    public Boolean isCollect(Long userId, Long trackId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId).and("trackId").is(trackId));
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId);
        UserCollect userCollect = mongoTemplate.findOne(query, UserCollect.class, collectionName);
        if (userCollect==null){
            return false;
        }
        return true;
    }

    @Override
    public PageVo<UserSubscribeVo> findUserSubscribePage(Integer page, Integer limit, Long userId) {
        PageVo<UserSubscribeVo> pageVo = new PageVo<>();
        pageVo.setCurrent(page.longValue());
        pageVo.setSize(limit.longValue());
        Query query = new Query();
        //当前页的开始（！！！！当前页码）
        int concurrentCount=page-1;
        //当前页的结束

        query.addCriteria(Criteria.where("userId").is(userId)).with(PageRequest.of(concurrentCount,limit));
        String collectionName =MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_SUBSCRIBE,userId);
        List<UserSubscribe> userSubscribes = mongoTemplate.find(query, UserSubscribe.class, collectionName);
        List<UserSubscribeVo> userSubscribeVoList= userSubscribes.stream()
                .map(userSubscribe -> {
                    AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(userSubscribe.getAlbumId()).getData();
                    UserSubscribeVo userSubscribeVo = BeanUtil.copyProperties(userSubscribe, UserSubscribeVo.class);
                    userSubscribeVo.setCoverUrl(albumInfo.getCoverUrl());
                    userSubscribeVo.setAlbumTitle(albumInfo.getAlbumTitle());
                    userSubscribeVo.setIsFinished(albumInfo.getIsFinished());
                    userSubscribeVo.setIncludeTrackCount(albumInfo.getIncludeTrackCount());
                    return userSubscribeVo;
                })
                .collect(Collectors.toList());
        pageVo.setRecords(userSubscribeVoList);

        return pageVo;
    }

    @Override
    public Boolean isPaidAlbum(Long albumId) {
        Long userId = AuthContextHolder.getUserId();
        //2.构建查询条件：用户ID+专辑ID 查询专辑购买记录表
        LambdaQueryWrapper<UserPaidAlbum> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserPaidAlbum::getUserId, userId);
        queryWrapper.eq(UserPaidAlbum::getAlbumId, albumId);
        Long count = userPaidAlbumMapper.selectCount(queryWrapper);
        return count > 0;
    }

    @Override
    public List<Long> findUserPaidTrackList(Long userId, Long albumId) {
        LambdaQueryWrapper<UserPaidTrack> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserPaidTrack::getAlbumId,albumId);
        queryWrapper.eq(UserPaidTrack::getUserId,userId);
        List<UserPaidTrack> userPaidTracks = userPaidTrackMapper.selectList(queryWrapper);

        if (CollectionUtil.isNotEmpty(userPaidTracks)){
            List<Long> userPaidTrackList = userPaidTracks.stream().map(userPaidTrack -> userPaidTrack.getTrackId()).collect(Collectors.toList());
            return userPaidTrackList;
        }
        return null;
    }

    @Autowired
    private StrategyFactory strategyFactory;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        String itemType = userPaidRecordVo.getItemType();
        ItemTypeStrategy itemTypeStrategy = strategyFactory.getItemTypeStrategy(itemType);
        itemTypeStrategy.savePaidRecord(userPaidRecordVo);
//        if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(itemType)) {
//            //查询专辑购买记录
//            LambdaQueryWrapper<UserPaidAlbum> userPaidAlbumLambdaQueryWrapper = new LambdaQueryWrapper<>();
//            userPaidAlbumLambdaQueryWrapper.eq(UserPaidAlbum::getOrderNo,userPaidRecordVo.getOrderNo());//查流水号
//            Long count = userPaidAlbumMapper.selectCount(userPaidAlbumLambdaQueryWrapper);
//            if (count>0){
//                return;
//            }
//            //没买过
//            UserPaidAlbum userPaidAlbum = new UserPaidAlbum();
//            userPaidAlbum.setOrderNo(userPaidRecordVo.getOrderNo());
//            userPaidAlbum.setUserId(userPaidRecordVo.getUserId());
//            userPaidAlbum.setAlbumId(userPaidRecordVo.getItemIdList().get(0));
//            userPaidAlbumMapper.insert(userPaidAlbum);
//        }else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(itemType)){
//            LambdaQueryWrapper<UserPaidTrack> userPaidTrackLambdaQueryWrapper = new LambdaQueryWrapper<>();
//            userPaidTrackLambdaQueryWrapper.eq(UserPaidTrack::getOrderNo,userPaidRecordVo.getOrderNo());
//            Long count = userPaidTrackMapper.selectCount(userPaidTrackLambdaQueryWrapper);
//            if (count>0){
//                return;
//            }
//            TrackInfo trackInfo = albumFeignClient.getTrackInfo(userPaidRecordVo.getItemIdList().get(0)).getData();
//            Long albumId = trackInfo.getAlbumId();
//            userPaidRecordVo.getItemIdList().forEach(trackId->{
//                UserPaidTrack userPaidTrack = new UserPaidTrack();
//                userPaidTrack.setOrderNo(userPaidRecordVo.getOrderNo());
//                userPaidTrack.setUserId(userPaidRecordVo.getUserId());
//                userPaidTrack.setAlbumId(albumId);
//                userPaidTrack.setTrackId(trackId);
//                userPaidTrackMapper.insert(userPaidTrack);
//            });
//
//        }else if (SystemConstant.ORDER_ITEM_TYPE_VIP.equals(itemType)){
//            UserVipService userVipServicePojo = new UserVipService();
//            //拿到vip套餐id去查月数
//            Long vipId = userPaidRecordVo.getItemIdList().get(0);
//            VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(vipId);
//            Integer serviceMonth = vipServiceConfig.getServiceMonth();
//            //判断身份
//            UserInfo userInfo = userInfoMapper.selectById(userPaidRecordVo.getUserId());
//            Integer isVip = userInfo.getIsVip();
//            if (isVip.intValue()==1&&userInfo.getVipExpireTime().after(new Date())){
//                //vip用户，延期
//                userVipServicePojo.setStartTime(userInfo.getVipExpireTime());
//
//                userVipServicePojo.setExpireTime(DateUtil.offsetMonth(userInfo.getVipExpireTime(),serviceMonth));
//            }else {
//                //普通用户，新开
//                userVipServicePojo.setStartTime(new Date());
//                userVipServicePojo.setExpireTime(DateUtil.offsetMonth(new Date(),serviceMonth));
//
//            }
//            userVipServicePojo.setUserId(userPaidRecordVo.getUserId());
//            userVipServicePojo.setOrderNo(userPaidRecordVo.getOrderNo());
//            userVipServiceMapper.insert(userVipServicePojo);
//            userInfo.setIsVip(1);
//            userInfo.setVipExpireTime(userVipServicePojo.getExpireTime());
//            userInfoMapper.updateById(userInfo);
//        }



    }


}
