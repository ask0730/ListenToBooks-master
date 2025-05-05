package com.youngqi.tingshu.dispatch.service.impl;

import com.youngqi.tingshu.dispatch.mapper.XxlJobConfigMapper;
import com.youngqi.tingshu.dispatch.service.XxlJobConfigService;
import com.youngqi.tingshu.model.dispatch.XxlJobConfig;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"all"})
public class XxlJobConfigServiceImpl extends ServiceImpl<XxlJobConfigMapper, XxlJobConfig> implements XxlJobConfigService {

	@Autowired
	private XxlJobConfigMapper xxlJobConfigMapper;
}
