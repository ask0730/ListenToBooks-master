package com.youngqi.tingshu.album.mapper;

import com.youngqi.tingshu.model.album.TrackStat;
import com.youngqi.tingshu.vo.album.TrackStatVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TrackStatMapper extends BaseMapper<TrackStat> {


    @Update("update track_stat set stat_num = stat_num + #{count} where track_id = #{trackId} and stat_type = #{statType}")
    void updateStat(@Param("trackId") Long trackId, @Param("statType") String statType, @Param("count") Integer count);

    @Select("select track_id, max(if(stat_type='0701',stat_num,0)) playStatNum," +
            "max(if(stat_type='0702',stat_num,0)) collectStatNum," +
            "max(if(stat_type='0703',stat_num,0)) praiseStatNum," +
            "max(if(stat_type='0704',stat_num,0)) commentStatNum " +
            "from track_stat where track_id = #{trackId} and is_deleted = 0 group by track_id") //from前的空格
    TrackStatVo getTrackStatVo(@Param("trackId") Long trackId);
}
