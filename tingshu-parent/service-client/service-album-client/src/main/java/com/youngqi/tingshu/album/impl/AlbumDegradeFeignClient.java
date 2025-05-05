package com.youngqi.tingshu.album.impl;


import com.youngqi.tingshu.album.AlbumFeignClient;
import com.youngqi.tingshu.common.result.Result;
import com.youngqi.tingshu.model.album.*;
import com.youngqi.tingshu.model.album.*;
import com.youngqi.tingshu.vo.album.AlbumStatVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AlbumDegradeFeignClient implements AlbumFeignClient {


    @Override
    public Result<AlbumInfo> getAlbumInfo(Long id) {
        log.error("[专辑模块]提供远程调用方法getAlbumInfo的服务降级");
        return null;
    }

    @Override
    public Result<BaseCategoryView> getCategoryView(Long category3Id) {
        log.error("[专辑模块]提供远程调用getCategoryView服务降级");
        return null;
    }

    @Override
    public Result<List<BaseCategory3>> getTop7BaseCategory3(Long category1Id) {
        log.error("[专辑模块]提供远程调用etTop7BaseCategory3服务降级");
        return null;
    }

    @Override
    public Result<AlbumStatVo> getAlbumStatVo(Long albumId) {
        log.error("[专辑模块]提供远程调用方法getAlbumStatVo服务降级");
        return null;
    }

    @Override
    public Result<List<BaseCategory1>> getCategory1List() {
        log.error("[专辑模块]提供远程调用方法getCategory1List服务降级");
        return null;
    }

    @Override
    public Result<List<TrackInfo>> findPaidTrackInfoList(Long trackId, Integer trackCount) {
        log.error("[专辑模块]提供远程调用方法getWaitBuyTrackInfoList服务降级");
        return null;
    }

    @Override
    public Result<TrackInfo> getTrackInfo(Long id) {
        log.error("[专辑模块]提供远程调用方法getTrackInfo服务降级");
        return null;
    }
}
