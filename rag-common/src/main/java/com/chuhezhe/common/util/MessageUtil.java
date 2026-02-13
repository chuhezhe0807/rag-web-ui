package com.chuhezhe.common.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * 国际化消息工具
 */
public final class MessageUtil {

    private MessageUtil() {}

    public static String getMessage(String key, Object... args) {
        MessageSource messageSource = SpringContextHolder.getBean(MessageSource.class);
        return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
    }
}


