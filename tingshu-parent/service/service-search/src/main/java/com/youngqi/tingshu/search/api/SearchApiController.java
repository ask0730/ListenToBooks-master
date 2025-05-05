package com.youngqi.tingshu.search.api;

import com.youngqi.tingshu.common.result.Result;
import com.youngqi.tingshu.model.search.AlbumInfoIndex;
import com.youngqi.tingshu.query.search.AlbumIndexQuery;
import com.youngqi.tingshu.search.service.SearchService;
import com.youngqi.tingshu.vo.search.AlbumSearchResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "搜索专辑管理")
@RestController
@RequestMapping("api/search")
@SuppressWarnings({"all"})
public class SearchApiController {

    @Autowired
    private SearchService searchService;


    @GetMapping("/albumInfo/upperAlbum/{albumId}")
    public Result upperAlbum(@PathVariable Long albumId){
            searchService.upperAlbum(albumId);
       return Result.ok();
    }

    @GetMapping("/albumInfo/lowerAlbum/{albumId}")
    public Result lowerAlbum(@PathVariable Long albumId){
        searchService.lowerAlbum(albumId);
        return Result.ok();
    }


    @Operation(summary = "站内检索（关键词、分类、标签、分页检索，结果高亮）")
    @PostMapping("/albumInfo")
    public Result<AlbumSearchResponseVo> searchAlbum(@RequestBody AlbumIndexQuery albumIndexQuery){
       AlbumSearchResponseVo albumSearchResponseVo= searchService.searchAlbum(albumIndexQuery);
      return Result.ok(albumSearchResponseVo);
    }

    @Operation(summary = "查询一级下每个三级类的热门专辑")
    @GetMapping("/albumInfo/channel/{category1Id}")
    public Result<List<Map<String,Object>>>getTopCategory3HotAlbumList(@PathVariable Long category1Id){
       List<Map<String,Object>> list = searchService.getTopCategory3HotAlbumList(category1Id);

       return Result.ok(list);
    }

    @Operation(summary = "提示关键词")
    @GetMapping("/albumInfo/completeSuggest/{keyword}")
    public Result<List<String>> completeSuggest(@PathVariable String keyword){
     List<String> suggestList = searchService.completeSuggest(keyword);
     return Result.ok(suggestList);
    }

    @Operation(summary = "为定时更新所有分类下排行榜(目前为手动调用)")
    @GetMapping("/albumInfo/updateLatelyAlbumRanking")
    public Result updateLatelyAlbumRanking(){
        searchService.updateLatelyAlbumRanking();
       return Result.ok();
    }
    @Operation(summary = "获取排行榜")
    @GetMapping("/albumInfo/findRankingList/{category1Id}/{dimension}")
    public Result<List<AlbumInfoIndex>> findRankingList(@PathVariable Long category1Id,@PathVariable String dimension ){
       List<AlbumInfoIndex> list= searchService.findRankingList(category1Id,dimension);
       return Result.ok(list);
    }
}

