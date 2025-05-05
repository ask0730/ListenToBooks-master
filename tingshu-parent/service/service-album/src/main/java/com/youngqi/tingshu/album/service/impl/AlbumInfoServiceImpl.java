package com.youngqi.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import com.youngqi.tingshu.album.mapper.*;
import com.youngqi.tingshu.album.mapper.*;
import com.youngqi.tingshu.album.service.AlbumInfoService;
import com.youngqi.tingshu.common.cache.YoungQiCache;
import com.youngqi.tingshu.common.constant.KafkaConstant;
import com.youngqi.tingshu.common.constant.RedisConstant;
import com.youngqi.tingshu.common.constant.SystemConstant;
import com.youngqi.tingshu.common.execption.GuiguException;
import com.youngqi.tingshu.common.service.KafkaService;
import com.youngqi.tingshu.model.album.AlbumAttributeValue;
import com.youngqi.tingshu.model.album.AlbumInfo;
import com.youngqi.tingshu.model.album.AlbumStat;
import com.youngqi.tingshu.model.album.TrackInfo;
import com.youngqi.tingshu.query.album.AlbumInfoQuery;
import com.youngqi.tingshu.user.client.UserFeignClient;
import com.youngqi.tingshu.vo.album.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youngqi.tingshu.vo.album.*;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

	@Autowired
	private AlbumInfoMapper albumInfoMapper;
	@Autowired
	private AlbumAttributeValueMapper albumAttributeValueMapper;
	@Autowired
	private AlbumStatMapper albumStatMapper;
	@Autowired
	private TrackStatMapper trackStatMapper;
	@Autowired
	private TrackInfoMapper trackInfoMapper;

	@Autowired
	private KafkaService kafkaService;

	@Autowired
	private RedisTemplate redisTemplate;
	@Autowired
	private RedissonClient redissonClient;
	@Autowired
	private UserFeignClient userFeignClient;

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void saveAlbumInfo(Long userId, AlbumInfoVo albumInfoVo) {
		//存入专辑表
		AlbumInfo albumInfo = BeanUtil.copyProperties(albumInfoVo, AlbumInfo.class);
		albumInfo.setUserId(userId);
		albumInfo.setIncludeTrackCount(0);
		albumInfo.setIsFinished("0");
		albumInfo.setTracksForFree(5);
		albumInfo.setStatus(SystemConstant.ALBUM_STATUS_PASS);
		albumInfoMapper.insert(albumInfo);
		Long albumId = albumInfo.getId();


		//保存专辑还有他的标签和值
		List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
		if(CollectionUtil.isNotEmpty(albumAttributeValueVoList)){
		for (AlbumAttributeValueVo abvv: albumAttributeValueVoList) {
			AlbumAttributeValue albumAttributeValue = BeanUtil.copyProperties(abvv, AlbumAttributeValue.class);
			albumAttributeValue.setAlbumId(albumId);
			albumAttributeValueMapper.insert(albumAttributeValue);
		}
		}
		//保存专辑统计数据
		saveAlbumStat(albumId,SystemConstant.ALBUM_STAT_PLAY,0);
		saveAlbumStat(albumId,SystemConstant.ALBUM_STAT_SUBSCRIBE,0);
		saveAlbumStat(albumId,SystemConstant.ALBUM_STAT_BUY,0);
		saveAlbumStat(albumId,SystemConstant.ALBUM_STAT_COMMENT,0);
		RLock aaa = redissonClient.getLock("aaa");
		aaa.lock();
		aaa.unlock();
		//TODO:审核（可以调用阿里云等接口）

		if (true && albumInfo.getIsOpen().equals("1")) {//审核通过且公开
			//审核后将上架消息发送到Kafka
			kafkaService.sendMessage(KafkaConstant.QUEUE_ALBUM_UPPER, albumId.toString());
		}


	}


	//保存点播量，订阅量等
	public void saveAlbumStat(Long albumId,String statType ,int statNum ){
		AlbumStat albumStat = new AlbumStat();
		albumStat.setAlbumId(albumId);
		albumStat.setStatType(statType);
		albumStat.setStatNum(statNum);
		albumStatMapper.insert(albumStat);
	}

	@Override
	public Page<AlbumListVo> findUserAlbumByPage(Page<AlbumListVo> pageInfo, AlbumInfoQuery albumInfoQuery) {
		return albumInfoMapper.selectUserAlbumByPage(pageInfo,albumInfoQuery);
	}


	/*
	*
	*删除专辑
	* */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deletedAlbumInfoById(Long id) {
			//要判断专辑下是否还存在声音
		QueryWrapper<TrackInfo> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("album_id",id);
		Long count = trackInfoMapper.selectCount(queryWrapper);
		if (count>0){
			throw new GuiguException(400,"该专辑还存在声音未删除");
		}
		albumInfoMapper.deleteById(id);

		//删订阅量 播放量...表
		QueryWrapper<AlbumStat> albumStatQueryWrapper = new QueryWrapper<>();
		albumStatQueryWrapper.eq("album_id",id);
		albumStatMapper.delete(albumStatQueryWrapper);

		//删专辑键值表
		QueryWrapper<AlbumAttributeValue> albumAttributeValueQueryWrapper = new QueryWrapper<>();
		albumAttributeValueQueryWrapper.eq("album_id",id);
		albumAttributeValueMapper.delete(albumAttributeValueQueryWrapper);

		//通知es下架专辑
		kafkaService.sendMessage(KafkaConstant.QUEUE_ALBUM_LOWER,id.toString());
	}


	/**
	 * 获取专辑信息(SpringDataRedis分布式锁)避免缓存击穿
	 * @param id
	 * @return
	 */
	@Override
	public AlbumInfo getAlbumInfo(Long id) {
		try {
			String dataKey = RedisConstant.ALBUM_INFO_PREFIX+id;
			AlbumInfo albumInfo = (AlbumInfo) redisTemplate.opsForValue().get(dataKey);//从redis中获取缓存
			if (albumInfo !=null){
				log.info("命中缓存，直接返回，线程ID：{}，线程名称：{}", Thread.currentThread().getId(), Thread.currentThread().getName());
				return albumInfo;
			}
			//构建锁的key
			String lockKey=RedisConstant.ALBUM_LOCK_PREFIX+id+RedisConstant.CACHE_LOCK_SUFFIX;
			RLock lock = redissonClient.getLock(lockKey);//获取锁
			//获取锁（阻塞，watchdog看门狗---> 放锁默认传了-1）
			lock.lock();
			try {
				//再次查缓存(防止已经有一个线程查完数据库后，其他线程再次查数据库)

				albumInfo = (AlbumInfo) redisTemplate.opsForValue().get(dataKey);
				if (albumInfo != null){
					log.info("当前线程{},获取锁成功，且再次命中缓存成功", Thread.currentThread().getName());
					return albumInfo;
				}
				log.info("当前线程{},开始查询数据库", Thread.currentThread().getName());
				albumInfo = getAlbumInfoFromDB(id);
				//区分数据库中为null的和能查到的数据的时间
				Long ttl=albumInfo==null?RedisConstant.ALBUM_TEMPORARY_TIMEOUT : RedisConstant.ALBUM_TIMEOUT;
				redisTemplate.opsForValue().set(dataKey,albumInfo,ttl,TimeUnit.SECONDS); //放入缓存中
				return albumInfo;
			} finally {
				//释放锁
				log.info("当前线程：{}，释放锁", Thread.currentThread().getName());
				lock.unlock();

			}
		} catch (Exception e) {
			//Redis有问题的兜底
			log.error("[专辑服务]查询专辑数据异常：{}", e);
			return this.getAlbumInfoFromDB(id);
		}

	}

	/**
	 * 根据专辑ID查询专辑信息(包含专辑标签及值)-从数据库获取
	 * @param id
	 * @return
	 */
	@Override
	@YoungQiCache(prefix = "album:info:")
	public AlbumInfo getAlbumInfoFromDB(Long id) {
		AlbumInfo albumInfo = albumInfoMapper.selectById(id);
		if (albumInfo !=null){
			QueryWrapper<AlbumAttributeValue> queryWrapper = new QueryWrapper<>();
			queryWrapper.eq("album_id",id);
			List<AlbumAttributeValue> albumAttributeValues = albumAttributeValueMapper.selectList(queryWrapper);
			albumInfo.setAlbumAttributeValueVoList(albumAttributeValues);
		}

		return albumInfo;
	}

	@Override
	public void updateAlbumInfo(AlbumInfoVo albumInfoVo, Long id) {
		//修改专辑的信息
		AlbumInfo albumInfo = BeanUtil.copyProperties(albumInfoVo, AlbumInfo.class);
			albumInfo.setId(id);
			albumInfoMapper.updateById(albumInfo);

		//修改专辑标签
		//删除
		QueryWrapper<AlbumAttributeValue> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("album_id",id);
		albumAttributeValueMapper.delete(queryWrapper);
		//新增
		List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
		if(CollectionUtil.isNotEmpty(albumAttributeValueVoList)){
			for (AlbumAttributeValueVo atvv: albumAttributeValueVoList) {
				AlbumAttributeValue albumAttributeValue = BeanUtil.copyProperties(atvv, AlbumAttributeValue.class);
				albumAttributeValue.setAlbumId(id);
				albumAttributeValueMapper.insert(albumAttributeValue);
			}
		}

		//TODO 专辑修改要先对专辑下架，修改，修改后审核通过再上架
		if ("1".equals(albumInfo.getIsOpen())){
			kafkaService.sendMessage(KafkaConstant.QUEUE_ALBUM_UPPER,id.toString());
		}else {
			kafkaService.sendMessage(KafkaConstant.QUEUE_ALBUM_LOWER,id.toString());
		}
	}

	@Override
	public List<AlbumInfo> findUserAllAlbumList(Long userId) {
		QueryWrapper<AlbumInfo> albumInfoQueryWrapper = new QueryWrapper<>();
				albumInfoQueryWrapper.eq("user_id",userId);
				albumInfoQueryWrapper.select("id","album_title","status");
				//防止数据量太大，微信加载太慢，先限制200
				albumInfoQueryWrapper.last("limit 200");
		List<AlbumInfo> albumInfos = albumInfoMapper.selectList(albumInfoQueryWrapper);

		return albumInfos;
	}

	@Override
	@YoungQiCache(prefix = "album:stat:")
	public AlbumStatVo getAlbumStatVo(Long albumId) {

		return  albumInfoMapper.getAlbumStatVo(albumId);
	}



	/**
	 * MQ监听更新声音统计信息
	 * @param trackStatMqVo
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateTrackStat(TrackStatMqVo trackStatMqVo) {
		//幂等处理
		String key="md:"+trackStatMqVo.getBusinessNo();//将用于幂等处理的唯一标识取出
		try {
			Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, trackStatMqVo.getBusinessNo(), 1, TimeUnit.HOURS);
			if (flag){
				//更新声音的统计信息
				trackStatMapper.updateStat(trackStatMqVo.getTrackId(),trackStatMqVo.getStatType(),trackStatMqVo.getCount());
				//更新专辑的统计信息
				if(SystemConstant.TRACK_STAT_PLAY.equals(trackStatMqVo.getStatType())){
					albumStatMapper.updateStat(trackStatMqVo.getAlbumId(),SystemConstant.ALBUM_STAT_PLAY,trackStatMqVo.getCount());
				}
				if (SystemConstant.TRACK_STAT_COMMENT.equals(trackStatMqVo.getStatType())){
					albumStatMapper.updateStat(trackStatMqVo.getAlbumId(),SystemConstant.ALBUM_STAT_COMMENT,trackStatMqVo.getCount());

				}
			}
		} catch (Exception e) {
			redisTemplate.delete(key);
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Map<String, Object>> getUserCanPaidPaidList(Long trackId) {
		//获取声音信息，为了得到专辑ID 和 序号
		TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
		Assert.notNull(trackInfo,"查询不到声音信息{}"+trackInfo.getId());
		//查询该声音后所有可购买的声音
		LambdaQueryWrapper<TrackInfo> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(TrackInfo::getAlbumId,trackInfo.getAlbumId());
		queryWrapper.ge(TrackInfo::getOrderNum,trackInfo.getOrderNum());
		List<TrackInfo> trackInfosCanBuy = trackInfoMapper.selectList(queryWrapper);
		if (CollectionUtil.isEmpty(trackInfosCanBuy)){
			throw new GuiguException(400,"该专辑下没有声音可购买");
		}
		List<Long> userPaidTrackIdList = userFeignClient.findUserPaidTrackList(trackInfo.getAlbumId()).getData();
		if (CollectionUtil.isNotEmpty(userPaidTrackIdList)){
			trackInfosCanBuy = trackInfosCanBuy.stream()
					.filter(tInfo -> !userPaidTrackIdList
							.contains(tInfo.getId())).collect(Collectors.toList());
		}
		List<Map<String,Object>> resList = new ArrayList<>();
		if (CollectionUtil.isNotEmpty(trackInfosCanBuy)){
			AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfo.getAlbumId());
			BigDecimal price = albumInfo.getPrice();//单集价格
			Map<String, Object> currentMap = new HashMap<>();
			currentMap.put("name", "本集");
			currentMap.put("price", price);
			currentMap.put("trackCount", 1);
			resList.add(currentMap);
			//组装除收集之外的
			int count=trackInfosCanBuy.size();//可以买的声音数量
			for (int i=10;i<=50;i=i+10){
				if (i<count){
					//每次+10集后的选项之一
					Map<String, Object> map = new HashMap<>();
					map.put("name", "后" + i + "集");
					map.put("price", price.multiply(new BigDecimal(i)));
					map.put("trackCount", i);
					resList.add(map);
				}else {
					//全集
					Map<String, Object> map = new HashMap<>();
					map.put("name", "后" + count + "集");
					map.put("price", price.multiply(new BigDecimal(count)));
					map.put("trackCount", count);
					resList.add(map);
					break;
				}
			}
		}



		return resList;
	}


}
