package com.youngqi.tingshu.user.api;

import com.youngqi.tingshu.common.login.YoungQiLogin;
import com.youngqi.tingshu.common.result.Result;
import com.youngqi.tingshu.common.util.AuthContextHolder;
import com.youngqi.tingshu.user.service.UserInfoService;
import com.youngqi.tingshu.vo.base.PageVo;
import com.youngqi.tingshu.vo.user.UserInfoVo;
import com.youngqi.tingshu.vo.user.UserPaidRecordVo;
import com.youngqi.tingshu.vo.user.UserSubscribeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class UserInfoApiController {

	@Autowired
	private UserInfoService userInfoService;


	@Operation(summary = "根据用户id查用户信息")
	@GetMapping("/userInfo/getUserInfoVo/{userId}")
	public Result<UserInfoVo> getUserInfoVoById(@PathVariable Long userId){
		UserInfoVo userInfoVo =userInfoService.getUserInfo(userId);

		return Result.ok(userInfoVo);
	}


	/**
	 * 提供给专辑服务
	*	判断当前用户某一页中声音列表购买情况
	* @param userId 用户id
	* @param albumId	专辑id
	* @param trackCheckIdList 待检查购买情况声音列表
	 * @return result: 1:已购  0:未购
	* */
	@Operation(summary = "获取某一页中用户声音列表付费情况")
	@PostMapping("/userInfo/userIsPaidTrack/{userId}/{albumId}")
	public Result<Map<Long,Integer>> getUserIsPaidTrack(@PathVariable Long userId,
														@PathVariable Long albumId,
														@RequestBody List<Long> trackCheckIdList){

		Map<Long,Integer> result=userInfoService.getUserIsPaidTrack(userId,albumId,trackCheckIdList);

		return Result.ok(result);
	}

	@YoungQiLogin(requiredLogin = true)
	@Operation(summary = "订阅专辑")
	@GetMapping("/userInfo/subscribe/{albumId}")
	public Result<Boolean> subscribe(@PathVariable Long albumId){
		Long userId = AuthContextHolder.getUserId();
		Boolean flag = userInfoService.subscribe(userId,albumId);
		return Result.ok(flag);
	}


	@YoungQiLogin(requiredLogin = false)
	@Operation(summary = "查询是否订阅专辑")
	@GetMapping("/userInfo/isSubscribe/{albumId}")
	public Result<Boolean> isSubscribe(@PathVariable Long albumId) {
		Long userId = AuthContextHolder.getUserId();
		if (userId ==null){
			return Result.ok(false);
		}
		Boolean flag=userInfoService.isSubscribe(userId,albumId);
		return Result.ok(flag);
	}
	@YoungQiLogin(requiredLogin = true)
	@Operation(summary = "收藏声音")
	@GetMapping("/userInfo/collect/{trackId}")
	public Result<Boolean> collect(@PathVariable Long trackId){
		Long userId = AuthContextHolder.getUserId();
		Boolean flag=userInfoService.collect(userId,trackId);
		return Result.ok(flag);
	}

	@YoungQiLogin(requiredLogin = false)
	@Operation(summary = "查询是否收藏声音")
	@GetMapping("/userInfo/isCollect/{trackId}")
	public Result<Boolean> isCollect (@PathVariable Long trackId){
		Long userId = AuthContextHolder.getUserId();
		if (userId==null){
			return Result.ok(false);
		}
		Boolean flag=userInfoService.isCollect(userId,trackId);
		return Result.ok(flag);
	}

	@YoungQiLogin(requiredLogin = false)
	@Operation(summary = "查询订阅历史")
	@GetMapping("/userInfo/findUserSubscribePage/{page}/{limit}")
	public Result<PageVo<UserSubscribeVo>> findUserSubscribePage(@PathVariable Integer page, @PathVariable Integer limit){
		Long userId = AuthContextHolder.getUserId();
		if (userId!=null){
			PageVo pageVo=userInfoService.findUserSubscribePage(page,limit,userId);
			return Result.ok(pageVo);
		}
		return null;
	}

	@YoungQiLogin
	@Operation(summary = "验证当前用户是否购买过该专辑(提供给订单模块调用)")
	@GetMapping("/userInfo/isPaidAlbum/{albumId}")
	public Result<Boolean> isPaidAlbum(@PathVariable Long albumId){
		Boolean res=userInfoService.isPaidAlbum(albumId);
		return Result.ok(res);
	}

	@YoungQiLogin
	@Operation(summary = "获取当前用户已购声音集合（提供给订单模块远程调用）")
	@GetMapping("/userInfo/findUserPaidTrackList/{albumId}")
	public Result<List<Long>>findUserPaidTrackList(@PathVariable Long albumId){
		Long userId = AuthContextHolder.getUserId();
		List<Long> userPaidTrackIdList=userInfoService.findUserPaidTrackList(userId,albumId);

		return Result.ok(userPaidTrackIdList);
	}

	/**
	 * 登不登陆均可，防止微信支付后异步回调的时候没有token
	 * @param userPaidRecordVo 购买记录vo
	 * @return
	 */
	@YoungQiLogin(requiredLogin = false)
	@Operation(summary ="处理用户购买记录" )
	@PostMapping("/userInfo/savePaidRecord")
	public Result savePaidRecord(@RequestBody UserPaidRecordVo userPaidRecordVo){
		Long userId = AuthContextHolder.getUserId();
		if (userId !=null){
			userPaidRecordVo.setUserId(userId);
		}
		userInfoService.savePaidRecord(userPaidRecordVo);
		return Result.ok();
	}





}

