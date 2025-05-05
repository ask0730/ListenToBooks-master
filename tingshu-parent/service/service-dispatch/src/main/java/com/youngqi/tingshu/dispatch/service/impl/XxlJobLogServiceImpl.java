package com.youngqi.tingshu.dispatch.service.impl;

import com.youngqi.tingshu.dispatch.mapper.XxlJobLogMapper;
import com.youngqi.tingshu.dispatch.service.XxlJobLogService;
import com.youngqi.tingshu.model.dispatch.XxlJobLog;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"all"})
public class XxlJobLogServiceImpl extends ServiceImpl<XxlJobLogMapper, XxlJobLog> implements XxlJobLogService {

	@Autowired
	private XxlJobLogMapper xxlJobLogMapper;

}
