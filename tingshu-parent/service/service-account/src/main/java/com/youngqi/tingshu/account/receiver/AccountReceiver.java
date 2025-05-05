package com.youngqi.tingshu.account.receiver;

import com.youngqi.tingshu.account.service.UserAccountService;
import com.youngqi.tingshu.common.constant.KafkaConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * @author YSQ
 * @PackageName:com.atguigu.tingshu.account.receiver
 * @className: AccountReceiver
 * @Description:
 * @date 2024/11/14 14:44
 */

@Slf4j
@Component
public class AccountReceiver {

    @Autowired
    private UserAccountService userAccountService;
        /*
        * 监听注册消息，进行初始化账户
        * //幂等
        * //事务管理
        * */

    @KafkaListener(topics = KafkaConstant.QUEUE_USER_REGISTER)
    public void userRegister(ConsumerRecord<String,String> record){
        String value = record.value();
        if (StringUtils.isNotBlank(value)){
            log.info("[账户服务]监听用户注册成功消息：{}", value);
            Long userId = Long.valueOf(value);
            userAccountService.initUserAccount(userId);
        }
    }
}
