package com.lovespace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Value("${upload.path}")
    private String uploadPath;
    
    @Value("${cors.allowed-origin-patterns:}")
    private String corsAllowedOriginPatterns;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String corsAllowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String corsAllowedHeaders;

    @Value("${cors.allow-credentials:false}")
    private boolean corsAllowCredentials;

    @Value("${cors.max-age:3600}")
    private long corsMaxAge;

    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (!StringUtils.hasText(corsAllowedOriginPatterns)) {
            return;
        }

        List<String> originPatterns = splitAndTrim(corsAllowedOriginPatterns);
        if (originPatterns.isEmpty()) {
            return;
        }

        List<String> methods = splitAndTrim(corsAllowedMethods);
        List<String> headers = splitAndTrim(corsAllowedHeaders);

        registry.addMapping("/**")
                .allowedOriginPatterns(originPatterns.toArray(new String[0]))
                .allowedMethods(methods.isEmpty() ? new String[0] : methods.toArray(new String[0]))
                .allowedHeaders(headers.isEmpty() ? new String[0] : headers.toArray(new String[0]))
                .allowCredentials(corsAllowCredentials)
                .maxAge(corsMaxAge);
    }
    
    /**
     * 静态资源映射（上传的文件）
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 使用绝对路径，确保在各种环境下都能正确映射
        String absoluteUploadPath = Paths.get(uploadPath).toAbsolutePath().toString();
        // 确保路径以文件分隔符结尾
        if (!absoluteUploadPath.endsWith(File.separator)) {
            absoluteUploadPath += File.separator;
        }
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absoluteUploadPath);
    }

    private List<String> splitAndTrim(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }
}
