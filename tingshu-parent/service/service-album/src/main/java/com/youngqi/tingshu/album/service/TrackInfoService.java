package com.youngqi.tingshu.album.service;

import com.youngqi.tingshu.model.album.TrackInfo;
import com.youngqi.tingshu.query.album.TrackInfoQuery;
import com.youngqi.tingshu.vo.album.AlbumTrackListVo;
import com.youngqi.tingshu.vo.album.TrackInfoVo;
import com.youngqi.tingshu.vo.album.TrackListVo;
import com.youngqi.tingshu.vo.album.TrackStatVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface TrackInfoService extends IService<TrackInfo> {


    void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId);
    /*
    *
    * 新增声音统计信息
    * */
    void saveTrackStat(Long trackId, String statType, int statNum);

    Page<TrackListVo> findUserTrackPage(Page<TrackListVo> pageInfo, TrackInfoQuery trackInfoQuery);

    void updateTrackInfo(TrackInfoVo trackInfoVo, Long id);

    void deletedTrackInfoById(Long id);


    /*
    * 分页获取专辑下声音列表，动态根据用户情况展示声音付费标识
    * */

    Page<AlbumTrackListVo> getAlbumTrackPage(Long userId, Long albumId, Page<AlbumTrackListVo> pageInfo);

    /*
    * 获取声音统计信息
    * */
    TrackStatVo getTrackStatVo(Long trackId);

    List<TrackInfo> findPaidTrackInfoList( Long trackId, Integer trackCount);
}
