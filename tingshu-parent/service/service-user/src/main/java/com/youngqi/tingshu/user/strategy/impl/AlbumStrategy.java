package com.youngqi.tingshu.user.strategy.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youngqi.tingshu.model.user.UserPaidAlbum;
import com.youngqi.tingshu.user.mapper.UserPaidAlbumMapper;
import com.youngqi.tingshu.user.strategy.ItemTypeStrategy;
import com.youngqi.tingshu.vo.user.UserPaidRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author YSQ
 * @PackageName:com.youngqi.tingshu.user.strategy.impl
 * @className: AlbumStrategy
 * @Description:
 * @date 2025/5/4 22:27
 */
@Slf4j
@Component("1001")
public class AlbumStrategy implements ItemTypeStrategy {
    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;
    /**
     * 处理购买专辑类型记录的策略
     * @param userPaidRecordVo
     */
    @Override
    public void savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        //查询专辑购买记录
        LambdaQueryWrapper<UserPaidAlbum> userPaidAlbumLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userPaidAlbumLambdaQueryWrapper.eq(UserPaidAlbum::getOrderNo,userPaidRecordVo.getOrderNo());//查流水号
        Long count = userPaidAlbumMapper.selectCount(userPaidAlbumLambdaQueryWrapper);
        if (count>0){
            return;
        }
        //没买过
        UserPaidAlbum userPaidAlbum = new UserPaidAlbum();
        userPaidAlbum.setOrderNo(userPaidRecordVo.getOrderNo());
        userPaidAlbum.setUserId(userPaidRecordVo.getUserId());
        userPaidAlbum.setAlbumId(userPaidRecordVo.getItemIdList().get(0));
        userPaidAlbumMapper.insert(userPaidAlbum);

    }
}
