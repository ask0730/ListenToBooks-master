package com.youngqi.tingshu.account.service;

import com.youngqi.tingshu.model.account.UserAccount;
import com.baomidou.mybatisplus.extension.service.IService;
import com.youngqi.tingshu.vo.account.AccountDeductVo;

import java.math.BigDecimal;

public interface UserAccountService extends IService<UserAccount> {


    void initUserAccount(Long userId);

    void saveUserAccountDetail(Long userId, String title, String tradeType, BigDecimal amount, String orderNo);

    BigDecimal getAvailableAmount(Long userId);

    void checkAndDeduct(AccountDeductVo accountDeductVo);

}
