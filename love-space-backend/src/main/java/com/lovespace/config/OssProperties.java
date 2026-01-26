package com.lovespace.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oss")
public class OssProperties {
    private boolean enabled;
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucket;
    private String domain;
    private String basePath;
}

