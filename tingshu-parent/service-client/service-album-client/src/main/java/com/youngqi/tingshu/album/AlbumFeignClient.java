package com.youngqi.tingshu.album;

import com.youngqi.tingshu.album.impl.AlbumDegradeFeignClient;
import com.youngqi.tingshu.common.result.Result;
import com.youngqi.tingshu.model.album.*;
import com.youngqi.tingshu.model.album.*;
import com.youngqi.tingshu.vo.album.AlbumStatVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * <p>
 * 专辑模块远程调用Feign接口
 * </p>
 *
 * @author atguigu
 */
@FeignClient(value = "service-album", path = "/api/album",fallback = AlbumDegradeFeignClient.class)
public interface AlbumFeignClient {


    @GetMapping("/albumInfo/getAlbumInfo/{id}")
    public Result<AlbumInfo> getAlbumInfo(@PathVariable Long id);


    @GetMapping("/category/getCategoryView/{category3Id}")
    public Result<BaseCategoryView> getCategoryView(@PathVariable Long category3Id);

    @GetMapping("/category/findTopBaseCategory3/{category1Id}")
    public Result<List<BaseCategory3>> getTop7BaseCategory3(@PathVariable Long category1Id);

    /*
    * 根据专辑id获取专辑统计信息
    * */
    @GetMapping("/albumInfo/getAlbumStatVo/{albumId}")
    public Result<AlbumStatVo> getAlbumStatVo(@PathVariable Long albumId);

    /**
     * 查询所有一级分类列表(给排行榜的检索用)
     * @return
     */
    @GetMapping("/category/findAllCategory1")
    public Result<List<BaseCategory1>> getCategory1List();

    /**
     * 查询当前用户待购买声音列表(提供给订单服务渲染购买商品)
     * @param trackId
     * @param trackCount
     * @return
     */
    @GetMapping("trackInfo/findPaidTrackInfoList/{trackId}/{trackCount}")
    public Result<List<TrackInfo>> findPaidTrackInfoList(@PathVariable Long trackId, @PathVariable Integer trackCount);

    /**
     * 根据声音id获取声音信息（为了封装用户购买声音中的专辑id）
     * @param id
     * @return
     */
    @GetMapping("/trackInfo/getTrackInfo/{id}")
    public Result<TrackInfo> getTrackInfo(@PathVariable Long id);

}
