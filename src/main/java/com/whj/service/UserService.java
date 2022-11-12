package com.whj.service;

import com.whj.spring.*;


@Scope(ScopeEnum.SINGLETON)
@Component
public class UserService implements BeanNameAware {

    @Autowired
    OrderService orderService;

    String beanName;

    String xxx;

    public void init() {

    }

    private void sayHello() {
        System.out.println(orderService.hello());
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }
}
