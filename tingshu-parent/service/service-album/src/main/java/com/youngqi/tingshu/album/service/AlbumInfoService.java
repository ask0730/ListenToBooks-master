package com.youngqi.tingshu.album.service;

import com.youngqi.tingshu.model.album.AlbumInfo;
import com.youngqi.tingshu.query.album.AlbumInfoQuery;
import com.youngqi.tingshu.vo.album.AlbumInfoVo;
import com.youngqi.tingshu.vo.album.AlbumListVo;
import com.youngqi.tingshu.vo.album.AlbumStatVo;
import com.youngqi.tingshu.vo.album.TrackStatMqVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface AlbumInfoService extends IService<AlbumInfo> {


    void saveAlbumInfo(Long userId, AlbumInfoVo albumInfoVo);

    Page<AlbumListVo> findUserAlbumByPage(Page<AlbumListVo> pageInfo, AlbumInfoQuery albumInfoQuery);

    void deletedAlbumInfoById(Long id);

    /**
     *
     * @param id
     * @return
     */
    AlbumInfo getAlbumInfo(Long id);

    /**
     * 从数据库中查询专辑信息
     * @param id
     * @return
     */
    AlbumInfo getAlbumInfoFromDB(Long id);

    void updateAlbumInfo(AlbumInfoVo albumInfoVo, Long id);

    List<AlbumInfo> findUserAllAlbumList(Long userId);

    /*
    *
    * 根据专辑id查专辑统计信息
    * */
    AlbumStatVo getAlbumStatVo(Long albumId);

    /**
     * MQ监听更新声音统计信息
     * @param trackStatMqVo
     */
    void updateTrackStat(TrackStatMqVo trackStatMqVo);

    List<Map<String, Object>> getUserCanPaidPaidList(Long trackId);
}
