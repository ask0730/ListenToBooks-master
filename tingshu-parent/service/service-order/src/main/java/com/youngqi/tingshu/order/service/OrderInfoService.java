package com.youngqi.tingshu.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youngqi.tingshu.model.order.OrderInfo;
import com.youngqi.tingshu.vo.order.OrderInfoVo;
import com.youngqi.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface OrderInfoService extends IService<OrderInfo> {


    OrderInfoVo trade(Long userId, TradeVo tradeVo);

    Map<String, String> submitOrder(OrderInfoVo orderInfoVo, Long userId);

    /**
     * 保存订单明细以及商品明细，优惠明细
     * @param orderInfoVo 订单信息Vo对象
     * @param userId 用户id
     * @return  保存后订单对象
     */
    OrderInfo saveOrderInfo(OrderInfoVo orderInfoVo, Long userId);

    OrderInfo getOrderInfo(String orderNo);

    Page<OrderInfo> getUserOrderByPage(Page<OrderInfo> pageInfo, Long userId);
}
