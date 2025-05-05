package com.youngqi.tingshu.search.respository;

import com.youngqi.tingshu.model.search.SuggestIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
/*
*
* 映射文档类型 和主键类型
* */
public interface SuggestIndexRepository  extends ElasticsearchRepository<SuggestIndex,String> {
}
