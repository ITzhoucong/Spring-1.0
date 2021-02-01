package com.zc.mvcframework.annotation;

import java.lang.annotation.*;


/**
 * @author 周聪
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestMapping {
    String value() default "";
}