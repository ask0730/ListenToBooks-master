package com.youngqi;

import com.youngqi.tingshu.ServiceSearchApplication;
import com.youngqi.tingshu.common.constant.RedisConstant;
import com.youngqi.tingshu.search.service.SearchService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author YSQ
 * @PackageName:com.atguigu
 * @className: BatchImportTest
 * @Description:
 * @date 2024/12/17 14:28
 */
@SpringBootTest(classes = ServiceSearchApplication.class) //因为测试类和启动类不在同一包下
public class BatchImportTest {

    @Autowired
    private SearchService searchService;


    @Test
    public void test(){
        for (long i=0;i<1607;i++){
            try {
                searchService.upperAlbum(i);
            } catch (Exception e) {
                continue;
            }
        }
    }

    @Autowired
    private RedissonClient redissonClient;
    @Test
    public void testBloomFilter(){
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        for (long i=0;i<1607;i++){
            try {
                bloomFilter.add(i);
            } catch (Exception e) {
                continue;
            }
        }
    }
}
