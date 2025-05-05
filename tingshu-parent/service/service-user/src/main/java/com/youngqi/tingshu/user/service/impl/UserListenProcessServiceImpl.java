package com.youngqi.tingshu.user.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.youngqi.tingshu.common.constant.KafkaConstant;
import com.youngqi.tingshu.common.constant.RedisConstant;
import com.youngqi.tingshu.common.constant.SystemConstant;
import com.youngqi.tingshu.common.service.KafkaService;
import com.youngqi.tingshu.common.util.MongoUtil;
import com.youngqi.tingshu.model.user.UserListenProcess;
import com.youngqi.tingshu.user.service.UserListenProcessService;
import com.youngqi.tingshu.vo.album.TrackStatMqVo;
import com.youngqi.tingshu.vo.user.UserListenProcessVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"all"})
public class UserListenProcessServiceImpl implements UserListenProcessService {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private RedisTemplate redisTemplate;
	@Autowired
	private KafkaService	kafkaService;



	@Override
	public BigDecimal getTrackBreakSecond(Long userId, Long trackId) {

		UserListenProcess userListenProcess = mongoTemplate.findOne(new Query()
						.addCriteria(Criteria.where("userId").is(userId).and("trackId").is(trackId))
				, UserListenProcess.class
				, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
		if (userListenProcess !=null){
				return userListenProcess.getBreakSecond();
		}
		return new BigDecimal("0.00");
	}

	@Override
	public void updateListenProcess(Long userId, UserListenProcessVo userListenProcessVo) {
		Query query = new Query();
		query.addCriteria(Criteria.where("userId").is(userId).and("trackId").is(userListenProcessVo.getTrackId()));
		query.limit(1);//防止播放器停止再开始重复发起请求造成两个播放记录（后期用分布式锁解决）
		UserListenProcess userListenProcess = mongoTemplate.findOne(query,
				UserListenProcess.class,
				MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
		//如果不存在新增播放进度
		if (userListenProcess == null){
			//新增
			userListenProcess=new UserListenProcess();
			userListenProcess.setUserId(userId);
			userListenProcess.setTrackId(userListenProcessVo.getTrackId());
			userListenProcess.setAlbumId(userListenProcessVo.getAlbumId());
			userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
			userListenProcess.setCreateTime(new Date());
			userListenProcess.setUpdateTime(new Date());
		}else {
			//更新
			userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
			userListenProcess.setUpdateTime(new Date());
		}
		//统一进行保存
		mongoTemplate.save(userListenProcess,MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS,userId));

//		//采用Redis提供set k v nx ex 确保在规定时间内（24小时/当日内）播放进度统计更新1次
		String key= RedisConstant.USER_TRACK_REPEAT_STAT_PREFIX+userId+":"+userListenProcessVo.getTrackId();
		long ttl= DateUtil.endOfDay(new Date()).getTime()-System.currentTimeMillis();
		Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, userListenProcess.getTrackId(), ttl, TimeUnit.MILLISECONDS);
		if (flag){
			TrackStatMqVo trackStatMqVo = new TrackStatMqVo();
			//生成业务唯一标识，消费者端(专辑服务、搜索服务)用来做幂等性处理，确保一个消息只能只被处理一次
			trackStatMqVo.setBusinessNo(IdUtil.fastSimpleUUID());
			trackStatMqVo.setAlbumId(userListenProcessVo.getAlbumId());
			trackStatMqVo.setTrackId(userListenProcessVo.getTrackId());
			trackStatMqVo.setStatType(SystemConstant.TRACK_STAT_PLAY);
			trackStatMqVo.setCount(1);
			//用kafka发消息
			kafkaService.sendMessage(KafkaConstant.QUEUE_TRACK_STAT_UPDATE, JSON.toJSONString(trackStatMqVo));


		}
	}
	/*获取用户最近一次播放记录*/
	@Override
	public Map<String, Long> getLatelyTrack(Long userId) {
		Query query = new Query();
		query.addCriteria(Criteria.where("userId").is(userId));
		query.with(Sort.by(Sort.Direction.DESC,"updateTime"));
		query.limit(1);
		UserListenProcess userListenProcess = mongoTemplate
				.findOne(query, UserListenProcess.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
		if (userListenProcess!=null){
			Map<String,Long> map=new HashMap();
			map.put("albumId",userListenProcess.getAlbumId());
			map.put("trackId",userListenProcess.getTrackId());
			return map;
		}
		return null;
	}
}
