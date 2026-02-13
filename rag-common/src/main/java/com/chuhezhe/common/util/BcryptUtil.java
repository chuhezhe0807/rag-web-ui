package com.chuhezhe.common.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * 密码加密/验证工具类（与Python bcrypt完全兼容）
 * 核心：保持盐的复杂度（log_rounds=10，Python bcrypt.gensalt()默认值）、UTF-8编码
 */
public class BcryptUtil {
    /**
     * 验证明文密码与加密密码是否匹配（对应Python的verify_password）
     * @param plainPassword 明文密码
     * @param hashedPassword 加密后的密码（Python生成的字符串）
     * @return 是否匹配
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        // jbcrypt内置处理UTF-8编码，无需手动encode/decode
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }

    /**
     * 生成密码的加密哈希值（对应Python的get_password_hash）
     * @param password 明文密码
     * @return 加密后的字符串（与Python生成的格式、内容完全一致）
     */
    public static String getPasswordHash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        // 生成盐
        String salt = BCrypt.gensalt();
        // 加密并返回字符串（jbcrypt自动处理编码）
        return BCrypt.hashpw(password, salt);
    }
}