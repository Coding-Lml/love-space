package com.lovespace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Value("${upload.path}")
    private String uploadPath;
    
    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    /**
     * 静态资源映射（上传的文件）
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 确保路径以文件分隔符结尾，并且适用于不同操作系统
        String normalizedUploadPath = uploadPath.endsWith(File.separator) ? uploadPath : uploadPath + File.separator;
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + normalizedUploadPath);
    }
}
