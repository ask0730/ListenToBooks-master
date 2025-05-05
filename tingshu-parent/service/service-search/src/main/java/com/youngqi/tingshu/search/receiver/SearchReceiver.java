package com.youngqi.tingshu.search.receiver;

import com.alibaba.fastjson.JSON;
import com.youngqi.tingshu.common.constant.KafkaConstant;
import com.youngqi.tingshu.search.service.SearchService;
import com.youngqi.tingshu.vo.album.TrackStatMqVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * @author YSQ
 * @PackageName:com.youngqi.tingshu.search.receiver
 * @className: SearchReceiver
 * @Description:
 * @date 2024/12/18 9:36
 */
@Slf4j
@Component
public class SearchReceiver {
    @Autowired
    private SearchService searchService;

    /*
    *  ！！！幂等性 （此处没影响）
    * 监听上架消息
    * */
    @KafkaListener(topics = KafkaConstant.QUEUE_ALBUM_UPPER)
    public void albumUpper(ConsumerRecord<String,String> record){
        String value = record.value();
        if (StringUtils.isNotBlank(value)){
            log.info("[搜索服务] 监听到专辑上架:{}",value);
            searchService.upperAlbum(Long.parseLong(value));
        }

    }


    /*
    * 监听下架消息
    * */
    @KafkaListener(topics = KafkaConstant.QUEUE_ALBUM_LOWER)
    public void albumLower(ConsumerRecord<String,String> record){
        String value = record.value();
        if (StringUtils.isNotBlank(value)){
            log.info("[搜索服务] 监听到专辑下架:{}",value);
            searchService.lowerAlbum(Long.parseLong(value));
        }
    }

    /*
    *
    * 监听统计信息（播放量变化）
    *
    * */
    @KafkaListener(topics = KafkaConstant.QUEUE_TRACK_STAT_UPDATE)
    public void updateStat(ConsumerRecord<String,String> record){
        String value= record.value();
        if (StringUtils.isNotBlank(value)){
            log.info("[搜索服务]，监听到更新声音统计消息：{}", value);
            searchService.updateStat(JSON.parseObject(value, TrackStatMqVo.class));
        }
    }
}
