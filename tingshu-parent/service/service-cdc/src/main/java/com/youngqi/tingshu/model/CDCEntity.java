package com.youngqi.tingshu.model;

import lombok.Data;

import javax.persistence.Column;

/**
 * @author YSQ
 * @PackageName:com.atguigu.tingshu.model
 * @className: CDCModel
 * @Description:
 * @date 2025/3/8 12:22
 */
@Data
public class CDCEntity {
    //必须使用java持久层的注解
    @Column(name = "id")
    private Long id;
}
