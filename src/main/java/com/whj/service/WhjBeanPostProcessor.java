package com.whj.service;

import com.whj.spring.BeanPostProcessor;
import com.whj.spring.Component;

@Component
public class WhjBeanPostProcessor implements BeanPostProcessor {
    @Override
    public void postProcessBeforeInitialization(String beanName, Object bean) {
        if(beanName.equals("userService")){
            System.out.println("WhjBeanPostProcessor::postProcessBeforeInitialization 发现 userService");
        }
    }

    @Override
    public void postProcessAfterInitialization(String beanName, Object bean) {
        if(beanName.equals("userService")){
            System.out.println("WhjBeanPostProcessor::postProcessAfterInitialization 发现 userService");
        }
    }
}
