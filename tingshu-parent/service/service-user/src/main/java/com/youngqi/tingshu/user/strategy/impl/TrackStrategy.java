package com.youngqi.tingshu.user.strategy.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youngqi.tingshu.album.AlbumFeignClient;
import com.youngqi.tingshu.model.album.TrackInfo;
import com.youngqi.tingshu.model.user.UserPaidTrack;
import com.youngqi.tingshu.user.mapper.UserPaidTrackMapper;
import com.youngqi.tingshu.user.strategy.ItemTypeStrategy;
import com.youngqi.tingshu.vo.user.UserPaidRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author YSQ
 * @PackageName:com.youngqi.tingshu.user.strategy.impl
 * @className: TrackStrategy
 * @Description:
 * @date 2025/5/4 22:29
 */
@Slf4j
@Component("1002")
public class TrackStrategy implements ItemTypeStrategy {
    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;
    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Override
    public void savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        LambdaQueryWrapper<UserPaidTrack> userPaidTrackLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userPaidTrackLambdaQueryWrapper.eq(UserPaidTrack::getOrderNo,userPaidRecordVo.getOrderNo());
        Long count = userPaidTrackMapper.selectCount(userPaidTrackLambdaQueryWrapper);
        if (count>0){
            return;
        }
        TrackInfo trackInfo = albumFeignClient.getTrackInfo(userPaidRecordVo.getItemIdList().get(0)).getData();
        Long albumId = trackInfo.getAlbumId();
        userPaidRecordVo.getItemIdList().forEach(trackId->{
            UserPaidTrack userPaidTrack = new UserPaidTrack();
            userPaidTrack.setOrderNo(userPaidRecordVo.getOrderNo());
            userPaidTrack.setUserId(userPaidRecordVo.getUserId());
            userPaidTrack.setAlbumId(albumId);
            userPaidTrack.setTrackId(trackId);
            userPaidTrackMapper.insert(userPaidTrack);
        });

    }
}
