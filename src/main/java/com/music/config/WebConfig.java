package com.music.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 统一配置 - "安检大门"
 * 统一管理 CORS 跨域和 JWT 拦截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置跨域访问规则
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 注册 JWT 拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtInterceptor())
                .addPathPatterns("/**") // 拦截所有请求
                // 白名单：不需要登录即可访问的接口
                .excludePathPatterns(
                    "/user/login",      // 登录接口
                    "/user/register",   // 注册接口
                    "/music/list",      // 公开音乐列表
                    "/music/ai/recommend", // AI 推荐接口
                    "/common/upload",    // 文件上传接口
                    "/error"             // 错误页面接口
                );
    }
}
