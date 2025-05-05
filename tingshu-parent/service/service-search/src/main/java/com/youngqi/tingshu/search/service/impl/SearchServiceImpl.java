package com.youngqi.tingshu.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Buckets;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import co.elastic.clients.json.JsonData;
import com.alibaba.fastjson.JSON;
import com.youngqi.tingshu.album.AlbumFeignClient;
import com.youngqi.tingshu.common.constant.RedisConstant;
import com.youngqi.tingshu.common.constant.SystemConstant;
import com.youngqi.tingshu.model.album.*;
import com.youngqi.tingshu.model.album.*;
import com.youngqi.tingshu.model.search.AlbumInfoIndex;
import com.youngqi.tingshu.model.search.AttributeValueIndex;
import com.youngqi.tingshu.model.search.SuggestIndex;
import com.youngqi.tingshu.query.search.AlbumIndexQuery;
import com.youngqi.tingshu.search.respository.AlbumInfoIndexRepositroy;
import com.youngqi.tingshu.search.respository.SuggestIndexRepository;
import com.youngqi.tingshu.search.service.SearchService;
import com.youngqi.tingshu.user.client.UserFeignClient;
import com.youngqi.tingshu.vo.album.TrackStatMqVo;
import com.youngqi.tingshu.vo.search.AlbumInfoIndexVo;
import com.youngqi.tingshu.vo.search.AlbumSearchResponseVo;
import com.youngqi.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"all"})
public class SearchServiceImpl implements SearchService {
    @Autowired
    private AlbumInfoIndexRepositroy albumInfoIndexRepositroy;
    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    private static final String INDEX_NAME="albuminfo";
    private static final String SUGGEST_INDEX_NAME="suggestinfo";

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    //添加索引库索引
    @Operation(summary = "专辑用于上架(测试)")
    @Override
    public void upperAlbum(Long albumId) {
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //处理专辑基本信息，不依赖任何任务，但当前任务要有返回值
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            Assert.notNull(albumInfo, "专辑不存在，专辑ID{}", albumId);
            //封装基本信息
            BeanUtil.copyProperties(albumInfo, albumInfoIndex);
            //封装标签信息
            List<AlbumAttributeValue> albumAttributeValueVoList = albumInfo.getAlbumAttributeValueVoList();
            if (CollectionUtil.isNotEmpty(albumAttributeValueVoList)) {
                List<AttributeValueIndex> attributeValueIndexList = albumAttributeValueVoList.stream().map(albumAttributeValue -> {
                    AttributeValueIndex attributeValueIndex = new AttributeValueIndex();
                    attributeValueIndex.setAttributeId(albumAttributeValue.getAttributeId());
                    attributeValueIndex.setValueId(albumAttributeValue.getValueId());
                    return attributeValueIndex;
                }).collect(Collectors.toList());
                albumInfoIndex.setAttributeValueIndexList(attributeValueIndexList);
            }
            return albumInfo;
        });


        //异步封装分类信息
        CompletableFuture<Void> categoryViewCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {

            BaseCategoryView baseCategoryView = albumFeignClient.getCategoryView(albumInfo.getCategory3Id()).getData();
            Assert.notNull(baseCategoryView, "分类不存在，分类ID：{}", albumInfo.getCategory3Id());
            albumInfoIndex.setCategory1Id(baseCategoryView.getCategory1Id());
            albumInfoIndex.setCategory2Id(baseCategoryView.getCategory2Id());
        }, threadPoolExecutor);


        //封装主播名称
        CompletableFuture<Void> userInfoCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVoById(albumInfo.getUserId()).getData();
            Assert.notNull(userInfoVo, "主播信息不存在，主播ID：{}", albumInfo.getUserId());
            albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());}, threadPoolExecutor);

        //假装去远程调用
        CompletableFuture<Void> stateCompletableFuture = CompletableFuture.runAsync(() -> {
          // 封装统计信息，采用产生随机值 以及专辑热度
//            AlbumStatVo data = albumFeignClient.getAlbumStatVo(albumId).getData();
            //随机为专辑产生播放量，订阅量，购买量，评论量
            int num1 = RandomUtil.randomInt(1000, 2000);
            int num2 = RandomUtil.randomInt(500, 1000);
            int num3 = RandomUtil.randomInt(200, 400);
            int num4 = RandomUtil.randomInt(100, 200);
            albumInfoIndex.setPlayStatNum(num1);
            albumInfoIndex.setSubscribeStatNum(num2);
            albumInfoIndex.setBuyStatNum(num3);
            albumInfoIndex.setCommentStatNum(num4);
            // 基于统计值计算出专辑得分 为不同统计类型设置不同权重
            BigDecimal bigDecimal1 = new BigDecimal(num4).multiply(new BigDecimal("0.4"));
            BigDecimal bigDecimal2 = new BigDecimal(num3).multiply(new BigDecimal("0.3"));
            BigDecimal bigDecimal3 = new BigDecimal(num2).multiply(new BigDecimal("0.2"));
            BigDecimal bigDecimal4 = new BigDecimal(num1).multiply(new BigDecimal("0.1"));
            BigDecimal hotScore = bigDecimal1.add(bigDecimal2).add(bigDecimal3).add(bigDecimal4);
            albumInfoIndex.setHotScore(hotScore.doubleValue());

        }, threadPoolExecutor);

        //组合异步任务（四个异步任务全部组合完毕，主线程继续）
        CompletableFuture.allOf(albumInfoCompletableFuture,
                        categoryViewCompletableFuture,
                        userInfoCompletableFuture,
                        stateCompletableFuture)
                        .join();
        //添加专辑到索专辑引库
        albumInfoIndexRepositroy.save(albumInfoIndex);
        //添加专辑信息到提词库
        this.saveSuggestIndex(albumInfoIndex);
        //将新增的商品SKUID存入布隆过滤器
        //获取布隆过滤器，将新增skuID存入布隆过滤器
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        bloomFilter.add(albumId);

    }

    @Autowired
    private SuggestIndexRepository suggestIndexRepository;
    /*
    *
    * 添加专辑到提词库
    * */
    private void saveSuggestIndex(AlbumInfoIndex albumInfoIndex) {
        SuggestIndex suggestIndex = new SuggestIndex();
        suggestIndex.setId(albumInfoIndex.getId().toString());
        //将专辑标题内容作为原始内容存入提词索引库(专辑名称)
        String albumTitle = albumInfoIndex.getAlbumTitle();
        suggestIndex.setTitle(albumTitle);
        //标题
        suggestIndex.setKeyword(new Completion(new String[]{albumTitle}));
        //拼音(中文转拼音)
        suggestIndex.setKeywordPinyin(new Completion(new String[]{PinyinUtil.getPinyin(albumTitle,"")}));
        suggestIndex.setKeywordSequence(new Completion(new String[]{PinyinUtil.getFirstLetter(albumTitle,"")}));
        suggestIndexRepository.save(suggestIndex);

    }



    //    删除文档库索引
    @Operation(summary = "专辑下架(测试)")
    @Override
    public void lowerAlbum(Long albumId) {

        albumInfoIndexRepositroy.deleteById(albumId);
        suggestIndexRepository.deleteById(albumId.toString());
    }

    /**
     * @Author YSQ
     * @Description  站内检索
     * @param albumIndexQuery 查询条件对象：关键字、分类id、标签列表、排序、分页信息
     * @return 专辑列表和分页信息
     */

    @Autowired
    private ElasticsearchClient esClient;

    @Override
    public AlbumSearchResponseVo searchAlbum(AlbumIndexQuery queryVo) {
        // 一、构建检索请求对象SearchRequest对象
        SearchRequest searchRequest=this.buildDSL(queryVo);
        log.error("这是searchRequest{}",searchRequest);
        //二、原生ES客户端对象进行检索
        SearchResponse<AlbumInfoIndex> searchResponse = null;
        try {
            searchResponse = esClient.search(searchRequest, AlbumInfoIndex.class);
            //三、解析ES响应结果
            return this.parseResult(searchResponse,queryVo);
        } catch (IOException e) {
            log.error("[搜索服务]查询条件:{}，站内检索异常:{}",queryVo,e);
            throw new RuntimeException(e);
        }


    }



    /*
    *
    * 查询一级下每个三级类的热门专辑
    * */
    @Override
    public List<Map<String, Object>> getTopCategory3HotAlbumList(Long category1Id) {
        List<BaseCategory3> baseCategory3List = albumFeignClient.getTop7BaseCategory3(category1Id).getData();
        Assert.notNull(baseCategory3List,"一级分类{}未包含置顶三级分类",category1Id);
        //拿到一级分类下所有三级分类id的集合
        List<Long>  baseCategory3IdList = baseCategory3List.stream().map(BaseCategory3::getId).collect(Collectors.toList());
        //三级分类转换成Map（返回值）
        Map<Long, BaseCategory3> baseCategory3Map  = baseCategory3List.stream().collect(Collectors.toMap(BaseCategory3::getId, baseCategory3 -> baseCategory3));
        //将置顶三级分类ID转为FieldValue类型
        List<FieldValue> fieldValueList = baseCategory3IdList.stream().map(category3Id -> FieldValue.of(category3Id)).collect(Collectors.toList());
        //ES检索三级分类（7个）不同三级分类下热度前6个的专辑列表
    SearchResponse<AlbumInfoIndex> searchResponse=null;
        try {
           searchResponse = esClient.search(s -> s.index(INDEX_NAME)
                    .query(q -> q.terms(t -> t.field("category3Id").terms(tf -> tf.value(fieldValueList))))
                    .size(0)
                    .aggregations("category3IdAgg", a -> a.terms(t -> t.field("category3Id").size(10)).aggregations("top6Agg", at -> at.topHits(tp -> tp.size(6).sort(sort -> sort.field(f -> f.field("hotScore").order(SortOrder.Desc)))))), AlbumInfoIndex.class);
            System.out.println(searchResponse);
        } catch (Exception e) {
           log.error("[检索服务]首页热门专辑异常:{}",e);
           throw new RuntimeException(e);
        }

        //解析结果

        //获取三级分类的聚合对象
        Map<String, Aggregate> aggregations = searchResponse.aggregations();
        Aggregate category3IdAgg = aggregations.get("category3IdAgg");
        //获取三级分类聚合“桶”集合
        Buckets<LongTermsBucket> buckets = category3IdAgg.lterms().buckets();
        List<LongTermsBucket> bucketList = buckets.array();
        if (CollectionUtil.isNotEmpty(bucketList)){
            List<Map<String, Object>>listMap= bucketList.stream().map(bucket -> {
                Map<String, Object> map = new HashMap<>();
                long category3Id = bucket.key(); //三级分类id
                BaseCategory3 baseCategory3 = baseCategory3Map.get(category3Id);
                map.put("baseCategory3", baseCategory3);
                //取下钻找热门专辑
                Aggregate top6Agg = bucket.aggregations().get("top6Agg");
                List<Hit<JsonData>> hits = top6Agg.topHits().hits().hits();
                if (CollectionUtil.isNotEmpty(hits)) {
                    List<AlbumInfoIndex> hotAlbumList = hits.stream().map(hit -> {
                        //获取6个热门专辑的json对象
                        JsonData source = hit.source();
                        return JSON.parseObject(source.toString(), AlbumInfoIndex.class);
                    }).collect(Collectors.toList());
                    map.put("list", hotAlbumList);
                }
                return map;
            }).collect(Collectors.toList());
            return listMap;

        }


        return null;
    }
    /*
    *
    * 关键词自动补全
    * */
    @Override
    public List<String> completeSuggest(String keyword) {

        try {
            SearchResponse<SuggestIndex> searchResponse = esClient.search(s -> s.index(SUGGEST_INDEX_NAME)
                    .suggest(su -> su.suggesters("mySuggestKeyword", fs -> fs.prefix(keyword).completion(c -> c.field("keyword").size(10).skipDuplicates(true)))
                            .suggesters("mySuggestPinYin", fs -> fs.prefix(PinyinUtil.getPinyin(keyword, "")).completion(c -> c.field("keywordPinyin").size(10).skipDuplicates(true)))
                                    .suggesters("mySuggestSequence", fs -> fs.prefix(PinyinUtil.getFirstLetter(keyword, "")).completion(c -> c.field("keywordSequence").size(10).skipDuplicates(true))))
                   , SuggestIndex.class);



            HashSet<String> hashSet = new HashSet<>();//解析结果,并进行去重(用hashSet)


           hashSet.addAll( parseSuggestResult("mySuggestKeyword",searchResponse));
           hashSet.addAll( parseSuggestResult("mySuggestPinYin",searchResponse));
           hashSet.addAll( parseSuggestResult("mySuggestSequence",searchResponse));
            if (hashSet.size()>=10){
                return new ArrayList<>(hashSet).subList(0,10);//第一个参数是开始的，第二个参数是排除的
            }
            //如果提词结果小于10，尝试采用全文查询进行补全
            if (hashSet.size()<10){
                SearchResponse<AlbumInfoIndex> response = esClient
                        .search(s -> s.index(INDEX_NAME).query(q -> q.match(m -> m.field("albumTitle").query(keyword))),
                                AlbumInfoIndex.class);
                //解析结果，将结果放入set
                List<Hit<AlbumInfoIndex>> hits = response.hits().hits();
                if (CollectionUtil.isNotEmpty(hits)){
                    for (Hit<AlbumInfoIndex> hit : hits) {
                        AlbumInfoIndex albumInfoIndex = hit.source();
                        hashSet.add(albumInfoIndex.getAlbumTitle());
                        if (hashSet.size()>=10){
                            break;
                        }
                    }
                }
            }
            return new ArrayList<>(hashSet);
        } catch (IOException e) {
            log.error("[搜索服务]关键词补全异常:{}",e);
            throw new RuntimeException(e);
        }


    }
      /*
       *
       * 复用解析结果逻辑
       *  关键词补全的结果解析
       * */
    @Override
    public Collection<String> parseSuggestResult(String suggestName, SearchResponse<SuggestIndex> searchResponse) {
        List<Suggestion<SuggestIndex>> suggestionList = searchResponse.suggest().get(suggestName);
        List<String> list = new ArrayList<>();
        for (Suggestion<SuggestIndex> suggestion : suggestionList) {
            for (CompletionSuggestOption<SuggestIndex> option : suggestion.completion().options()) {
                SuggestIndex suggestIndex= option.source();
                list.add(suggestIndex.getTitle());
            }
        }
        return list;
    }



    /*
    * 封装检索请求（查询条件query，分页from，size，排序，sort，高亮 highlight，字段指定_source）
    * */
    private SearchRequest buildDSL(AlbumIndexQuery albumIndexQuery) {

        //创建请求构建器对象
        SearchRequest.Builder builder = new SearchRequest.Builder();
        //指定索引库
        builder.index(INDEX_NAME);
        //设置请求参数"query"处理查询条件(关键词、分类、标签)

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder(); //构建最外层bool查询
        String keyword = albumIndexQuery.getKeyword();
        //处理关键词查询
        if (StringUtils.isNotBlank(keyword)){
            BoolQuery.Builder keyWordBoolQueryBuilder = new BoolQuery.Builder();
            keyWordBoolQueryBuilder.should(s->s.match(m->m.field("albumTitle").query(keyword)));//标题匹配
            keyWordBoolQueryBuilder.should(s->s.match(m->m.field("albumIntro").query(keyword)));//专辑简介匹配
            keyWordBoolQueryBuilder.should(s->s.term(t->t.field("announcerName").value(keyword)));//精确查询作者名称
            boolQueryBuilder.must(keyWordBoolQueryBuilder.build()._toQuery());//将关键词查询传入最外层查询

        }
        //处理分类Id查询
        if (albumIndexQuery.getCategory1Id()!=null){
            boolQueryBuilder.filter(f->f.term(t->t.field("category1Id").value(albumIndexQuery.getCategory1Id())));

        }
        if (albumIndexQuery.getCategory2Id()!=null){
            boolQueryBuilder.filter(f->f.term(t->t.field("category2Id").value(albumIndexQuery.getCategory2Id())));
        }
        if (albumIndexQuery.getCategory3Id()!=null){
            boolQueryBuilder.filter(f->f.term(t->t.field("category3Id").value(albumIndexQuery.getCategory3Id())));
        }
        //处理标签查询
        List<String> attributeList = albumIndexQuery.getAttributeList();
        if (CollectionUtil.isNotEmpty(attributeList)){
            for (String attribute:attributeList){
                String[] split = attribute.split(":");
                if (split!=null&&split.length==2){
                    NestedQuery.Builder nestedQueryBuilder = new NestedQuery.Builder();//创建一个Nested的builder
                    nestedQueryBuilder.path("attributeValueIndexList");
                    BoolQuery.Builder nestedBoolQueryBuilder = new BoolQuery.Builder();
                    nestedBoolQueryBuilder.must(m->m.term(t->t.field("attributeValueIndexList.attributeId").value(split[0])));
                    nestedBoolQueryBuilder.must(m->m.term(t->t.field("attributeValueIndexList.valueId").value(split[1])));
                    nestedQueryBuilder.query(nestedBoolQueryBuilder.build()._toQuery());
                    boolQueryBuilder.filter(nestedQueryBuilder.build()._toQuery());
                }
            }
        }
        builder.query(boolQueryBuilder.build()._toQuery());

        //处理分页from,size
        Integer size = albumIndexQuery.getPageSize();
        Integer from=(albumIndexQuery.getPageNo()-1)*size;
        builder.from((from)).size(size);
        //sort排序(判断)
            String order = albumIndexQuery.getOrder();
            if (StringUtils.isNotBlank(order)){//判断排序是否提交
                String[] split = order.split(":");
                if (split!=null&&split.length==2){  //判断是否为真实数据
                    String orderFiled="";
                    switch (split[0]){
                        case "1":
                            orderFiled="hotScore";
                            break;
                        case "2":
                            orderFiled="playStatNum";
                            break;
                        case "3":
                            orderFiled="createTime";
                            break;
                    }
                    String finalOrderFiled = orderFiled;
                    builder.sort(b->b.field(f->f.field(finalOrderFiled).order("asc".equals(split[1])? SortOrder.Asc:SortOrder.Desc)));

                }
            }                                     //排序方式：(1.综合，2.播放量,3.发布时间)
        //请求体“highlight”处理高亮，关键！：录入关键字
        if (StringUtils.isNotBlank(albumIndexQuery.getKeyword())){
            builder.highlight(h->h.fields("albumTitle",f->f.preTags("<font style=color:red>").postTags("</font>")));
        }

        //设置请求体参数"_source"
        builder.source(s->s.filter(f->f.excludes("category1Id","category2Id","category3Id",
                "attributeValueIndexList.attributeId","attributeValueIndexList.valueId")));

        return builder.build();
    }

    /*
    *
    * 解析ES检索的响应结果
    * */

    private AlbumSearchResponseVo parseResult(SearchResponse<AlbumInfoIndex> searchResponse,AlbumIndexQuery queryVo) {
        AlbumSearchResponseVo albumSearchResponseVo = new AlbumSearchResponseVo();
       //封装分页数据
        albumSearchResponseVo.setPageNo(queryVo.getPageNo());
        albumSearchResponseVo.setPageSize(queryVo.getPageSize());
        long total = searchResponse.hits().total().value(); //总记录数
        albumSearchResponseVo.setTotal(total);//总记录数

        Integer pageSize = queryVo.getPageSize();//页数大小
        long totalPages=total%pageSize==0?total/pageSize:total/pageSize+1; //看是否能整除决定多少页
        albumSearchResponseVo.setTotalPages(totalPages);//总页数
       //封装响应数据
        List<Hit<AlbumInfoIndex>> hits = searchResponse.hits().hits();
        if (CollectionUtil.isNotEmpty(hits)){
            List<AlbumInfoIndexVo> indexVoList = hits.stream()
                    .map(hit -> {
                        AlbumInfoIndexVo albumInfoIndexVo = BeanUtil.copyProperties(hit.source(), AlbumInfoIndexVo.class);
                        //处理高亮
                        Map<String, List<String>> highlightMap = hit.highlight();
                        if (CollectionUtil.isNotEmpty(highlightMap)&&highlightMap.containsKey("albumTitle")){
                            List<String> highlightList = highlightMap.get("albumTitle");
                            String highLightTitle = highlightList.get(0);
                            albumInfoIndexVo.setAlbumTitle(highLightTitle);
                        }
                        return albumInfoIndexVo;
                    }).collect(Collectors.toList());
            albumSearchResponseVo.setList(indexVoList);
        }


        return albumSearchResponseVo;
    }

    /*
    *
    * 更新检索中的专辑统计信息
    * */
    @Override
    public void updateStat(TrackStatMqVo trackStatMqVo) {
        String key="seachmd:"+trackStatMqVo.getBusinessNo();
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, trackStatMqVo.getTrackId(), 1, TimeUnit.HOURS);
        try {
            AlbumInfoIndex albumInfoIndex = albumInfoIndexRepositroy.findById(trackStatMqVo.getAlbumId()).get();
            Assert.notNull(albumInfoIndex,"专辑{}不存在",trackStatMqVo.getAlbumId());
            if (flag){
                if (SystemConstant.TRACK_STAT_PLAY.equals(trackStatMqVo.getStatType())){
                    albumInfoIndex.setPlayStatNum(albumInfoIndex.getPlayStatNum()+1);
                    albumInfoIndexRepositroy.save(albumInfoIndex);
                }
                if (SystemConstant.TRACK_STAT_COMMENT.equals(trackStatMqVo.getStatType())){
                    albumInfoIndex.setCommentStatNum(albumInfoIndex.getCommentStatNum()+1);
                }
            }
        } catch (IllegalArgumentException e) {
            redisTemplate.delete(key);
           throw new RuntimeException(e);
        }


    }

    @Override
    public void updateLatelyAlbumRanking() {
        //获取一级分类列表
        try {
            List<BaseCategory1> category1List = albumFeignClient.getCategory1List().getData();
            Assert.notNull(category1List,"一级分类为空");
            for (BaseCategory1 baseCategory1 : category1List) {
                Long category1Id = baseCategory1.getId();
                String [] rankingDimensionArray=new String[]{
                        "hotScore", "playStatNum", "subscribeStatNum", "buyStatNum", "commentStatNum"
                };
                for (String rankingDimension : rankingDimensionArray) {
                    SearchResponse<AlbumInfoIndex> searchResponse =
                            esClient.search(s -> s.index(INDEX_NAME).query(q -> q.term(t -> t.field("category1Id").value(category1Id)))
                            .sort(sort -> sort.field(f -> f.field(rankingDimension).order(SortOrder.Desc)))
                            .size(10), AlbumInfoIndex.class);

                    List<Hit<AlbumInfoIndex>> hits = searchResponse.hits().hits();
                    if (CollectionUtil.isNotEmpty(hits)){
                        List<AlbumInfoIndex> albumInfoIndexList = hits.stream().map(hit -> hit.source()).collect(Collectors.toList());

                        String key= RedisConstant.RANKING_KEY_PREFIX+category1Id;
                        redisTemplate.opsForHash().put(key,rankingDimension,albumInfoIndexList);
                    }
                }

            }
        } catch (IOException e) {
            log.error("[搜索服务]更新排行榜异常：{}", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<AlbumInfoIndex> findRankingList(Long category1Id, String dimension) {
        String key =RedisConstant.RANKING_KEY_PREFIX+category1Id;
        Boolean flag = redisTemplate.opsForHash().hasKey(key, dimension);
        if (flag){
            List<AlbumInfoIndex> list =(List) redisTemplate.opsForHash().get(key, dimension);
            return list;
        }
        return null;
    }


}
