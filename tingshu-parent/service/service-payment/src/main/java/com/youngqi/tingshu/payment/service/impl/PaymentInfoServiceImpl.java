package com.youngqi.tingshu.payment.service.impl;

import com.youngqi.tingshu.model.payment.PaymentInfo;
import com.youngqi.tingshu.payment.mapper.PaymentInfoMapper;
import com.youngqi.tingshu.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"all"})
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

}
