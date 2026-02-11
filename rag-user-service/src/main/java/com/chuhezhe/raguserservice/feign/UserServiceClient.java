package com.chuhezhe.raguserservice.feign;

import com.chuhezhe.common.entity.Result;
import com.chuhezhe.raguserservice.vo.request.LoginRequest;
import com.chuhezhe.raguserservice.vo.request.RegisterRequest;
import com.chuhezhe.raguserservice.vo.response.LoginResponse;
import com.chuhezhe.raguserservice.vo.response.RegisterResponse;
import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "rag-ai-service")
public interface UserServiceClient {

    /**
     * 登录
     * fastapi 侧使用的是 form-data 所这里接收和转发都应该使用 Content-Type: application/x-www-form-urlencoded
     */
    @PostMapping(
            value = "/api/ai/auth/token",
            // 声明该方法接收form表单格式的请求体
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    @Headers("Content-Type: application/x-www-form-urlencoded") // 当前方法作为 Feign 客户端发送请求时，要携带的请求头
    Result<LoginResponse> login(@RequestBody LoginRequest password);

    /**
     * 注册
     */
    @PostMapping("/api/ai/auth/register")
    Result<RegisterResponse> register(@RequestBody RegisterRequest registerRequest);
}
