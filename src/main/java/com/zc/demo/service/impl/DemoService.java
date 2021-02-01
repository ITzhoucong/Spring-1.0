package com.zc.demo.service.impl;

import com.zc.demo.service.IDemoService;
import com.zc.mvcframework.annotation.MyService;

/**
 * @ClassName DemoService
 * @Author 周聪
 * @Date 2021/1/29 14:16
 * @Version 1.0
 * @Description
 */
@MyService
public class DemoService implements IDemoService {
    @Override
    public String get(String name) {
        return "My name is " + name;
    }
}
