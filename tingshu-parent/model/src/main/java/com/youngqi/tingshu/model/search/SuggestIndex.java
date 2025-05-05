package com.youngqi.tingshu.model.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.suggest.Completion;

@Data
@Document(indexName = "suggestinfo")
@JsonIgnoreProperties(ignoreUnknown = true)//目的：防止json字符串转成实体对象时因未识别字段报错
public class SuggestIndex {

    /*
    *
    * 提词原始内容（标题）
    * */
    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;
    /*
    * 用于提词的字段:汉字
    *
    * */
    @CompletionField(analyzer = "standard", searchAnalyzer = "standard", maxInputLength = 20)
    private Completion keyword;
    /*
    *
    * 拼音提词
    * */
    @CompletionField(analyzer = "standard", searchAnalyzer = "standard", maxInputLength = 20)
    private Completion keywordPinyin;
    /*
    *
    * 首字母提词
    * */
    @CompletionField(analyzer = "standard", searchAnalyzer = "standard", maxInputLength = 20)
    private Completion keywordSequence;

}
