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
 * @className: AlbumCdcHandler
 * @Description:
 * @date 2025/3/8 13:21
 */
@Slf4j
@CanalTable("album_info")
@Component
public class AlbumCdcHandler implements EntryHandler<CDCEntity> {
    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public void update(CDCEntity before, CDCEntity after) {
        log.info("监听到专辑数据修改,ID:{}", after.getId());
        String key="album:info:"+after.getId();
        redisTemplate.delete(key);
    }

    @Override
    public void delete(CDCEntity cdcEntity) {
        log.info("监听到专辑数据删除,ID:{}", cdcEntity.getId());
        String key="album:info:"+cdcEntity.getId();
        redisTemplate.delete(key);
    }
}
