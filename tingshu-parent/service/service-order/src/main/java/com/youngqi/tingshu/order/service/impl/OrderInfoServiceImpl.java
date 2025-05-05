package com.youngqi.tingshu.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youngqi.tingshu.account.AccountFeignClient;
import com.youngqi.tingshu.album.AlbumFeignClient;
import com.youngqi.tingshu.common.constant.RedisConstant;
import com.youngqi.tingshu.common.constant.SystemConstant;
import com.youngqi.tingshu.common.execption.GuiguException;
import com.youngqi.tingshu.common.result.Result;
import com.youngqi.tingshu.common.result.ResultCodeEnum;
import com.youngqi.tingshu.model.album.AlbumInfo;
import com.youngqi.tingshu.model.album.TrackInfo;
import com.youngqi.tingshu.model.order.OrderDerate;
import com.youngqi.tingshu.model.order.OrderDetail;
import com.youngqi.tingshu.model.order.OrderInfo;
import com.youngqi.tingshu.model.user.VipServiceConfig;
import com.youngqi.tingshu.order.helper.SignHelper;
import com.youngqi.tingshu.order.mapper.OrderDerateMapper;
import com.youngqi.tingshu.order.mapper.OrderDetailMapper;
import com.youngqi.tingshu.order.mapper.OrderInfoMapper;
import com.youngqi.tingshu.order.service.OrderInfoService;
import com.youngqi.tingshu.user.client.UserFeignClient;
import com.youngqi.tingshu.vo.account.AccountDeductVo;
import com.youngqi.tingshu.vo.account.AccountLockVo;
import com.youngqi.tingshu.vo.order.OrderDerateVo;
import com.youngqi.tingshu.vo.order.OrderDetailVo;
import com.youngqi.tingshu.vo.order.OrderInfoVo;
import com.youngqi.tingshu.vo.order.TradeVo;
import com.youngqi.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youngqi.tingshu.vo.user.UserPaidRecordVo;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private  OrderDetailMapper orderDetailMapper;
    @Autowired
    private OrderDerateMapper orderDerateMapper;
    @Autowired
    private AccountFeignClient accountFeignClient;


    @Override
    public OrderInfoVo trade(Long userId, TradeVo tradeVo) {
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        orderInfoVo.setItemType(tradeVo.getItemType());
        //提前声明价格
        BigDecimal originalAmount = new BigDecimal("0.00");
        BigDecimal derateAmount = new BigDecimal("0.00");
        BigDecimal orderAmount = new BigDecimal("0.00");
        //声明  订单明细 以及 订单优惠明细列表
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();//订单明细
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();//优惠明细

        String itemType = tradeVo.getItemType();//付款的项目类型
        if (SystemConstant.ORDER_ITEM_TYPE_VIP.equals(itemType)){
            VipServiceConfig vipServiceConfig = userFeignClient.getVipServiceConfig(tradeVo.getItemId()).getData();
            Assert.notNull(vipServiceConfig,"VIP套餐：{}不存在", tradeVo.getItemId());
            //封装价格
            originalAmount=vipServiceConfig.getPrice(); //原价
            orderAmount=vipServiceConfig.getDiscountPrice();//订单价
            derateAmount=originalAmount.subtract(orderAmount);//减免数额
            //封装订单中商品明细
            OrderDetailVo orderDetailVo=new OrderDetailVo();
            orderDetailVo.setItemId(tradeVo.getItemId());//会员id
            orderDetailVo.setItemName(vipServiceConfig.getName());//会员名字
            orderDetailVo.setItemUrl(vipServiceConfig.getImageUrl());//会员图片
            orderDetailVo.setItemPrice(originalAmount);//会员原价
            orderDetailVoList.add(orderDetailVo);
            //封装订单中优惠明细
            OrderDerateVo orderDerateVo=new OrderDerateVo();
            orderDerateVo.setDerateAmount(derateAmount);//优惠金额
            orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_VIP_SERVICE_DISCOUNT);//优惠类型
            orderDerateVo.setRemarks("VIP现时优惠："+derateAmount);
            orderDerateVoList.add(orderDerateVo);
        }else if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(itemType)){
            Boolean isBuy = userFeignClient.isPaidAlbum(tradeVo.getItemId()).getData();
            if (isBuy){
                throw new GuiguException(400,"当前用户已购买该专辑！");
            }
            //远程调用“用户服务” -获取当前用户信息(得到身份)
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVoById(userId).getData();
            Assert.notNull(userInfoVo,"用户信息为空！");
            Integer isVip = userInfoVo.getIsVip();
            //远调取专辑
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(tradeVo.getItemId()).getData();
            Assert.notNull(albumInfo,"专辑信息为空");
            //计算价格
            originalAmount=albumInfo.getPrice();
            orderAmount=originalAmount;
            //专辑是否存在普通折扣
            BigDecimal discount = albumInfo.getDiscount();
            if (discount.intValue()!=-1){
                //普通折扣存在非会员 或 会员到期了
                if (isVip.intValue()==0&&(isVip==1&&new Date().after(userInfoVo.getVipExpireTime()))){
                    orderAmount=originalAmount.multiply(discount).divide(new BigDecimal("10"),2, RoundingMode.HALF_UP);
                }
            }
            //专辑是否存在vip折扣
            BigDecimal vipDiscount = albumInfo.getVipDiscount();
            if (vipDiscount.intValue()!=-1){
                if (isVip.intValue()==1&&new Date().before(userInfoVo.getVipExpireTime())){
                    orderAmount=originalAmount.multiply(vipDiscount).divide(new BigDecimal("10"),2,RoundingMode.HALF_UP);
                }
            }
            derateAmount=originalAmount.subtract(orderAmount);//优惠金额
            //封装订单商品明细
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemName(albumInfo.getAlbumTitle());
            orderDetailVo.setItemUrl(albumInfo.getCoverUrl());
            orderDetailVo.setItemPrice(originalAmount);
            orderDetailVoList.add(orderDetailVo);
            //封装优惠明细
            if (derateAmount.doubleValue()>0) {
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_ALBUM_DISCOUNT);
                orderDerateVo.setDerateAmount(derateAmount);
                orderDerateVo.setRemarks("专辑优惠："+derateAmount);
                orderDerateVoList.add(orderDerateVo);
            }

        }else if(SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(itemType)){
            //远程调用获得所有可以买的声音
            List<TrackInfo> paidTrackInfoList = albumFeignClient.findPaidTrackInfoList(tradeVo.getItemId(), tradeVo.getTrackCount()).getData();
            if (CollectionUtil.isEmpty(paidTrackInfoList)){
                throw new GuiguException(400,"无符合要求声音");
            }
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(paidTrackInfoList.get(0).getAlbumId()).getData();
            BigDecimal price = albumInfo.getPrice();//声音单价
            originalAmount=price.multiply(new BigDecimal(paidTrackInfoList.size()));//声音单价*集数
            orderAmount=originalAmount;
            orderDetailVoList=paidTrackInfoList.stream().map(trackInfo -> {
                OrderDetailVo orderDetailVo = new OrderDetailVo();
                orderDetailVo.setItemId(trackInfo.getId());
                orderDetailVo.setItemName(trackInfo.getTrackTitle());
                orderDetailVo.setItemUrl(trackInfo.getCoverUrl());
                orderDetailVo.setItemPrice(price);
                return orderDetailVo;
            }).collect(Collectors.toList());



        }


        //均要返回的
        orderInfoVo.setOriginalAmount(originalAmount);//计算后的原价
        orderInfoVo.setOrderAmount(orderAmount);//计算后的订单价
        orderInfoVo.setDerateAmount(derateAmount);//计算后的减免价
        orderInfoVo.setOrderDetailVoList(orderDetailVoList);//订单详情项
        orderInfoVo.setOrderDerateVoList(orderDerateVoList);//订单减免项

        //流水号(防止重复提交)
        String tradeNokey= RedisConstant.ORDER_TRADE_NO_PREFIX+userId;
        String tradeNo = IdUtil.fastSimpleUUID();//生成流水号
        redisTemplate.opsForValue().set(tradeNokey,tradeNo,5, TimeUnit.MINUTES);
        orderInfoVo.setTradeNo(tradeNo);//封装流水号
        //时间戳
        orderInfoVo.setTimestamp(DateUtil.current());//当前时间戳
        //签名(防止篡改)
        Map<String, Object> map = BeanUtil.beanToMap(orderInfoVo, false, true);
        String sign = SignHelper.getSign(map);
        orderInfoVo.setSign(sign);


        return orderInfoVo;
    }

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public Map<String, String> submitOrder(OrderInfoVo orderInfoVo, Long userId) {
        //验流水号
        String tradeNokey=RedisConstant.ORDER_TRADE_NO_PREFIX+userId;
        String luaScript="if(redis.call('get', KEYS[1]) == ARGV[1]) then return redis.call('del', KEYS[1]) else return 0 end";
        DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>(luaScript,Boolean.class);
        boolean flag =(Boolean) redisTemplate.execute(redisScript, Arrays.asList(tradeNokey),orderInfoVo.getTradeNo());
        if (!flag){
            throw new GuiguException(400,"流水号异常");
        }
        //验证签名
        Map<String, Object> param = BeanUtil.beanToMap(orderInfoVo);
        param.remove("payWay");
        SignHelper.checkSign(param);//验签
        //保存订单
        OrderInfo orderInfo=this.saveOrderInfo(orderInfoVo,userId);
        //余额支付
        if (SystemConstant.ORDER_PAY_ACCOUNT.equals(orderInfoVo.getPayWay())){
            //余额扣减
            AccountDeductVo accountDeductVo = new AccountDeductVo();
            accountDeductVo.setOrderNo(orderInfo.getOrderNo());
            accountDeductVo.setUserId(userId);
            accountDeductVo.setAmount(orderInfo.getOrderAmount());
            accountDeductVo.setContent("消费："+orderInfoVo.getOrderDetailVoList().get(0).getItemName());
            //调用账户远程服务
            Result accountResult = accountFeignClient.checkAndDeduct(accountDeductVo);
            //判断远程调用响应业务状态码是否为200，非200则抛出异常，防止全局异常捕获后seata感知不到
            if (200!=accountResult.getCode()){
                throw new GuiguException(ResultCodeEnum.ACCOUNT_LESS);
            }
            //调用用户购买记录添加（虚拟发货）
            UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
            userPaidRecordVo.setOrderNo(orderInfo.getOrderNo());
            userPaidRecordVo.setUserId(userId);
            userPaidRecordVo.setItemType(orderInfo.getItemType());
            List<Long> itemIdList = orderInfoVo.getOrderDetailVoList().stream().map(OrderDetailVo::getItemId).collect(Collectors.toList());
            userPaidRecordVo.setItemIdList(itemIdList);
            Result userResult = userFeignClient.savePaidRecord(userPaidRecordVo);
            if (200!=userResult.getCode()){
                throw new GuiguException(userResult.getCode(),userResult.getMessage());
            }
            orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_PAID);
            orderInfoMapper.updateById(orderInfo);
        }

        HashMap<String, String> resMap = new HashMap<>();
        resMap.put("orderNo",orderInfo.getOrderNo());
        return resMap;
    }


    /**
     * 具体的保存订单和订单明细
     * @param orderInfoVo 订单信息Vo对象
     * @param userId      用户id
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderInfo saveOrderInfo(OrderInfoVo orderInfoVo, Long userId) {
        //保存订单
        OrderInfo orderInfo = BeanUtil.copyProperties(orderInfoVo, OrderInfo.class);//拷贝VO到PO
        orderInfo.setUserId(userId);//设置用户ID
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_UNPAID);//设置状态未支付
        //生成全局唯一订单编号 形式：当日日期+雪花算法
        String orderNo=DateUtil.today().replaceAll("-","")+IdUtil.getSnowflakeNextIdStr();
        orderInfo.setOrderNo(orderNo);
        //保存订单
        orderInfoMapper.insert(orderInfo);
        Long orderId = orderInfo.getId();
        //保存订单商品明显
        List<OrderDetailVo> orderDetailVoList = orderInfoVo.getOrderDetailVoList();
        if (CollectionUtil.isNotEmpty(orderDetailVoList)){
            orderDetailVoList.forEach(orderDetailVo -> {
                OrderDetail orderDetail = BeanUtil.copyProperties(orderDetailVo, OrderDetail.class);
                //关联订单
                orderDetail.setOrderId(orderId);
                orderDetailMapper.insert(orderDetail);
            });
        }
        List<OrderDerateVo> orderDerateVoList = orderInfoVo.getOrderDerateVoList();
        if (CollectionUtil.isNotEmpty(orderDerateVoList)){
            orderDerateVoList.forEach(orderDerateVo -> {
                OrderDerate orderDerate = BeanUtil.copyProperties(orderDerateVo, OrderDerate.class);
                orderDerate.setOrderId(orderId);
                orderDerateMapper.insert(orderDerate);
            });
        }
        return orderInfo;
    }

    /**
     * 根据订单编号查询订单信息
     * @param orderNo
     * @return
     */
    @Override
    public OrderInfo getOrderInfo(String orderNo) {
        LambdaQueryWrapper<OrderInfo> orderInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
        orderInfoLambdaQueryWrapper.eq(OrderInfo::getOrderNo,orderNo);
        OrderInfo orderInfo = orderInfoMapper.selectOne(orderInfoLambdaQueryWrapper);
        if (orderInfo!=null){
            LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
            orderDetailLambdaQueryWrapper.eq(OrderDetail::getOrderId,orderInfo.getId());
            List<OrderDetail> orderDetailList = orderDetailMapper.selectList(orderDetailLambdaQueryWrapper);
            orderInfo.setOrderDetailList(orderDetailList);

            LambdaQueryWrapper<OrderDerate> orderDerateLambdaQueryWrapper = new LambdaQueryWrapper<>();
            orderDerateLambdaQueryWrapper.eq(OrderDerate::getOrderId,orderInfo.getId());
            List<OrderDerate> orderDerateList = orderDerateMapper.selectList(orderDerateLambdaQueryWrapper);
            orderInfo.setOrderDerateList(orderDerateList);

        }
        //处理支付状态，付款方式
        orderInfo.setOrderStatusName(getOrderStatusName(orderInfo.getOrderStatus()));
        orderInfo.setPayWayName(getPayWayName(orderInfo.getPayWay()));
        return orderInfo;
    }

    /**
     * 分页获取当前用户订单列表
     * @param pageInfo
     * @param userId
     * @return
     */
    @Override
    public Page<OrderInfo> getUserOrderByPage(Page<OrderInfo> pageInfo, Long userId) {
        pageInfo=orderInfoMapper.getUserOrderByPage(pageInfo,userId);
        pageInfo.getRecords().forEach(orderInfo -> {
            orderInfo.setOrderStatusName(getOrderStatusName(orderInfo.getOrderStatus()));
            orderInfo.setPayWayName(getPayWayName(orderInfo.getPayWay()));
        });
        return pageInfo;
    }

    /**
     * 将订单状态变成中文
     * @param orderStatus
     * @return
     */
    private String getOrderStatusName(String orderStatus) {
        if (SystemConstant.ORDER_STATUS_UNPAID.equals(orderStatus)) {
            return "未支付";
        } else if (SystemConstant.ORDER_STATUS_PAID.equals(orderStatus)) {
            return "已支付";
        } else if (SystemConstant.ORDER_STATUS_CANCEL.equals(orderStatus)) {
            return "取消";
        }
        return null;
    }

    /**
     * 根据支付方式编号得到支付名称
     *
     * @param payWay
     * @return
     */
    private String getPayWayName(String payWay) {
        if (SystemConstant.ORDER_PAY_WAY_WEIXIN.equals(payWay)) {
            return "微信";
        } else if (SystemConstant.ORDER_PAY_ACCOUNT.equals(payWay)) {
            return "余额";
        } else if (SystemConstant.ORDER_PAY_WAY_ALIPAY.equals(payWay)) {
            return "支付宝";
        }
        return "";
    }
}
