package com.youngqi.tingshu.album.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.youngqi.tingshu.album.mapper.*;
import com.youngqi.tingshu.album.mapper.*;
import com.youngqi.tingshu.album.service.BaseCategoryService;
import com.youngqi.tingshu.common.cache.YoungQiCache;
import com.youngqi.tingshu.model.album.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youngqi.tingshu.model.album.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"all"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

	@Autowired
	private BaseCategory1Mapper baseCategory1Mapper;

	@Autowired
	private BaseCategory2Mapper baseCategory2Mapper;

	@Autowired
	private BaseCategory3Mapper baseCategory3Mapper;

	@Autowired
	private BaseCategoryViewMapper baseCategoryViewMapper;
	@Autowired
	private BaseAttributeMapper baseAttributeMapper;
	/*
	*
	* 查视图，查所有分类,在业务中处理分级
	*   放redis缓存
	* */

	@Override
	@YoungQiCache(prefix = "baseCategoryList:")
	public List<JSONObject> getAllBaseCategoryList() {
		List<JSONObject> resList = new ArrayList<>();
		//查视图出来的包含1，2，3级的BaseCategoryView集合
		List<BaseCategoryView> baseCategoryViewsList = baseCategoryViewMapper.selectList(null);

		//用steam流分出1级map，k为1级id，v为BaseCategoryView对象
		Map<Long, List<BaseCategoryView>> c1Map = baseCategoryViewsList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
		if (CollectionUtil.isNotEmpty(c1Map)){
			//处理1级分类
			for (Map.Entry<Long,List<BaseCategoryView>> e1:c1Map.entrySet()) {
				//1级分类id
				Long c1Id = e1.getKey();
				//1级分类名称
				String category1Name = e1.getValue().get(0).getCategory1Name();
				JSONObject jsonObject = new JSONObject();
					jsonObject.put("categoryId",c1Id);
					jsonObject.put("categoryName",category1Name);

			//对每个1级分类的2级分类进行处理
				List<BaseCategoryView> everyC1List = e1.getValue();
				Map<Long, List<BaseCategoryView>> c2Map = everyC1List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
				//2级child
					List<Object> c2List = new ArrayList<>();
					if (CollectionUtil.isNotEmpty(c2Map)){
				for (Map.Entry<Long,List<BaseCategoryView>> e2:c2Map.entrySet()){
					Long c2Id = e2.getKey();
					String category2Name = e2.getValue().get(0).getCategory2Name();
					JSONObject jsonObject2 = new JSONObject();
								jsonObject2.put("categoryId",c2Id);
								jsonObject2.put("categoryName",category2Name);
								c2List.add(jsonObject2);

					List<BaseCategoryView> everyC2List = e2.getValue();
					//3级child
						List c3List=new ArrayList();
					Map<Long, List<BaseCategoryView>> c3Map = everyC2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory3Id));
					if (CollectionUtil.isNotEmpty(c3Map)){
						for (Map.Entry<Long,List<BaseCategoryView>> e3:c3Map.entrySet()){
							Long c3Id = e3.getKey();
							String category3Name = e3.getValue().get(0).getCategory3Name();
							JSONObject jsonObject3 = new JSONObject();
							jsonObject3.put("categoryId",c3Id);
							jsonObject3.put("categoryName",category3Name);
							c3List.add(jsonObject3);
						}
					}
					jsonObject2.put("categoryChild",c3List);
				}
				jsonObject.put("categoryChild",c2List);
					}
				resList.add(jsonObject);
			}

		}

		return resList;
	}

	@Override
	@YoungQiCache(prefix = "category1Id:attributes:")
	public List<BaseAttribute> getAttributeAndValueByC1Id(Long category1Id) {

		return baseAttributeMapper.getAttributeAndValueByC1Id(category1Id);
	}

	@Override
	@YoungQiCache(prefix = "category3View:")
	public BaseCategoryView getCategoryView(Long category3Id) {
		return baseCategoryViewMapper.selectById(category3Id);
	}

	@Override
	@YoungQiCache(prefix = "baseCategory3Top7:")
	public List<BaseCategory3> getTop7BaseCategory3(Long category1Id) {
		QueryWrapper<BaseCategory2> queryWrapperC2 = new QueryWrapper<>();
		queryWrapperC2.eq("category1_id",category1Id);
		queryWrapperC2.select("id");
		List<BaseCategory2> baseCategory2List = baseCategory2Mapper.selectList(queryWrapperC2);
		if (CollectionUtil.isNotEmpty(baseCategory2List)){
			List<Long> C2IdLsit = baseCategory2List.stream().map(b2 -> b2.getId()).collect(Collectors.toList());
			QueryWrapper<BaseCategory3> queryWrapperC3 = new QueryWrapper<>();
					queryWrapperC3.in("category2_id",C2IdLsit);
					queryWrapperC3.eq("is_top",1);
					queryWrapperC3.orderByAsc("order_num");
					queryWrapperC3.last("limit 7");

			List<BaseCategory3> baseCategory3List = baseCategory3Mapper.selectList(queryWrapperC3);

			return baseCategory3List;
		}

		return null;
	}

	@Override
	@YoungQiCache(prefix = "baseCategoryListByCategoryId:")
	public JSONObject getAllBaseCategoryListById(Long category1Id) {
		QueryWrapper<BaseCategoryView> queryWrapperC1 = new QueryWrapper<>();
		queryWrapperC1.eq("category1_id",category1Id);
		List<BaseCategoryView> baseCategoryByC1 = baseCategoryViewMapper.selectList(queryWrapperC1);
		if (CollectionUtil.isNotEmpty(baseCategoryByC1)){
			JSONObject jsonObjectC1 = new JSONObject();
			jsonObjectC1.put("categoryId",baseCategoryByC1.get(0).getCategory1Id());
			jsonObjectC1.put("categoryName",baseCategoryByC1.get(0).getCategory1Name());
			//根据c2的id进行分类
			Map<Long, List<BaseCategoryView>> MapByC2Id = baseCategoryByC1.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
			List<JSONObject> c2List = new ArrayList<>();
			for (Map.Entry<Long,List<BaseCategoryView>> e2:MapByC2Id.entrySet()){
				JSONObject jsonObjectC2 = new JSONObject();
						jsonObjectC2.put("categoryId",e2.getKey());
						jsonObjectC2.put("categoryName",e2.getValue().get(0).getCategory2Name());
						c2List.add(jsonObjectC2);
				List<BaseCategoryView> c3ByC2Id = e2.getValue();
				List<JSONObject> c3List = new ArrayList<>();
				for (BaseCategoryView c3:c3ByC2Id){
					JSONObject jsonObjectC3 = new JSONObject();
					jsonObjectC3.put("categoryId",c3.getCategory3Id());
					jsonObjectC3.put("categoryName",c3.getCategory3Name());
					c3List.add(jsonObjectC3);
				}
				jsonObjectC2.put("categoryChild",c3List); //三级放2级里
			}
			jsonObjectC1.put("categoryChild",c2List);//2级放1级里
			return jsonObjectC1;
		}
		return null;
	}


}
