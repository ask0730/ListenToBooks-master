package com.youngqi.tingshu.search.client;

import com.youngqi.tingshu.search.client.impl.SearchDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>
 * 搜索模块远程调用API接口
 * </p>
 *
 * @author atguigu
 */
@FeignClient(value = "service-search", fallback = SearchDegradeFeignClient.class)
public interface SearchFeignClient {


}
