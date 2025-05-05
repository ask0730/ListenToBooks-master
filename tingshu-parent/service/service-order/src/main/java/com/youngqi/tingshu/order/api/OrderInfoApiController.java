package com.youngqi.tingshu.order.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youngqi.tingshu.common.login.YoungQiLogin;
import com.youngqi.tingshu.common.result.Result;
import com.youngqi.tingshu.common.util.AuthContextHolder;
import com.youngqi.tingshu.model.order.OrderInfo;
import com.youngqi.tingshu.order.service.OrderInfoService;
import com.youngqi.tingshu.vo.order.OrderInfoVo;
import com.youngqi.tingshu.vo.order.TradeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order")
@SuppressWarnings({"all"})
public class OrderInfoApiController {

	@Autowired
	private OrderInfoService orderInfoService;

	@YoungQiLogin
	@Operation(summary = "订单确认")
	@PostMapping("/orderInfo/trade")
	public Result<OrderInfoVo> trade(@RequestBody TradeVo tradeVo){
		Long userId = AuthContextHolder.getUserId();
		OrderInfoVo orderInfoVo=orderInfoService.trade(userId,tradeVo);
		return Result.ok(orderInfoVo);
	}
	@YoungQiLogin
	@Operation(summary = "订单提交(可能微信，可能余额)")
	@PostMapping("/orderInfo/submitOrder")
	public Result<Map<String,String>> submitOrder(@RequestBody OrderInfoVo orderInfoVo){
		Long userId = AuthContextHolder.getUserId();
		Map<String,String> resultMap=orderInfoService.submitOrder(orderInfoVo,userId);
		return Result.ok(resultMap);
	}

	@Operation(summary = "根据订单编号查询订单信息(商品明细、优惠明细)")
	@GetMapping("/orderInfo/getOrderInfo/{orderNo}")
	public Result<OrderInfo> getOrderInfo(@PathVariable String orderNo){
		OrderInfo orderInfo=orderInfoService.getOrderInfo(orderNo);
		return Result.ok(orderInfo);
	}
	@YoungQiLogin
	@Operation(summary = "订单列表分页")
	@GetMapping("/orderInfo/findUserPage/{page}/{limit}")
	public Result<Page<OrderInfo>>getUserOrderByPage(@PathVariable Integer page, @PathVariable Integer limit){
		Long userId = AuthContextHolder.getUserId();
		Page<OrderInfo> pageInfo = new Page<>(page,limit);
		pageInfo=orderInfoService.getUserOrderByPage(pageInfo,userId);
		return Result.ok(pageInfo);

	}
}

