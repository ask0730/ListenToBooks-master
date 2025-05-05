package com.youngqi.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.youngqi.tingshu.album.service.BaseCategoryService;
import com.youngqi.tingshu.common.cache.YoungQiCache;
import com.youngqi.tingshu.common.result.Result;
import com.youngqi.tingshu.model.album.BaseAttribute;
import com.youngqi.tingshu.model.album.BaseCategory1;
import com.youngqi.tingshu.model.album.BaseCategory3;
import com.youngqi.tingshu.model.album.BaseCategoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "分类管理")
@RestController
@RequestMapping(value="/api/album")
@SuppressWarnings({"all"})
public class BaseCategoryApiController {

	@Autowired
	private BaseCategoryService baseCategoryService;

	@Operation(summary = "查询所有级分类")
	@GetMapping("/category/getBaseCategoryList")
	public Result<List<JSONObject>> getAllBaseCategoryList(){
		List allList=baseCategoryService.getAllBaseCategoryList();

		return Result.ok(allList);
	}

	@Operation(summary = "用1级id获取展示的标签名标签值")
	@GetMapping("/category/findAttribute/{category1Id}")
	public Result<List<BaseAttribute>> getAttributeAndValue(@PathVariable Long category1Id){
		List<BaseAttribute> avList	=baseCategoryService.getAttributeAndValueByC1Id(category1Id);
		return Result.ok(avList);
	}

	@Operation(summary = "根据三级分类查出所有分类信息，用于远程调用")
	@GetMapping("/category/getCategoryView/{category3Id}")
	public Result<BaseCategoryView> getCategoryView(@PathVariable Long category3Id){
			BaseCategoryView bcv=baseCategoryService.getCategoryView(category3Id);

		return 	Result.ok(bcv);
	}
	@Operation(summary = "根据一级分类查询三级分类列表（七个）")
	@GetMapping("/category/findTopBaseCategory3/{category1Id}")
	public Result<List<BaseCategory3>> getTop7BaseCategory3(@PathVariable Long category1Id){
		List<BaseCategory3> baseCategory3List=baseCategoryService.getTop7BaseCategory3(category1Id);

		return Result.ok(baseCategory3List);
	}
	@Operation(summary = "根据一级id查询所有分类")
	@GetMapping("/category/getBaseCategoryList/{category1Id}")
	public Result<JSONObject> getBaseCategoryListByC1Id(@PathVariable Long category1Id){
		JSONObject jsonObject =baseCategoryService.getAllBaseCategoryListById(category1Id);

		return Result.ok(jsonObject);
	}
	/**
	 * 查询所有一级分类列表
	 * @return
	 */
	@Operation(summary ="查询所有一级分类列表" )
	@GetMapping("/category/findAllCategory1")
	@YoungQiCache(prefix = "allCategory1:")
	public Result<List<BaseCategory1>> getCategory1List(){
		return Result.ok(baseCategoryService.list());
	}





}

