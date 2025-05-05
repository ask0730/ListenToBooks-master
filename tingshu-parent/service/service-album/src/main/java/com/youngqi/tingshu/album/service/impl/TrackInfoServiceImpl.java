package com.youngqi.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import com.youngqi.tingshu.album.config.VodConstantProperties;
import com.youngqi.tingshu.album.mapper.AlbumInfoMapper;
import com.youngqi.tingshu.album.mapper.TrackInfoMapper;
import com.youngqi.tingshu.album.mapper.TrackStatMapper;
import com.youngqi.tingshu.album.service.TrackInfoService;
import com.youngqi.tingshu.album.service.VodService;
import com.youngqi.tingshu.common.constant.SystemConstant;
import com.youngqi.tingshu.common.execption.GuiguException;
import com.youngqi.tingshu.common.login.YoungQiLogin;
import com.youngqi.tingshu.model.album.AlbumInfo;
import com.youngqi.tingshu.model.album.TrackInfo;
import com.youngqi.tingshu.model.album.TrackStat;
import com.youngqi.tingshu.query.album.TrackInfoQuery;
import com.youngqi.tingshu.user.client.UserFeignClient;
import com.youngqi.tingshu.vo.album.*;
import com.youngqi.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youngqi.tingshu.vo.album.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

	@Autowired
	private TrackInfoMapper trackInfoMapper;


	@Autowired
	private VodConstantProperties vodConstantProperties;
	@Autowired
	private AlbumInfoMapper albumInfoMapper;
	@Autowired
	private VodService vodService;
	@Autowired
	private TrackStatMapper trackStatMapper;
	@Autowired
	private UserFeignClient userFeignClient;

	@YoungQiLogin
	@Transactional(rollbackFor = Exception.class)
	@Override
	public void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId) {
		TrackInfo trackInfo = BeanUtil.copyProperties(trackInfoVo, TrackInfo.class);
		trackInfo.setUserId(userId);
		trackInfo.setSource("1");
		trackInfo.setStatus(SystemConstant.TRACK_STATUS_PASS);
		AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfoVo.getAlbumId());
//		计算声音序号
		trackInfo.setOrderNum(albumInfo.getIncludeTrackCount()+1);
		//用唯一表示在腾讯云点播拿信息
		TrackMediaInfoVo trackMediaInfoVo = vodService.getTrackMediaInfo(trackInfoVo.getMediaFileId());
		if (trackMediaInfoVo!=null){
			trackInfo.setMediaDuration(new BigDecimal(trackMediaInfoVo.getDuration()));
			trackInfo.setMediaSize(trackMediaInfoVo.getSize());
			trackInfo.setMediaType(trackMediaInfoVo.getType());
		}
		trackInfoMapper.insert(trackInfo);
		Long id = trackInfo.getId();
		saveTrackStat(id, SystemConstant.TRACK_STAT_PLAY, 0);
		saveTrackStat(id, SystemConstant.TRACK_STAT_COLLECT, 0);
		saveTrackStat(id, SystemConstant.TRACK_STAT_PRAISE, 0);
		saveTrackStat(id, SystemConstant.TRACK_STAT_COMMENT, 0);
		//更新专辑下声音数量
		albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount()+1);
		albumInfoMapper.updateById(albumInfo);


	}

	@Override
	public void saveTrackStat(Long trackId, String statType, int statNum) {
		TrackStat trackStat = new TrackStat();
		trackStat.setTrackId(trackId);
		trackStat.setStatType(statType);
		trackStat.setStatNum(statNum);
		trackStatMapper.insert(trackStat);
	}

	@Override
	public Page<TrackListVo> findUserTrackPage(Page<TrackListVo> pageInfo, TrackInfoQuery trackInfoQuery) {
		return trackInfoMapper.selectUserTrackPage(pageInfo,trackInfoQuery);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateTrackInfo(TrackInfoVo trackInfoVo, Long id) {
		TrackInfo trackInfo = trackInfoMapper.selectById(id);
		//老的声音唯一标识
		String oldMediaFileId = trackInfo.getMediaFileId();
		//新的声音唯一标识
		String newMediaFileId = trackInfoVo.getMediaFileId();
		BeanUtil.copyProperties(trackInfoVo, trackInfo);
		//比较声音是否发生变化
		if (!oldMediaFileId.equals(newMediaFileId)){
			TrackMediaInfoVo trackMediaInfoVo = vodService.getTrackMediaInfo(newMediaFileId);
				trackInfo.setMediaDuration(new BigDecimal(trackMediaInfoVo.getDuration()));
				trackInfo.setMediaSize(trackMediaInfoVo.getSize());
				trackInfo.setMediaType(trackMediaInfoVo.getType());
				vodService.deleteTrackMedia(oldMediaFileId);
		}
		trackInfoMapper.updateById(trackInfo);


	}

	@Override
	public void deletedTrackInfoById(Long id) {
		TrackInfo trackInfo = trackInfoMapper.selectById(id);
		Integer orderNum = trackInfo.getOrderNum();
			trackInfoMapper.deleteById(trackInfo);
			//更新当前专辑所有声音的顺序
			trackInfoMapper.updateOrderNum(trackInfo.getAlbumId(),orderNum);
			//删除声音统计信息
		QueryWrapper<TrackStat> queryWrapper = new QueryWrapper<>();
					queryWrapper.eq("track_id",id);
					trackStatMapper.delete(queryWrapper);
			//更新专辑声音
		AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfo.getAlbumId());
			albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount()-1);
			albumInfoMapper.updateById(albumInfo);
			//删云点播平台声音文件
		vodService.deleteTrackMedia(trackInfo.getMediaFileId());

	}



	/*
	 * 分页获取专辑下声音列表，动态根据用户情况展示声音付费标识
	 * */


	@Override
	public Page<AlbumTrackListVo> getAlbumTrackPage(Long userId, Long albumId, Page<AlbumTrackListVo> pageInfo) {

		pageInfo=albumInfoMapper.getAlbumTrackPage(pageInfo,albumId);
		//todo 动态判断当前页中每个声音的付费标识
		AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
		Assert.notNull(albumInfo,"专辑:{}不存在",albumId);
		String payType = albumInfo.getPayType();
		//用户未登录
		if (userId==null){
			//判断专辑付费类型为VIP免费(0102) 、付费(0103)，除了免费试听以外，其他都要有付费标识
			if (SystemConstant.ALBUM_PAY_TYPE_VIPFREE.equals(payType)||SystemConstant.ALBUM_PAY_TYPE_REQUIRE.equals(payType)){

				pageInfo.getRecords().stream()
						.filter(albumTrackListVo ->albumTrackListVo.getOrderNum()>albumInfo.getTracksForFree()) //排除大于试听级数的
						.collect(Collectors.toList())
						.stream().forEach(albumTrackListVo -> albumTrackListVo.setIsShowPaidMark(true));
			}
		}else {
			UserInfoVo userInfoVo = userFeignClient.getUserInfoVoById(userId).getData();
			Assert.notNull(userInfoVo,"用户{}不存在",userId);
			Integer isVip = userInfoVo.getIsVip();
			Boolean isNeedChcekPaid=false;//是否需要查询支付（便于判断是否需要远程调用用户购买服务，《最后一起调用》）
			//登录了且专辑为VIP免费
			if (SystemConstant.ALBUM_PAY_TYPE_VIPFREE.equals(payType)){
				//用户为普通用户
				if (0==isVip.intValue()){
					isNeedChcekPaid=true;
				}
				//用户为vip但是过期了
				if (1==isVip.intValue()&&new Date().after(userInfoVo.getVipExpireTime())){
					isNeedChcekPaid=true;
				}
			}
			//如果为付费
			if (SystemConstant.ALBUM_PAY_TYPE_REQUIRE.equals(payType)){
				isNeedChcekPaid=true;
			}
			//isNeedChcekPaid 最后进行判断是否需要判断买过专辑或者声音的
			if (isNeedChcekPaid){
				List<AlbumTrackListVo> albumTrackListVoList = pageInfo.getRecords().stream()
						.filter(albumTrackListVo -> albumTrackListVo.getOrderNum() > albumInfo.getTracksForFree()) //排除大于试听级数的
						.collect(Collectors.toList());

				//拿到当前页除了试听的所有声音id(为了远程调用)
				List<Long> trackCheckIdList = albumTrackListVoList.stream()
						.map(albumTrackListVo -> albumTrackListVo.getTrackId())
						.collect(Collectors.toList());
				Map<Long, Integer> userPayTrackMap= userFeignClient.getUserIsPaidTrack(userId, albumId, trackCheckIdList).getData();

				for (AlbumTrackListVo albumTrackListVo : albumTrackListVoList) {
					if (userPayTrackMap.get(albumTrackListVo.getTrackId()).intValue()==0){ //0为未购买
						albumTrackListVo.setIsShowPaidMark(true);
					}
				}


			}
		}

		return pageInfo;
}

	@Override
	public TrackStatVo getTrackStatVo(Long trackId) {

		return trackStatMapper.getTrackStatVo(trackId);
	}

	@Override
	public List<TrackInfo> findPaidTrackInfoList( Long trackId, Integer trackCount) {
		TrackInfo trackInfo = trackInfoMapper.selectById(trackId);//获取声音信息
		List<Long> userHasPaidTrackIdList = userFeignClient.findUserPaidTrackList(trackInfo.getAlbumId()).getData();
		LambdaQueryWrapper<TrackInfo> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(TrackInfo::getAlbumId,trackInfo.getAlbumId());
		queryWrapper.ge(TrackInfo::getOrderNum,trackInfo.getOrderNum());
		if (CollectionUtil.isNotEmpty(userHasPaidTrackIdList)){
			queryWrapper.notIn(TrackInfo::getId,userHasPaidTrackIdList);
		}
		//设置购买数量
		queryWrapper.last("limit "+trackCount);
		queryWrapper.select(TrackInfo::getId,TrackInfo::getTrackTitle, TrackInfo::getCoverUrl, TrackInfo::getAlbumId);
		queryWrapper.orderByAsc(TrackInfo::getOrderNum);
		List<TrackInfo> PaidTrackInfoList = trackInfoMapper.selectList(queryWrapper);
		if (CollectionUtil.isEmpty(PaidTrackInfoList)){
			throw new GuiguException(400,"该专辑下没有符合购买要求声音");
		}

		return PaidTrackInfoList;
	}

}
