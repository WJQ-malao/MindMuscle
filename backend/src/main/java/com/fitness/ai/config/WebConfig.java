package com.fitness.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    /** 动作插图上传目录，见 app.exercise-images.dir */
    @org.springframework.beans.factory.annotation.Value("${app.exercise-images.dir:./exercise-images}")
    private String imageDir;

    public WebConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**")
                // 动作插图：<img> 标签无法携带 token，需匿名访问
                .excludePathPatterns("/api/exercise-images/**");
    }

    @Override
    public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
        // 必须转 URI（file:///D:/...）：直接拼 "file:" + Windows 反斜杠路径不是合法 URL，会导致资源解析 404
        String location = java.nio.file.Path.of(imageDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/api/exercise-images/**").addResourceLocations(location);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 开发期放开跨域；生产环境应限定前端域名
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
