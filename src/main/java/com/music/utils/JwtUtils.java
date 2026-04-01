package com.music.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类 - "造票机"
 * 用于生成和验证 JWT 令牌
 */
public class JwtUtils {
    // 极其机密的盐值
    private static final String SECRET = "EchoScene_Super_Secret_Key_888";
    // 过期时间：7 天
    private static final long EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000;

    /**
     * 生成 JWT Token
     * @param userId 用户 ID
     * @param username 用户名
     * @return JWT 令牌字符串
     */
    public static String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        
        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .compact();
    }

    /**
     * 验证 JWT Token 是否有效
     * @param token JWT 令牌
     * @return true-有效，false-无效
     */
    public static boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
