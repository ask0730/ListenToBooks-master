package com.youngqi.tingshu.album.mapper;

import com.youngqi.tingshu.model.album.BaseAttribute;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BaseAttributeMapper extends BaseMapper<BaseAttribute> {


//    根据一级分类Id获取分类（标签名包含标签值） 列表
    List<BaseAttribute> getAttributeAndValueByC1Id(@Param("category1Id") Long category1Id);
}
