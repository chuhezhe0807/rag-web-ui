package com.chuhezhe.ragcommonservice.config;

import com.chuhezhe.ragcommonservice.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**") // 对所有请求都进行拦截
                // /api/ai/auth/** 由 rag-user-service 暴露 register / login 等前置接口，
                // 登录态尚不存在，不能再要求 JWT；AuthInterceptor 内部也会识别 @AnonymousAccess，
                // 这里做 path 级白名单是 belt-and-suspenders，防止以后忘了加注解
                .excludePathPatterns("/api/ai/auth/**");
    }
}
