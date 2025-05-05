package com.youngqi.tingshu.user.service;

import com.youngqi.tingshu.vo.user.UserListenProcessVo;

import java.math.BigDecimal;
import java.util.Map;

public interface UserListenProcessService {

    BigDecimal getTrackBreakSecond(Long userId,Long trackId);

    void updateListenProcess(Long userId, UserListenProcessVo userListenProcessVo);

/*
*
*获取用户最近一次播放记录
* */
    Map<String,Long> getLatelyTrack(Long userId);
}
