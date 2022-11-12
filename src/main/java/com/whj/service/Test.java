package com.whj.service;

import com.whj.spring.WhjApplicationContext;

import java.util.Optional;

public class Test {
    public static void main(String[] args) {
        WhjApplicationContext context = new WhjApplicationContext(AppConfig.class);
        UserService userService = context
                .<UserService>getBean("userService")
                .orElseThrow(() -> new RuntimeException("不存在该bean"));
        System.out.println(userService.beanName);

    }
}
