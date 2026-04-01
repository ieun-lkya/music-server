package com.music.config;

import com.music.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 拦截器 - "检票员"
 * 负责验证每个请求的 JWT 令牌是否有效
 */
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行跨域预检请求（OPTIONS 请求）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        
        // 从请求头获取 Authorization 字段
        String token = request.getHeader("Authorization");
        
        // 校验令牌格式并验证有效性
        if (token != null && token.startsWith("Bearer ")) {
            // 去掉 "Bearer " 前缀，提取真实 Token
            token = token.substring(7);
            
            // 验证 Token 是否有效
            if (JwtUtils.validateToken(token)) {
                // 令牌有效，放行请求！
                return true;
            }
        }
        
        // 令牌无效或未登录，直接返回 401 状态码！
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }
}
