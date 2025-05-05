package com.youngqi.tingshu;

import com.youngqi.tingshu.common.constant.RedisConstant;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)//取消数据源自动配置
@EnableDiscoveryClient
@EnableFeignClients //开启feign扫描 确保该注解能够扫描到Feign接口
@EnableAsync
public class ServiceSearchApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ServiceSearchApplication.class, args);
    }



    /*
    * 初始化布隆过滤器
    * */
    @Autowired
    private RedissonClient redissonClient;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("布隆过滤器初始化");
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        bloomFilter.tryInit(500000L,0.03);
    }
}
