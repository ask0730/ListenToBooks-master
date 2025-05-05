package com.youngqi.tingshu.account.service.impl;

import com.youngqi.tingshu.account.mapper.UserAccountDetailMapper;
import com.youngqi.tingshu.account.mapper.UserAccountMapper;
import com.youngqi.tingshu.account.service.UserAccountService;
import com.youngqi.tingshu.common.constant.SystemConstant;
import com.youngqi.tingshu.common.execption.GuiguException;
import com.youngqi.tingshu.model.account.UserAccount;
import com.youngqi.tingshu.model.account.UserAccountDetail;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youngqi.tingshu.vo.account.AccountDeductVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

	@Autowired
	private UserAccountMapper userAccountMapper;
	@Autowired
	private UserAccountDetailMapper userAccountDetailMapper;

	/*
	* 初始化账户信息
	* */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void initUserAccount(Long userId) {
		UserAccount userAccount = new UserAccount();
		userAccount.setUserId(userId);
		userAccount.setTotalAmount(new BigDecimal("100"));
		userAccount.setAvailableAmount(new BigDecimal("100"));
		userAccount.setTotalIncomeAmount(new BigDecimal("100"));
		userAccountMapper.insert(userAccount);
		if (userAccount.getAvailableAmount().intValue()>0){
			saveUserAccountDetail(userId,"赠送", SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT,userAccount.getAvailableAmount(),null);
		}

	}

	@Override
	public void saveUserAccountDetail(Long userId, String title, String tradeType, BigDecimal amount, String orderNo) {
		UserAccountDetail userAccountDetail = new UserAccountDetail();
		userAccountDetail.setUserId(userId);
		userAccountDetail.setTitle(title);
		userAccountDetail.setTradeType(tradeType);
		userAccountDetail.setAmount(amount);
		userAccountDetail.setOrderNo(orderNo);
		userAccountDetailMapper.insert(userAccountDetail);
	}

	@Override
	public BigDecimal getAvailableAmount(Long userId) {
		LambdaQueryWrapper<UserAccount> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(UserAccount::getUserId,userId);
		UserAccount userAccount = userAccountMapper.selectOne(queryWrapper);

		return userAccount.getAvailableAmount();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void checkAndDeduct(AccountDeductVo accountDeductVo) {
		int count =userAccountMapper.checkAndDeduct(accountDeductVo.getUserId(),accountDeductVo.getAmount());
		if (count==0){
			throw new GuiguException(400,"账户余额不足");
		}
		this.saveUserAccountDetail(accountDeductVo.getUserId(),
									accountDeductVo.getContent(),
									SystemConstant.ACCOUNT_TRADE_TYPE_MINUS,
									accountDeductVo.getAmount(),
									accountDeductVo.getOrderNo());


	}

}
