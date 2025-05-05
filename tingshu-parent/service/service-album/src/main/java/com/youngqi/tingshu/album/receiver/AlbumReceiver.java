package com.youngqi.tingshu.album.receiver;

import com.alibaba.fastjson.JSON;
import com.youngqi.tingshu.album.service.AlbumInfoService;
import com.youngqi.tingshu.common.constant.KafkaConstant;
import com.youngqi.tingshu.vo.album.TrackStatMqVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * @author YSQ
 * @PackageName:com.atguigu.tingshu.album.receiver
 * @className: AlbumReceiver
 * @Description:
 * @date 2025/1/13 11:29
 */
@Slf4j
@Component
public class AlbumReceiver {

    /*
    * /**
     * MQ监听更新声音统计信息
     *
     */

    @Autowired
    private AlbumInfoService albumInfoService;

    @KafkaListener(topics = KafkaConstant.QUEUE_TRACK_STAT_UPDATE)
    public void updateTrackStat(ConsumerRecord<String,String> record){
        String value = record.value();
        if (StringUtils.isNotBlank(value)){
            log.info("[专辑服务]，监听到更新声音统计消息：{}", value);
            TrackStatMqVo trackStatMqVo = JSON.parseObject(value, TrackStatMqVo.class);
            albumInfoService.updateTrackStat(trackStatMqVo);
        }
    }
}
