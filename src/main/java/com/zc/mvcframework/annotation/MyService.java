package com.zc.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author 周聪
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyService {
    String value() default "";
}