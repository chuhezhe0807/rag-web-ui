package com.chuhezhe.ragcommonservice.interceptor;

import com.chuhezhe.common.constants.ErrorConstants;
import com.chuhezhe.common.entity.Result;
import com.chuhezhe.common.exception.CommonException;
import com.chuhezhe.common.util.MessageUtil;
import com.chuhezhe.common.util.SpringContextHolder;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * 通用全局异常处理器，供所有微服务使用
 */
@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalExceptionHandler {

    protected final ObjectMapper objectMapper;

    @ExceptionHandler(Exception.class)
    public void handlerException(HttpServletRequest request, HttpServletResponse response, Exception ex) throws IOException {
        String serviceName = getServiceName();
        log.error("{}服务异常 - 访问地址:{} 出错 \n 携带参数:{} \n 报错信息:", serviceName, request.getRequestURI(), JSONObject.toJSONString(request.getParameterMap()), ex);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> error = Result.error(ErrorConstants.INTERNAL_ERROR);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    @ExceptionHandler(CommonException.class)
    public void handlerCommonException(HttpServletRequest request, HttpServletResponse response, CommonException ex) throws IOException {
        String serviceName = getServiceName();
        log.error("{}服务业务异常 - 访问地址:{} 出错 \n 携带参数:{} \n 报错信息:", serviceName, request.getRequestURI(), JSONObject.toJSONString(request.getParameterMap()), ex);
        response.setStatus(ex.getErrorConstants().getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        if (ex.getArgs() != null) {
            Result<Object> error = Result.error(ex.getErrorConstants());
            // 使用带参数的消息覆盖（保持结构一致）
            error.setMessage(MessageUtil.getMessage("error." + ex.getErrorConstants().getCode(), ex.getArgs()));
            response.getWriter().write(objectMapper.writeValueAsString(error));
            return;
        }
        Result<Object> error = Result.error(ex.getErrorConstants());
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public void handlerMissingRequestHeaderException(HttpServletRequest request, HttpServletResponse response, MissingRequestHeaderException ex) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json;charset=UTF-8");

        // 提取具体的请求头名称
        String headerName = ex.getHeaderName();
        Result<Object> error = Result.error(ErrorConstants.MISSING_REQUEST_HEADER);
        // 使用国际化消息，传入请求头名称作为参数
        error.setMessage(MessageUtil.getMessage("error." + ErrorConstants.MISSING_REQUEST_HEADER.getCode(), headerName));
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    @ExceptionHandler(ServletException.class)
    public void handlerServletException(HttpServletRequest request, HttpServletResponse response, ServletException ex) throws IOException {
        String serviceName = getServiceName();
        log.error("{}服务Servlet异常 - 访问地址:{} 出错 \n 携带参数:{} \n 报错信息:", serviceName, request.getRequestURI(), JSONObject.toJSONString(request.getParameterMap()), ex);
        // 检查是否包装了 CommonException
        Throwable rootCause = ex.getRootCause();
        if (rootCause instanceof CommonException) {
            handlerCommonException(request, response, (CommonException) rootCause);
        } else {
            handlerException(request, response, ex);
        }
    }

    /**
     * Bean 参数校验失败（@NotNull、@Valid、@Validated 等）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public void handleValidationException(HttpServletResponse response, MethodArgumentNotValidException ex) throws IOException {
        response.setStatus(400);
        response.setContentType("application/json;charset=UTF-8");

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        Result<Object> error = Result.error(ErrorConstants.PARAMETER_ERROR, errorMessage);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public void handleConstraintViolationException(HttpServletResponse response, ConstraintViolationException ex) throws IOException {
        response.setStatus(400);
        response.setContentType("application/json;charset=UTF-8");

        String errorMessage = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        Result<Object> error = Result.error(ErrorConstants.PARAMETER_ERROR, errorMessage);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    /**
     * RuntimeException 异常
     */
    @ExceptionHandler(RuntimeException.class)
    public void handlerRuntimeException(HttpServletRequest request, HttpServletResponse response, RuntimeException ex) throws IOException {
        String serviceName = getServiceName();
        log.error("{}服务运行时异常 - 访问地址:{} 出错 \n 携带参数:{} \n 报错信息:", serviceName, request.getRequestURI(), JSONObject.toJSONString(request.getParameterMap()), ex);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> error = Result.error(ErrorConstants.INTERNAL_ERROR);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }


    @ExceptionHandler(NoResourceFoundException.class)
    public void handlerNoResourceFoundException(HttpServletRequest request, HttpServletResponse response, NoResourceFoundException ex) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> error = Result.error(ErrorConstants.NOT_FOUND_API);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public void handlerMethodNotSupportedException(HttpServletRequest request, HttpServletResponse response, HttpRequestMethodNotSupportedException ex) throws IOException {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> error = Result.error(ErrorConstants.METHOD_NOT_ALLOWED);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    @ExceptionHandler(FeignException.class)
    public void handlerFeignException(HttpServletRequest request, HttpServletResponse response, FeignException ex) throws IOException {
        // 使用原始异常的状态码
        response.setStatus(ex.status());
        response.setContentType("application/json;charset=UTF-8");

        // 尝试解析原始响应内容
        if (ex.contentUTF8() != null && !ex.contentUTF8().isEmpty()) {
            try {
                // 解析原始响应内容，提取code
                String content = ex.contentUTF8();
                // 简单解析JSON获取code值
                if (content.contains("\"code\":14006")) {
                    Result<Object> error;
                    if (content.contains("\"msg\":\"请先登录\"")) {
                        // 未登录错误
                        error = Result.error(ErrorConstants.NOT_LOGGED_IN);
                    } else {
                        // 未登录错误
                        error = Result.error(ErrorConstants.REMOTE_LOGIN);
                    }
                    response.getWriter().write(objectMapper.writeValueAsString(error));
                } else {
                    // 其他情况直接返回原始内容
                    response.getWriter().write(content);
                }
                return;
            } catch (Exception ignored) {
            }
        }
        // 默认处理
        Result<Object> error = Result.error(ErrorConstants.INTERNAL_ERROR);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    /**
     * 获取服务名称
     */
    private String getServiceName() {
        try {
            // 从环境变量或配置中获取服务名称
            String serviceName = SpringContextHolder.getConfigProperty("spring.application.name", String.class, "未知服务");
            if (serviceName.isEmpty()) {
                serviceName = "未知服务";
            }
            return serviceName;
        } catch (Exception e) {
            return "未知服务";
        }
    }
}
