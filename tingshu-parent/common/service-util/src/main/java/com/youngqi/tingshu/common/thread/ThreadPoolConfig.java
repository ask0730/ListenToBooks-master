package com.youngqi.tingshu.common.thread;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author YSQ
 * @PackageName:com.atguigu.tingshu.common.thread
 * @className: ThreadPoolConfig
 * @Description:
 * @date 2024/12/16 17:16
 */
@Configuration
public class ThreadPoolConfig {

        /*
        * 自定义线程池
        * */
    @Bean
    public ThreadPoolExecutor threadPoolExecutor(){
        //核心线程数==cpu逻辑处理器*2
        int coreCount = Runtime.getRuntime().availableProcessors()*2;
        //创建线程池对象
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                coreCount,coreCount,
                0,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),
                Executors.defaultThreadFactory(),
                (r,e)->{
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    e.submit(r);
                }
                );
         //项目一启动核心线程池就开始创建
        threadPoolExecutor.prestartCoreThread();
        return threadPoolExecutor;
    }
}
