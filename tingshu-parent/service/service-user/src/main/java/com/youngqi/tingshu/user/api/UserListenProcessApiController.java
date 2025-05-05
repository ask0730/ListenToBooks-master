package com.youngqi.tingshu.user.api;

import com.youngqi.tingshu.common.login.YoungQiLogin;
import com.youngqi.tingshu.common.result.Result;
import com.youngqi.tingshu.common.util.AuthContextHolder;
import com.youngqi.tingshu.user.service.UserListenProcessService;
import com.youngqi.tingshu.vo.user.UserListenProcessVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "用户声音播放进度管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class UserListenProcessApiController {

	@Autowired
	private UserListenProcessService userListenProcessService;

	@Operation(summary = "获取当前用户声音播放进度")
	@YoungQiLogin(requiredLogin = false)
	@GetMapping("/userListenProcess/getTrackBreakSecond/{trackId}")
	public Result<BigDecimal> getTrackBreakSecond(@PathVariable Long trackId){
		Long userId = AuthContextHolder.getUserId();
		if (userId!=null){
		BigDecimal trackBreakSecond = userListenProcessService.getTrackBreakSecond(userId,trackId);
		return Result.ok(trackBreakSecond);
		}
		return Result.ok();
	}
	@Operation(summary = "更新当前用户声音播放进度")
	@YoungQiLogin(requiredLogin = false)
	@PostMapping("/userListenProcess/updateListenProcess")
	public Result updateListenProcess(@RequestBody UserListenProcessVo userListenProcessVo){
		Long userId = AuthContextHolder.getUserId();
		if (userId !=null){
			userListenProcessService.updateListenProcess(userId,userListenProcessVo);
		}
		return Result.ok();
	}


	@YoungQiLogin
	@Operation(summary = "获取用户最近一次播放记录")
	@GetMapping("/userListenProcess/getLatelyTrack")
	public Result<Map<String,Long>>getLatelyTrack(){
		Long userId = AuthContextHolder.getUserId();
		return Result.ok(userListenProcessService.getLatelyTrack(userId));
	}

}

