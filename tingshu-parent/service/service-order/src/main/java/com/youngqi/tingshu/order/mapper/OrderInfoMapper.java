package com.youngqi.tingshu.order.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youngqi.tingshu.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    /**
     * 分页获取订单列表
     * @param pageInfo
     * @param userId
     * @return
     */
    Page<OrderInfo> getUserOrderByPage(Page<OrderInfo> pageInfo, Long userId);
}
