package com.youngqi.tingshu.account;

import com.youngqi.tingshu.account.impl.AccountDegradeFeignClient;
import com.youngqi.tingshu.common.result.Result;
import com.youngqi.tingshu.vo.account.AccountDeductVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>
 * 账号模块远程调用API接口
 * </p>
 *
 * @author atguigu
 */
@FeignClient(value = "service-account", path = "api/account",fallback = AccountDegradeFeignClient.class)
public interface AccountFeignClient {


    @PostMapping("/userAccount/checkAndDeduct")
    public Result checkAndDeduct(@RequestBody AccountDeductVo accountDeductVo);

}
