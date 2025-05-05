package com.youngqi.tingshu.account.impl;


import com.youngqi.tingshu.account.AccountFeignClient;
import com.youngqi.tingshu.common.result.Result;
import com.youngqi.tingshu.vo.account.AccountDeductVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AccountDegradeFeignClient implements AccountFeignClient {

    @Override
    public Result checkAndDeduct(AccountDeductVo accountDeductVo) {
        log.error("[账户服务]执行服务降级方法：checkAndDeduct");
        return null;
    }
}
