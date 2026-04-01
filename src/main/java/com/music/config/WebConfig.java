package com.music.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtInterceptor())
                .addPathPatterns("/**") // 拦截一切
                //  极其关键的终极白名单（千万不要漏掉任何一个斜杠）
                .excludePathPatterns(
                        "/user/login",      // 放行用户登录
                        "/user/register",   // 放行用户注册
                        "/admin/login",     // 放行管理员登录
                        "/music/list",      // 放行获取歌曲列表
                        "/music/page",      // 放行分页列表
                        "/music/ai/recommend", // 放行 AI 推荐
                        "/common/upload",   // 放行文件上传
                        "/error",           // 放行 Spring 内置错误映射
                        "/", "/index.html", "/favicon.ico", "/assets/**", "/login", "/admin" // 静态资源兜底
                );
    }
}
