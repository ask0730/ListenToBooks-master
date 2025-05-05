package com.youngqi.tingshu.user.strategy.impl;

import cn.hutool.core.date.DateUtil;
import com.youngqi.tingshu.model.user.UserInfo;
import com.youngqi.tingshu.model.user.UserVipService;
import com.youngqi.tingshu.model.user.VipServiceConfig;
import com.youngqi.tingshu.user.mapper.UserInfoMapper;
import com.youngqi.tingshu.user.mapper.UserVipServiceMapper;
import com.youngqi.tingshu.user.mapper.VipServiceConfigMapper;
import com.youngqi.tingshu.user.strategy.ItemTypeStrategy;
import com.youngqi.tingshu.vo.user.UserPaidRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author YSQ
 * @PackageName:com.youngqi.tingshu.user.strategy.impl
 * @className: VipStrategy
 * @Description:
 * @date 2025/5/4 22:29
 */
@Slf4j
@Component("1003")
public class VipStrategy implements ItemTypeStrategy {
    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private UserVipServiceMapper userVipServiceMapper;
    @Override
    public void savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        UserVipService userVipServicePojo = new UserVipService();
        //拿到vip套餐id去查月数
        Long vipId = userPaidRecordVo.getItemIdList().get(0);
        VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(vipId);
        Integer serviceMonth = vipServiceConfig.getServiceMonth();
        //判断身份
        UserInfo userInfo = userInfoMapper.selectById(userPaidRecordVo.getUserId());
        Integer isVip = userInfo.getIsVip();
        if (isVip.intValue()==1&&userInfo.getVipExpireTime().after(new Date())){
            //vip用户，延期
            userVipServicePojo.setStartTime(userInfo.getVipExpireTime());

            userVipServicePojo.setExpireTime(DateUtil.offsetMonth(userInfo.getVipExpireTime(),serviceMonth));
        }else {
            //普通用户，新开
            userVipServicePojo.setStartTime(new Date());
            userVipServicePojo.setExpireTime(DateUtil.offsetMonth(new Date(),serviceMonth));

        }
        userVipServicePojo.setUserId(userPaidRecordVo.getUserId());
        userVipServicePojo.setOrderNo(userPaidRecordVo.getOrderNo());
        userVipServiceMapper.insert(userVipServicePojo);
        userInfo.setIsVip(1);
        userInfo.setVipExpireTime(userVipServicePojo.getExpireTime());
        userInfoMapper.updateById(userInfo);

    }
}
