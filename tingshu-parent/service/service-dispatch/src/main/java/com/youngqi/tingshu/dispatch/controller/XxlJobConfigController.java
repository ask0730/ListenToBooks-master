package com.youngqi.tingshu.dispatch.controller;

import com.youngqi.tingshu.dispatch.service.XxlJobConfigService;
import com.youngqi.tingshu.dispatch.service.XxlJobLogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "XxlJobConfig管理")
@RestController
@RequestMapping(value="/admin/dispatch/xxlJobConfig")
@SuppressWarnings({"all"})
public class XxlJobConfigController {
	
	@Autowired
	private XxlJobConfigService xxlJobConfigService;

	@Autowired
	private XxlJobLogService xxlJobLogService;

}

