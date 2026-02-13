package com.chuhezhe.common.entity;

import com.chuhezhe.common.constants.ErrorConstants;
import com.chuhezhe.common.util.MessageUtil;
import lombok.Data;
import org.springframework.http.HttpStatus;

/**
 * 通用返回结果
 */
@Data
public class Result<T> {
    private int code;
    private String codeDesc;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setCode(HttpStatus.OK.value());
        result.setCodeDesc(HttpStatus.OK.getReasonPhrase());
        result.setMessage(MessageUtil.getMessage("common.success"));
        result.setData(data);
        return result;
    }

    public static <T> Result<T> ok() {
        Result<T> result = new Result<>();
        result.setCode(HttpStatus.OK.value());
        result.setCodeDesc(HttpStatus.OK.getReasonPhrase());
        result.setMessage(MessageUtil.getMessage("common.success"));
        result.setData(null);
        return result;
    }

    public static <T> Result<T> error(ErrorConstants errorConstants) {
        Result<T> result = new Result<>();
        result.setCode(errorConstants.getHttpStatus().value());
        result.setCodeDesc(errorConstants.getCode());
        result.setMessage(MessageUtil.getMessage("error." + errorConstants.getCode()));
        return result;
    }

    // 带自定义消息的错误封装
    public static <T> Result<T> error(ErrorConstants errorConstants, String customMessage) {
        Result<T> result = new Result<>();
        result.setCode(errorConstants.getHttpStatus().value());
        result.setCodeDesc(errorConstants.getCode());
        result.setMessage(customMessage);
        return result;
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(null);
        return result;
    }

    public static <T> Result<T> error() {
        Result<T> result = new Result<>();
        result.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return result;
    }
}
