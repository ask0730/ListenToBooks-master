package com.youngqi.tingshu.account.service.impl;

import com.youngqi.tingshu.account.mapper.RechargeInfoMapper;
import com.youngqi.tingshu.account.service.RechargeInfoService;
import com.youngqi.tingshu.model.account.RechargeInfo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"all"})
public class RechargeInfoServiceImpl extends ServiceImpl<RechargeInfoMapper, RechargeInfo> implements RechargeInfoService {

	@Autowired
	private RechargeInfoMapper rechargeInfoMapper;

}
