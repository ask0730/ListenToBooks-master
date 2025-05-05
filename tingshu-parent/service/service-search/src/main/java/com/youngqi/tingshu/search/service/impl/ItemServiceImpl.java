package com.youngqi.tingshu.search.service.impl;

import cn.hutool.core.lang.Assert;
import com.youngqi.tingshu.album.AlbumFeignClient;
import com.youngqi.tingshu.common.constant.RedisConstant;
import com.youngqi.tingshu.common.execption.GuiguException;
import com.youngqi.tingshu.model.album.AlbumInfo;
import com.youngqi.tingshu.model.album.BaseCategoryView;
import com.youngqi.tingshu.search.service.ItemService;
import com.youngqi.tingshu.user.client.UserFeignClient;
import com.youngqi.tingshu.vo.album.AlbumStatVo;
import com.youngqi.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class ItemServiceImpl implements ItemService {

    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public Map<String, Object> getItemInfo(Long albumId) {
        //查询布隆过滤器是否包含查询专辑ID
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        boolean flag = bloomFilter.contains(albumId);
        if (!flag){
            throw new GuiguException(404,"专辑访问不存在");
        }

        Map<String,Object> resMap=new ConcurrentHashMap<>();

        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //获取专辑基本信息（albumInfo）
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            Assert.notNull(albumInfo, "专辑:{}不存在", albumId);
            resMap.put("albumInfo", albumInfo);
            return albumInfo;
        }, threadPoolExecutor);


        CompletableFuture<Void> albumStatCompletableFuture = CompletableFuture.runAsync(() -> {
            //获取专辑统计信息（albumStatVo）
            AlbumStatVo albumStatVo = albumFeignClient.getAlbumStatVo(albumId).getData();
            Assert.notNull(albumStatVo, "专辑:{}统计信息不存在", albumId);
            resMap.put("albumStatVo",albumStatVo);
        }, threadPoolExecutor);

        CompletableFuture<Void> baseCategoryViewCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            //获取专辑分类信息(baseCategoryView)
            BaseCategoryView baseCategoryView = albumFeignClient.getCategoryView(albumInfo.getCategory3Id()).getData();
            Assert.notNull(baseCategoryView, "专辑:{}分类信息不存在", albumId);
            resMap.put("baseCategoryView", baseCategoryView);
        }, threadPoolExecutor);

        CompletableFuture<Void> announecerCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            //获取专辑的主播信息（announecer属性）
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVoById(albumInfo.getUserId()).getData();
            resMap.put("announcer", userInfoVo);
        }, threadPoolExecutor);

        //组合异步任务
        CompletableFuture.allOf(
                albumInfoCompletableFuture,
                albumStatCompletableFuture,
                baseCategoryViewCompletableFuture,
                announecerCompletableFuture).join();

        return resMap;
    }
}
