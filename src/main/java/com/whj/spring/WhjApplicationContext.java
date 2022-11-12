package com.whj.spring;

import io.vavr.collection.List;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.whj.spring.ScopeEnum.SINGLETON;

@Slf4j
public class WhjApplicationContext {

    private final Class<?> configClass;

    private final ConcurrentHashMap<String, BeanDefinition<?>> beanDefinitionMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Optional<?>> singletonObjects = new ConcurrentHashMap<>();
    private final java.util.List<BeanPostProcessor> beanPostProcessors = Collections.synchronizedList(new ArrayList<>());
    // 缓存class字段，提高性能
    private final ConcurrentHashMap<Class<?>, Field[]> classFields = new ConcurrentHashMap<>();

    public WhjApplicationContext(Class<?> configClass) {
        this.configClass = configClass;
        // 首先扫描bean
        Optional.of(configClass)
                .map(v -> v.getAnnotation(ComponentScan.class))
                .map(ComponentScan::value)
                .map(this::paths2BeanDefinitionOptions)
                .ifPresent(optionals ->
                        optionals.forEach(beanDefinitionOpt ->
                                beanDefinitionOpt.ifPresent(beanDefinition ->
                                        beanDefinitionMap.put(beanDefinition.getBeanName(), beanDefinition))));


        //实例化单例bean
        beanDefinitionMap.forEach((beanName, beanDefinition) -> {
            if (beanDefinition.getScope() == SINGLETON) {
                Optional<?> bean = this.createBean(beanDefinition);
                singletonObjects.put(beanName, bean);
            }
        });
    }


    private <T> List<Optional<BeanDefinition<T>>> paths2BeanDefinitionOptions(String[] paths) {
        List<Optional<BeanDefinition<T>>> optionals = Optional.ofNullable(paths)
                .map(List::of)
                .orElse(List.empty())
                .filter(Objects::nonNull)
                .map(this::replacePath)
                .flatMap(this::findBeanDefinitionInPath);
        return optionals;
    }

    private <T> List<Optional<BeanDefinition<T>>> findBeanDefinitionInPath(String path) {
        List<Optional<BeanDefinition<T>>> optionals = Optional.ofNullable(path)
                .map(this::getResourceFromPath)
                .map(URL::getFile)
                .map(File::new)
                .filter(File::exists)
                .filter(File::isDirectory)
                .map(file -> this.<T>findBeanDefinitionsInDirectoryPath(file, path))
                .orElse(List.empty());
        return optionals;
    }

    private <T> List<Optional<BeanDefinition<T>>> findBeanDefinitionsInDirectoryPath(File directory, String path) {
        List<Optional<BeanDefinition<T>>> map = Optional.of(directory)
                .map(File::listFiles)
                .map(List::of)
                .orElse(List.empty())
                .filter(this::isClass)
                .map(file -> createBeanDefinition(path, file));
        return map;
    }


    private <T> Optional<BeanDefinition<T>> createBeanDefinition(String path, File file) {
        String name = path + File.separator + getFileNameWithoutSuffix(file);
        name = name.replace(String.valueOf(File.separator), ".");
        log.info("fileName:{}", name);
        Class<T> clazz = this.loadClassByName(name);
        if (clazz.isAnnotationPresent(Component.class)) {
            BeanDefinition<T> tBeanDefinition = this.buildBeanDefinition(clazz);
            if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
                try {
                    BeanPostProcessor t = (BeanPostProcessor) clazz.newInstance();
                    beanPostProcessors.add(t);
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            return Optional.ofNullable(tBeanDefinition);
        }
        return Optional.empty();

    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> loadClassByName(String name) {
        Class<T> beanClass;
        try {
            ClassLoader classLoader = WhjApplicationContext.class.getClassLoader();
            beanClass = (Class<T>) classLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return beanClass;
    }

    private <T> BeanDefinition<T> buildBeanDefinition(Class<T> clz) {
        ScopeEnum scopeEnum = Optional
                .ofNullable(clz.getAnnotation(Scope.class))
                .map(Scope::value)
                .orElse(SINGLETON);
        String beanName = getBeanName(clz);
        return BeanDefinition.<T>builder().scope(scopeEnum).type(clz).beanName(beanName).build();
    }

    private <T> String getBeanName(Class<T> clz) {
        String simpleName = clz.getSimpleName();
        simpleName = toLowerCamel(simpleName);
        String beanName = Optional.ofNullable(clz.getAnnotation(Component.class))
                .map(Component::value)
                .filter(v -> !"".equals(v))
                .orElse(simpleName);
        return beanName;
    }

    private static String toLowerCamel(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
                Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        char[] chars = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    private String getFileNameWithoutSuffix(File file) {
        return file.getName().substring(0, file.getName().lastIndexOf(".class"));
    }

    private boolean isClass(File v) {
        return v.getAbsolutePath().endsWith(".class");
    }

    private String replacePath(String path) {
        return path.replace(".", File.separator);
    }

    private URL getResourceFromPath(String path) {
        return WhjApplicationContext.class.getClassLoader().getResource(path);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getBean(String beanName) {
        BeanDefinition<T> beanDefinition = (BeanDefinition<T>) beanDefinitionMap.get(beanName);

        if (beanDefinition == null) {
            return Optional.empty();
        }

        ScopeEnum scope = beanDefinition.getScope();

        if (scope == SINGLETON) {
            return getBean(beanDefinition);
        } else {
            return createBean(beanDefinition);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> getBean(BeanDefinition<T> beanDefinition) {
        Optional<T> bean = (Optional<T>) singletonObjects.get(beanDefinition.getBeanName());
        if (bean.isPresent()) {
            return bean;
        }
        return this.createBean(beanDefinition);
    }

    private <T> Optional<T> createBean(BeanDefinition<T> beanDefinition) {
        try {
            Class<T> clz = beanDefinition.getType();
            T bean = clz.getConstructor().newInstance();

            // 依赖注入
            Field[] fields = classFields.computeIfAbsent(clz, k -> clz.getDeclaredFields());
            for (Field f : fields) {
                if (f.isAnnotationPresent(Autowired.class)) {
                    f.setAccessible(true);
                    f.set(bean, getBean(f.getName()));
                }

            }
            // 判断对象是不是实现了 ware接口
            if (bean instanceof BeanNameAware) {
                ((BeanNameAware) bean).setBeanName(beanDefinition.getBeanName());
            }

            // 初始化
            if (bean instanceof InitializationBean) {
                ((InitializationBean) bean).afterPropertiesSet();
            }

            // 初始化后 AOP
            for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
                beanPostProcessor.postProcessAfterInitialization(beanDefinition.getBeanName(), bean);
            }

            return Optional.of(bean);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }


}
