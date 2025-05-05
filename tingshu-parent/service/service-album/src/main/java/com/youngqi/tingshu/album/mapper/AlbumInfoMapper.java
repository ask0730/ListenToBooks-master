package com.youngqi.tingshu.album.mapper;

import com.youngqi.tingshu.model.album.AlbumInfo;
import com.youngqi.tingshu.query.album.AlbumInfoQuery;
import com.youngqi.tingshu.vo.album.AlbumListVo;
import com.youngqi.tingshu.vo.album.AlbumStatVo;
import com.youngqi.tingshu.vo.album.AlbumTrackListVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AlbumInfoMapper extends BaseMapper<AlbumInfo> {

    Page<AlbumListVo> selectUserAlbumByPage(Page<AlbumListVo> pageInfo, @Param("albumInfoQuery") AlbumInfoQuery albumInfoQuery);


    AlbumStatVo getAlbumStatVo(@Param("albumId") Long albumId);

    /**
    * 分页获取专辑下的声音列表
    * @param pageInfo 分页对象
    * @param albumId    专辑Id
    * */

    Page<AlbumTrackListVo> getAlbumTrackPage(Page<AlbumTrackListVo> pageInfo, @Param("albumId") Long albumId);
}
