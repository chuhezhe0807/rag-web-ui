package com.chuhezhe.ragcommonservice.config;

import com.chuhezhe.common.constants.GConstants;
import feign.Logger;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfiguration {

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.HEADERS;
    }

    /**
     * 把入站请求的 JWT header 原样透传到出站 Feign 调用。
     *
     * Why: Java 服务之间通过 Feign 互调时，每一跳都要自己从 header 拿 token 去
     * userServiceClient.getUserInfo；如果不透传，下游只能拿到空 header，
     * AuthInterceptor 会直接判 401，业务链路一掉到底。
     *
     * 规则：
     *  - 优先读 GConstants.JWT_TOKEN_HEADER（"Authorization"），这是
     *    AuthInterceptor 期望的 header 名
     *  - 兼容读一份 "X-Token"（PRD US-009 里提到的另一种写法；前端 / 运维
     *    脚本如果还在用 X-Token 发请求，也应当能走通）
     *  - 如果当前线程没有 ServletRequest（比如定时任务 / MQ 消费里发 Feign），
     *    就什么也不做，交给下游的业务代码自己决定要不要塞 header
     */
    @Bean
    public RequestInterceptor authHeaderRelayRequestInterceptor() {
        return template -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return;
            }
            HttpServletRequest request = attrs.getRequest();

            String auth = request.getHeader(GConstants.JWT_TOKEN_HEADER);
            if (StringUtils.hasText(auth)) {
                template.header(GConstants.JWT_TOKEN_HEADER, auth);
            }

            String xToken = request.getHeader("X-Token");
            if (StringUtils.hasText(xToken)) {
                template.header("X-Token", xToken);
            }
        };
    }
}
