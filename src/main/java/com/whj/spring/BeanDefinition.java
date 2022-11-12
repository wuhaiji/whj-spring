package com.whj.spring;

import lombok.*;

@NoArgsConstructor
@Builder
@Getter
@Setter
@AllArgsConstructor
public class BeanDefinition<T> {
    private Class<T> type;
    private ScopeEnum scope;
    private String beanName;

}
