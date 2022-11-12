package com.whj.spring;

/**
 * 在bean创建完成后 执行初始化
 */
public interface InitializationBean {
    void afterPropertiesSet();
}
