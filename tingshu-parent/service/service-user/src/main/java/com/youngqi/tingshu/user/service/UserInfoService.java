package com.youngqi.tingshu.user.service;

import com.youngqi.tingshu.model.user.UserInfo;
import com.youngqi.tingshu.vo.base.PageVo;
import com.youngqi.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.youngqi.tingshu.vo.user.UserPaidRecordVo;

import java.util.List;
import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {

    Map<String, String> wxLogin(String code);

    UserInfoVo getUserInfo(Long userId);

    void updateUser(UserInfoVo userInfoVo);


    Map<Long, Integer> getUserIsPaidTrack(Long userId, Long albumId, List<Long> trackCheckIdList);

    /*
    * 订阅
    * */
    Boolean subscribe(Long userId, Long albumId);
    /*
    *
    * 查是否订阅
    * */
    Boolean isSubscribe(Long userId, Long albumId);

    /*
    * 收藏声音
    * */
    Boolean collect(Long userId, Long trackId);
    /*
    * 查询是否收藏声音
    * */
    Boolean isCollect(Long userId, Long trackId);


    PageVo findUserSubscribePage(Integer page, Integer limit, Long userId);

    Boolean isPaidAlbum(Long albumId);

    List<Long> findUserPaidTrackList(Long userId, Long albumId);

    /**
     * "处理用户购买记录"
     * @param userPaidRecordVo
     */
    void savePaidRecord(UserPaidRecordVo userPaidRecordVo);

}
