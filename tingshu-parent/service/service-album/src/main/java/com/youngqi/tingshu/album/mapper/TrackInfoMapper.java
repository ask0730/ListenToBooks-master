package com.youngqi.tingshu.album.mapper;

import com.youngqi.tingshu.model.album.TrackInfo;
import com.youngqi.tingshu.query.album.TrackInfoQuery;
import com.youngqi.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TrackInfoMapper extends BaseMapper<TrackInfo> {


    Page<TrackListVo> selectUserTrackPage(Page<TrackListVo> pageInfo, @Param("trackInfoQuery") TrackInfoQuery trackInfoQuery);


    void updateOrderNum(Long albumId, Integer orderNum);
}
