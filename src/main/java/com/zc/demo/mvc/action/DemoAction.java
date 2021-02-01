package com.zc.demo.mvc.action;

import com.zc.demo.service.IDemoService;
import com.zc.mvcframework.annotation.MyAutowired;
import com.zc.mvcframework.annotation.MyController;
import com.zc.mvcframework.annotation.MyRequestMapping;
import com.zc.mvcframework.annotation.MyRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @ClassName DemoAction
 * @Author 周聪
 * @Date 2021/1/29 10:52
 * @Version 1.0
 * @Description
 */
@MyController
@MyRequestMapping(value = "/demo")
public class DemoAction {
    @MyAutowired
    private IDemoService demoService;

    @MyRequestMapping(value = "query")
    public void query(HttpServletRequest req, HttpServletResponse resp, @MyRequestParam(value = "name") String name) {
//        String result = demoService.get(name);
        String result = "My name is " + name;
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @MyRequestMapping(value = "add")
    public void add(HttpServletRequest req, HttpServletResponse resp, @MyRequestParam(value = "a") Integer a, @MyRequestParam(value = "b") Integer b) {
        try {
            resp.getWriter().write(a + "+" + b + "=" + (a + b));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @MyRequestMapping("/remove")
    public String remove(@MyRequestParam("id") Integer id) {
        return "" + id;
    }

}
