package com.youngqi.tingshu.handler;

import com.youngqi.tingshu.model.CDCEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import top.javatool.canal.client.annotation.CanalTable;
import top.javatool.canal.client.handler.EntryHandler;

/**
 * @author YSQ
 * @PackageName:com.atguigu.tingshu.handler
 * @className: UserCdcHandler
 * @Description:
 * @date 2025/3/8 12:35
 */
@Slf4j
@Component
@CanalTable("user_info")
public class UserCdcHandler implements EntryHandler<CDCEntity> {

    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public void update(CDCEntity before, CDCEntity after) {
        log.info("监听到用户数据修改,ID:{}", after.getId());
        String key= "userInfoVo:" + after.getId();
       redisTemplate.delete(key);
    }

    @Override
    public void delete(CDCEntity cdcEntity) {
        log.info("监听到用户数据删除,ID:{}", cdcEntity.getId());
        String key = "userInfoVo:" + cdcEntity.getId();
        redisTemplate.delete(key);
    }
}
