package com.zc.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author 周聪
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestParam {
    String value() default "";
}