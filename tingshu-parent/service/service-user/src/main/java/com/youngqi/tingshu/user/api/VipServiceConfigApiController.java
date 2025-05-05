package com.youngqi.tingshu.user.api;

import com.youngqi.tingshu.common.result.Result;
import com.youngqi.tingshu.model.user.VipServiceConfig;
import com.youngqi.tingshu.user.service.VipServiceConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "VIP服务配置管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class VipServiceConfigApiController {

	@Autowired
	private VipServiceConfigService vipServiceConfigService;


	@Operation(summary = "获取全部VIP会员服务配置信息")
	@GetMapping("/vipServiceConfig/findAll")
	public Result<List<VipServiceConfig>> findAll(){
		List<VipServiceConfig> vipServiceConfigList = vipServiceConfigService.list();

		return Result.ok(vipServiceConfigList);
	}
	@Operation(summary = "根据id获取VIP服务配置信息")
	@GetMapping("/vipServiceConfig/getVipServiceConfig/{id}")
	public Result<VipServiceConfig> getVipServiceConfig(@PathVariable Long id){
		VipServiceConfig vipServiceConfig = vipServiceConfigService.getById(id);
		return Result.ok(vipServiceConfig);
	}


}

