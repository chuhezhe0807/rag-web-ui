package com.chuhezhe.raggateway.filter;

import com.chuhezhe.common.constants.ErrorConstants;
import com.chuhezhe.common.constants.GConstants;
import com.chuhezhe.common.entity.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关级 JWT 存在性校验：入口处先筛掉没带 token 的请求，再把原始 header 原样
 * 透传给下游；下游 AuthInterceptor 拿到 token 后再做"解析 + 用户是否存在"等
 * 完整校验。分层拦截是为了：
 *  - 网关不依赖 UserServiceClient，不承担业务状态；
 *  - 无效/过期 token 的"最终判决"始终在 user-service 手里，避免出现两个真相源
 *
 * 放行规则：
 *  - 白名单路径（/api/ai/auth/**）直接放行——登录/注册接口不应当要求 JWT
 *  - 其他所有路径必须带 Authorization 或 X-Token 非空 header，否则 401 + Result
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    // 白名单：/api/ai/auth/** 用于注册、登录，token 还没发出来时不能再要求 JWT
    private static final List<String> AUTH_WHITELIST = List.of(
            "/api/ai/auth/**"
    );

    private static final PathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 白名单直接放行
        for (String pattern : AUTH_WHITELIST) {
            if (PATH_MATCHER.match(pattern, path)) {
                return chain.filter(exchange);
            }
        }

        // 2. 取 token：优先 Authorization（下游 AuthInterceptor 读的 header），
        //    兼容历史/前端使用的 X-Token
        String token = firstNonBlank(
                request.getHeaders().getFirst(GConstants.JWT_TOKEN_HEADER),
                request.getHeaders().getFirst("X-Token")
        );

        if (!StringUtils.hasText(token)) {
            log.debug("[JwtAuthGlobalFilter] reject {} without token", path);
            return writeUnauthorized(exchange);
        }

        // 3. 存在性校验通过，header 原样透传给下游；Spring Cloud Gateway 默认
        //    就会 passthrough 原始 header，不需要手动再 setHeader
        return chain.filter(exchange);
    }

    /**
     * 越小的 order 越早执行；放在业务 Filter 之前、但在路由匹配之后通常够用
     */
    @Override
    public int getOrder() {
        return -100;
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Result<Object> body = Result.error(ErrorConstants.UNAUTHORIZED);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            log.error("[JwtAuthGlobalFilter] serialize Result failed", e);
            bytes = "{\"code\":401,\"message\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (StringUtils.hasText(v)) {
                return v;
            }
        }
        return null;
    }
}
