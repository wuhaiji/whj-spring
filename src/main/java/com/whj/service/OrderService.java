package com.whj.service;

import com.whj.spring.Component;
import com.whj.spring.Scope;
import com.whj.spring.ScopeEnum;


@Scope(ScopeEnum.SINGLETON)
@Component
public class OrderService {

    public String hello() {
        return "hello world";
    }
}
