package com.chuhezhe.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JWT工具类
 */
public class JWTUtil {

    // 密钥(默认值)
    private static final String SECRET_KEY = "R0SwBuEwXe6IjGUoUaLiBftw2ATZVGYQ";

    // token有效期(默认值 单位：分钟)
    private static final long TOKEN_EXPIRATION_MINUTES = 10080;

    /**
     * 生成JWT token
     * @param claims 载荷信息
     * @return token字符串
     */
    public static String generateToken(Map<String, Object> claims) {
        Long expiration = SpringContextHolder.getConfigProperty("jwt.expiration", Long.class, TOKEN_EXPIRATION_MINUTES);
        TimeUnit timeUnit = SpringContextHolder.getConfigProperty("jwt.timeUnit", TimeUnit.class, TimeUnit.MINUTES);
        long expirationTime = timeUnit.toMillis(expiration);
        String secret = SpringContextHolder.getConfigProperty("jwt.secret", String.class, SECRET_KEY);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject((String) claims.get("userId"))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256) // 签名算法与python服务保持一致，防止其他接口鉴权不通过
                .compact();
    }

    /**
     * 解析token获取载荷
     *
     * @param token token字符串
     * @return Claims
     */
    public static Claims getClaims(String token) {
        String secret = SpringContextHolder.getConfigProperty("jwt.secret", String.class, SECRET_KEY);

        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 校验token是否有效
     *
     * @param token token字符串
     * @return 是否有效
     */
    public static boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
