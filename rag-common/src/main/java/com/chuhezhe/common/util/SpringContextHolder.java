package com.chuhezhe.common.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SpringContextHolder {

    private static ApplicationContext context;

    @Autowired
    public void setContext(ApplicationContext context) {
        SpringContextHolder.context = context;
    }

    public static <T> T getConfigProperty(String key, Class<T> targetClass, T defaultValue) {
        return context.getEnvironment().getProperty(key, targetClass, defaultValue);
    }

    public static <T> T getBean(Class<T> requiredType) {
        return context.getBean(requiredType);
    }
}
