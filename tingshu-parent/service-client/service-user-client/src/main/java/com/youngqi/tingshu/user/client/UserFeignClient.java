package com.youngqi.tingshu.user.client;

import com.youngqi.tingshu.common.result.Result;
import com.youngqi.tingshu.model.user.VipServiceConfig;
import com.youngqi.tingshu.user.client.impl.UserDegradeFeignClient;
import com.youngqi.tingshu.vo.user.UserInfoVo;
import com.youngqi.tingshu.vo.user.UserPaidRecordVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author atguigu
 */
@FeignClient(value = "service-user", path = "/api/user",fallback = UserDegradeFeignClient.class)
public interface UserFeignClient {


    @GetMapping("/userInfo/getUserInfoVo/{userId}")
    public Result<UserInfoVo> getUserInfoVoById(@PathVariable Long userId);

    /*
    * 判断当前用户某一页中声音列表购买情况
    *
    * */
    @PostMapping("/userInfo/userIsPaidTrack/{userId}/{albumId}")
    public Result<Map<Long,Integer>> getUserIsPaidTrack(@PathVariable Long userId,
                                                        @PathVariable Long albumId,
                                                        @RequestBody List<Long> trackCheckIdList);

    /*
    *
    * 根据vipId获取Vip套餐
    * */
    @GetMapping("/vipServiceConfig/getVipServiceConfig/{id}")
    public Result<VipServiceConfig> getVipServiceConfig(@PathVariable Long id);


    /**
     *
     * 提供给订单服务调用，验证当前用户是否购买过专辑
     * @param albumId
     * @return
     */
    @GetMapping("/userInfo/isPaidAlbum/{albumId}")
    public Result<Boolean> isPaidAlbum(@PathVariable Long albumId);


    /**
     * 提供给订单服务调用，验证当前用户是否买过哪些声音
     * @param albumId
     * @return
     */
    @GetMapping("/userInfo/findUserPaidTrackList/{albumId}")
    public Result<List<Long>>findUserPaidTrackList(@PathVariable Long albumId);

    /**
     * 用户添加购买记录（虚拟物品发货）
     * @param userPaidRecordVo
     * @return
     */
    @PostMapping("/userInfo/savePaidRecord")
    public Result savePaidRecord(@RequestBody UserPaidRecordVo userPaidRecordVo);

}
