package com.youngqi.tingshu.user.client.impl;


import com.youngqi.tingshu.common.result.Result;
import com.youngqi.tingshu.model.user.VipServiceConfig;
import com.youngqi.tingshu.user.client.UserFeignClient;
import com.youngqi.tingshu.vo.user.UserInfoVo;
import com.youngqi.tingshu.vo.user.UserPaidRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class UserDegradeFeignClient implements UserFeignClient {

    @Override
    public Result<UserInfoVo> getUserInfoVoById(Long userId) {
        log.error("远程调用[用户服务]getUserInfoVoByUserId方法服务降级");
        return null;
    }

    @Override
    public Result<Map<Long, Integer>> getUserIsPaidTrack(Long userId, Long albumId, List<Long> trackCheckIdList) {
        log.error("远程调用[用户服务]getUserIsPaidTrack方法服务降级");
        return null;
    }

    @Override
    public Result<VipServiceConfig> getVipServiceConfig(Long id) {
        log.error("[用户服务]提供远程调用方法getVipServiceConfig执行服务降级");
        return null;
    }

    @Override
    public Result<Boolean> isPaidAlbum(Long albumId) {
        log.error("[用户服务]提供远程调用方法isPaidAlbum执行服务降级");
        return null;
    }

    @Override
    public Result<List<Long>> findUserPaidTrackList(Long albumId) {
        log.error("[用户服务]提供远程调用方法findUserPaidTrackList执行服务降级");
        return null;
    }

    @Override
    public Result savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        log.error("[用户服务]提供远程调用方法savePaidRecord执行服务降级");
        return null;
    }
}
