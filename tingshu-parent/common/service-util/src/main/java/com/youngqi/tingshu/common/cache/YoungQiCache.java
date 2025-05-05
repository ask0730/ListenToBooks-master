package com.youngqi.tingshu.common.cache;


import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface YoungQiCache {

    String prefix() default "data:";
}
