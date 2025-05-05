package com.youngqi.tingshu.model.base;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
public class BaseEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("create_time")
    private Date createTime;

    //   @JsonIgnore在json序列化时将java bean中的一些属性忽略掉，序列化和反序列化都受影响。
    @JsonIgnore
    @TableField("update_time")
    private Date updateTime;

    @JsonIgnore
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    @JsonIgnore
    @TableField(exist = false)
    private Map<String,Object> param = new HashMap<>();
}
