package com.youngqi.tingshu.account.api;

import com.youngqi.tingshu.account.service.UserAccountService;
import com.youngqi.tingshu.common.login.YoungQiLogin;
import com.youngqi.tingshu.common.result.Result;
import com.youngqi.tingshu.common.util.AuthContextHolder;
import com.youngqi.tingshu.vo.account.AccountDeductVo;
import com.youngqi.tingshu.vo.account.AccountLockVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "用户账户管理")
@RestController
@RequestMapping("api/account")
@SuppressWarnings({"all"})
public class UserAccountApiController {

	@Autowired
	private UserAccountService userAccountService;

	@Operation(summary = "获取当前登录的用户可用余额")
	@GetMapping("/userAccount/getAvailableAmount")
	@YoungQiLogin
	public Result<BigDecimal> getAvailableAmount(){
		Long userId = AuthContextHolder.getUserId();
		BigDecimal availableamount=userAccountService.getAvailableAmount(userId);
		return Result.ok(availableamount);
	}

	@YoungQiLogin(requiredLogin = false)
	@Operation(summary = "检查及扣减账户余额")
	@PostMapping("/userAccount/checkAndDeduct")
	public Result checkAndDeduct(@RequestBody AccountDeductVo accountDeductVo){
		Long userId = AuthContextHolder.getUserId();
		if (userId!=null){
			accountDeductVo.setUserId(userId);
		}
		userAccountService.checkAndDeduct(accountDeductVo);
		return Result.ok();
	}
}

