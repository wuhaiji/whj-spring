package com.whj.spring;

public interface BeanPostProcessor {

    void postProcessBeforeInitialization(String beanName, Object bean);

    void postProcessAfterInitialization(String beanName, Object bean);
}
