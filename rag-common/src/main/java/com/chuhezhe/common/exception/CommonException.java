package com.chuhezhe.common.exception;

import com.chuhezhe.common.constants.ErrorConstants;
import com.chuhezhe.common.util.SpringContextHolder;
import lombok.Getter;
import org.springframework.context.MessageSource;

import java.util.Locale;

@Getter
public class CommonException extends RuntimeException {

    private final ErrorConstants errorConstants;
    private final Object[] args;


    public CommonException(Exception e){
        super(e.getMessage(), e);
        this.errorConstants = ErrorConstants.INTERNAL_ERROR;
        this.args = null;
    }

    public CommonException(ErrorConstants errorConstants, Throwable cause) {
        super(cause.getMessage(), cause);
        this.errorConstants = errorConstants;
        this.args = null;
    }

    public CommonException(ErrorConstants errorConstants) {
        super(getChineseMessage(errorConstants));
        this.errorConstants = errorConstants;
        this.args = null;
    }

    public CommonException(ErrorConstants errorConstants, String message) {
        super(message);
        this.errorConstants = errorConstants;
        this.args = null;
    }

    public CommonException(ErrorConstants errorConstants, Object... args) {
        super(errorConstants.getCode());
        this.errorConstants = errorConstants;
        this.args = args;
    }

    private static String getChineseMessage(ErrorConstants errorConstants) {
        try {
            MessageSource messageSource = SpringContextHolder.getBean(MessageSource.class);
            return messageSource.getMessage("error." + errorConstants.getCode(), null,
                    errorConstants.getCode(), Locale.SIMPLIFIED_CHINESE);
        } catch (Exception e) {
            return errorConstants.getCode();
        }
    }
}
