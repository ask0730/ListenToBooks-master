package com.youngqi.tingshu.common.login;

/*
*
* 自定义注解  验证用户状态
*   元注解：
*       @Target注解使用位置：  类、方法、属性，方法参数，构造等
*       @Retention注解的生命周期（注解会保留到什么阶段 SOURCE源码、CLASS字节码、RUNTIME）
*       @Inherited是否能被继承
*       @Documented：通过JDK提供的javadoc命令产生文档是否生成该注解的文档
*
*
* */


import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface YoungQiLogin {

    /*
     *      是否必须登录 ,默认必须登录
     *      @return boolean
     *
     *
     **/
    boolean requiredLogin() default true;
}
