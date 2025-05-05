package com.youngqi.tingshu.search.service;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.youngqi.tingshu.model.search.AlbumInfoIndex;
import com.youngqi.tingshu.model.search.SuggestIndex;
import com.youngqi.tingshu.query.search.AlbumIndexQuery;
import com.youngqi.tingshu.vo.album.TrackStatMqVo;
import com.youngqi.tingshu.vo.search.AlbumSearchResponseVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface SearchService {


    void upperAlbum(Long albumId);

    void lowerAlbum(Long albumId);

    AlbumSearchResponseVo searchAlbum(AlbumIndexQuery albumIndexQuery);

    List<Map<String, Object>> getTopCategory3HotAlbumList(Long category1Id);

    List<String> completeSuggest(String keyword);

   Collection<String> parseSuggestResult(String mySuggestKeyword, SearchResponse<SuggestIndex> searchResponse);

   /*
   * 更新专辑统计信息
   * */
    void updateStat(TrackStatMqVo parseObject);

    /**
     * 更新所有分类下排行榜-手动调用
     *
     */
    void updateLatelyAlbumRanking();

    /**
     * 获取指定1级分类下不同排序方式榜单列表-从Redis中获取
     * @param category1Id
     * @param dimension  排序方式
     * @return
     */
    List<AlbumInfoIndex> findRankingList(Long category1Id, String dimension);

}
