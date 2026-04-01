package com.music.config;

import com.music.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class JwtInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 放行跨域预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true; 
        
        // 2. 获取通行证
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            if (JwtUtils.validateToken(token)) {
                return true; // 令牌有效，顺利放行！
            } else {
                System.out.println(" 拦截器击杀：Token 已失效或被篡改，拦截接口 -> " + request.getRequestURI());
            }
        } else {
            System.out.println(" 拦截器击杀：未携带 Token (或格式错误)，拦截接口 -> " + request.getRequestURI());
        }
        
        // 3. 令牌无效或未登录，直接踢回 401！
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }
}
