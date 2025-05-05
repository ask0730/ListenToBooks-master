package com.youngqi.tingshu.common.cache;

import com.youngqi.tingshu.common.constant.RedisConstant;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author YSQ
 * @PackageName:com.youngqi.tingshu.common.cache
 * @className: YoungQiAspect
 * @Description: 自定义缓存切面类
 * @date 2025/1/22 10:27
 *
 *  环绕通知:对所有业务模块任意方法使用自定义注解缓存方法进行增强，增强逻辑:
 *  1.优先从缓存redis中获取业务数据
 *  2.未命中缓存，获取分布式锁
 *  3.执行目标方法(查询数据库方法)
 *  4.锁释放
 */
@Slf4j
@Aspect
@Component
public class YoungQiCacheAspect {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @SneakyThrows //相当于在整个外边又加了trycatch
    @Around("@annotation(youngQiCache)")
    public Object aspectCache(ProceedingJoinPoint pjp, YoungQiCache youngQiCache)  {
        //获取方法中的参数
        try {
            Object[] args = pjp.getArgs();
            String paramVal="none";
            //将参数变为 xx:xx:xx形式
            if (args !=null&&args.length>0){
                paramVal = Arrays.asList(args)
                        .stream()
                        .map(arg -> arg.toString())
                        .collect(Collectors.joining(":"));

            }
            //用 "前缀+方法参数构建key" 例如：album:info:参数1:参数2
            String datakey=youngQiCache.prefix()+paramVal;
            //从redis缓存中获取参数
            Object resultObj = redisTemplate.opsForValue().get(datakey);
            if (resultObj !=null){
                return  resultObj;
            }
            //获取分布式锁
            String lockKey=datakey+ RedisConstant.CACHE_LOCK_SUFFIX;//”数据key + 锁后缀“用于当锁的key
            RLock lock = redissonClient.getLock(lockKey);
            lock.lock();//锁！阻塞直到拿到锁
            //执行目标方法(！！！ PS:执行之前再查一次缓存，保正有一个线程查完放缓存了其他处于阻塞状态线程的不用再查数据库了)
            try {
                resultObj= redisTemplate.opsForValue().get(datakey);
                if (resultObj !=null){
                    return  resultObj;
                }
                //未命中缓存，执行目标方法（查数据库，放缓存）
                resultObj= pjp.proceed();
                Long ttl=resultObj==null?RedisConstant.ALBUM_TEMPORARY_TIMEOUT:RedisConstant.ALBUM_TIMEOUT;
                //放缓存
                redisTemplate.opsForValue().set(datakey,resultObj,ttl, TimeUnit.SECONDS);
                return resultObj;
            } finally {
                //释放锁
                lock.unlock();
            }
        } catch (Throwable e) {
            //兜底处理:(redis不可用),全包裹的大try catch
            log.info("自定义缓存切面异常：{}", e);
            //查数据
            return pjp.proceed();
        }




    }
}