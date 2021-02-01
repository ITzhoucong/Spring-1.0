package com.zc.mvcframework.v2.servlet;

import com.zc.mvcframework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;

/**
 * @ClassName MyDispatcherServlet
 * @Author 周聪
 * @Date 2021/1/29 11:50
 * @Version 1.0
 * @Description 用到工厂模式、注册式单例模式、委派模式、模板模式、策略模式（HandlerMappings解决了if...else...）
 */
public class MyDispatcherServlet extends HttpServlet {

    /**
     * 保存application。properties配置文件中的内容
     */
    private Properties contextConfig = new Properties();

    /**
     * 保存扫描的所有的类名
     */
    private List<String> classNames = new ArrayList<>();

    /**
     * 传说中的IOC容器，我们来揭开它的神秘面纱
     * 为了简化程序，暂时不考虑ConcurrentHashMap，主要还是关注设计思想
     */
    private Map<String, Object> ioc = new HashMap<String, Object>();

    /**
     * 保存url和method对应关系
     */
    private Map<String, Method> handlerMappings = new HashMap<String, Method>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        6、调用 运行阶段
        try {
            doDispatch(req, resp);

        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception, Detail : " + Arrays.toString(e.getStackTrace()));
        }
    }

    /**
     * 初始化阶段
     *
     * @param config
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
//        1、加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
//        2、扫描相关的类
        doScanner(contextConfig.getProperty("scanPackage"));
//        3、初始化扫描到的类，并且将它们放入到IOC容器之中
        doInstance();
//        4、完成依赖注入
        doAutowired();
//        5、初始化HandlerMapping
        initHandlerMapping();

        System.out.println("Spring framework is init");
    }

    /**
     * 执行请求调用方法
     *
     * @param req
     * @param resp
     */
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {

       /*
        basePath:http://localhost:8080/test/
        getContextPath:/test
        getServletPath:/test.jsp
        getRequestURI:/test/test.jsp
        getRequestURL:http://localhost:8080/test/test.jsp
        getRealPath:D:\Tomcat 6.0\webapps\test\
        getServletContext().getRealPath:D:\Tomcat 6.0\webapps\test\
        getQueryString:p=fuck
        */

//        绝对路径
        String requestURI = req.getRequestURI();
//        处理成相对路径
        String contextPath = req.getContextPath();
        String url = requestURI.replace(contextPath, "").replaceAll("/+", "/");
        if (!this.handlerMappings.containsKey(url)) {
            resp.getWriter().write("404 Not Found !!!");
            return;
        }
        Method method = this.handlerMappings.get(url);
//        投机取巧的方式
//        通过反射拿到method所在class,拿到class之后还会拿到class的名称
//        再调用toLowerFirstCase活动beanName
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
//        获取请求参数
        Map<String, String[]> params = req.getParameterMap();
//        获取方法的形参列表
        Class<?>[] parameterTypes = method.getParameterTypes();
//        获取方法的参数列表
        Parameter[] parameters = method.getParameters();
//        保存赋值参数的位置
        Object[] paramValues = new Object[parameterTypes.length];
//        根据参数位置动态赋值
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (parameterType.equals(HttpServletRequest.class)) {
                paramValues[i] = req;
                continue;
            } else if (parameterType.equals(HttpServletResponse.class)) {
                paramValues[i] = resp;
                continue;
            }
            MyRequestParam annotation = parameters[i].getAnnotation(MyRequestParam.class);
            if (params.containsKey(annotation.value())) {
//                            拿到key所对应的值,而拿到的这个值,有一对多的关系,一个key对应一个数组
//                            http://127.0.0.1:8080/Spring-1.0/demo/query?name=zhangsan&name=lisi&name=wangwu
                System.out.println(Arrays.toString(params.get(annotation.value())));
//                  类型的强制转换
                paramValues[i] = convert(parameterType, Arrays.toString(params.get(annotation.value())));
            }

        }

//        第一个参数:方法所在的实例,第二个参数:调用时所需要的参数
        method.invoke(ioc.get(beanName), paramValues);
    }

    /**
     * url传过来的参数都是String类型的,http是基于字符串协议
     * 只需要把String转换成任意类型
     *
     * @param type
     * @param value
     * @return
     */
    private Object convert(Class<?> type, String value) {
//        去掉数组的中括号
        value = value.replaceAll("\\[|\\]", "")
                .replaceAll("\\s", "");

//        如果是int
        if (Integer.class == type) {
            return Integer.valueOf(value);
        }
//        如果还有double或者其他类型,继续加if
//        这时候我们应该想到策略模式了
//        这里暂时不实现
        return value;
    }

    /**
     * 初始化url和Method绑定关系
     */
    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(MyController.class)) {
                continue;
            }
//            保存写在类上面的@MyRequestMapping(name = "/demo")
            String baseUrl = "";
            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                MyRequestMapping annotation = clazz.getAnnotation(MyRequestMapping.class);
                baseUrl = annotation.value();
            }

//            默认获取所有的public方法
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                    continue;
                }
                MyRequestMapping annotation = method.getAnnotation(MyRequestMapping.class);
//                优化,多个 / 替换成一个 ，用户不管自己写不写/,我都加一个/
                String url = ("/" + baseUrl + "/" + annotation.value()).replaceAll("/+", "/");
                handlerMappings.put(url, method);
                System.out.println("Mapped : " + url + " , " + method);
            }
        }
    }

    /**
     * 依赖注入
     */
    private void doAutowired() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
//            Declared 获取所有的，特定的字段 包括private/protected/default
//            正常来说，普通的OOP变成只能拿到public属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(MyAutowired.class)) {
                    continue;
                }
//                自定义beanName赋值
                MyAutowired annotation = field.getAnnotation(MyAutowired.class);
                String beanName = annotation.value().trim();
                if ("".equals(beanName)) {
//                    根据接口类型赋值
                    beanName = field.getType().getName();
                }
//                如果是public以外的修饰符，只要加了Autowired注解，都要强制赋值，暴力访问
                field.setAccessible(true);
                try {
//                    用反射机制，动态给字段赋值
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 初始化，为DI做准备
     */
    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
//                什么样的类才需要初始化？
//                加了注解的类，才初始化，怎么判断？
//                为了简化代码逻辑，主要体会设计思想，只举例@Controller和@Service
//                @Component ....就不一一举例了
                if (clazz.isAnnotationPresent(MyController.class)) {
                    Object instance = clazz.newInstance();
//                    Spring默认类名首字母小写
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, instance);
                } else if (clazz.isAnnotationPresent(MyService.class)) {
//                    1、自定义的beanName
                    MyService annotation = clazz.getAnnotation(MyService.class);
                    String beanName = annotation.value();
//                    2、默认类名首字母小写
                    if ("".equals(annotation.value().trim())) {
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
//                    3、根据类型自动赋值,投机取巧的方式
                    for (Class<?> i : clazz.getInterfaces()) {
                        if (ioc.containsKey(i.getName())) {
                            throw new Exception("The “" + i.getName() + "” is exists !");
                        }
                        ioc.put(i.getName(), instance);
                    }
                } else {
                    continue;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 类名首字母小写
     * 如果类名本身是小写字母确实会出现问题
     * 但是我要说明的是：这个方法是我自己用，private的
     * 传值也是自己传，类也都遵循了驼峰命名法
     * 默认传入的值，不存在首字母小写的情况，也不可能出现非首字母的情况
     * 为了简化简化程序就不做判断了
     *
     * @param simpleName
     * @return
     */
    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
//        之所以加32，是因为大小写字母的ASCII码相差32
//        大写字母的ASCII码要小于小写字母的ASCII码
//        在Java中，对char做算学运算，实际上就是对ASCII码做算学运算
        chars[0] += 32;
        return String.valueOf(chars);
    }

    /**
     * 扫描出相关的类
     *
     * @param scanPackage
     */
    private void doScanner(String scanPackage) {
//        scanPackage  =  com.zc.demo  存储的是包路径
//        转换为文件路径，实际上就是把.替换成/
        URL url = this.getClass().getClassLoader().getResource(scanPackage.replaceAll("\\.", "/"));
        File classPath = new File(url.getFile());
        for (File file : classPath.listFiles()) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                String className = scanPackage + "." + file.getName().replace(".class", "");
                this.classNames.add(className);
            }
        }
    }

    /**
     * 加载配置文件
     *
     * @param contextConfigLocation
     */
    private void doLoadConfig(String contextConfigLocation) {
//        直接从类路径下找到Spring主配置文件所在的路径
//        并且将其读取进来放到Properties对象中
//        相当于scanPackage=com.zc.demo 从文件中保存到了内存中
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (resourceAsStream != null) {
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
