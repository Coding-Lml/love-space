package com.lovespace.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProdConfigValidator implements CommandLineRunner {

    private final Environment environment;

    @Override
    public void run(String... args) {
        require("spring.datasource.url");
        require("spring.datasource.username");
        require("spring.datasource.password");
        require("jwt.secret");
        require("upload.path");
        require("logging.file.path");

        String jwtSecret = environment.getProperty("jwt.secret");
        if (jwtSecret != null && jwtSecret.getBytes().length < 32) {
            throw new IllegalStateException("配置项 jwt.secret 长度不足（至少 32 字节）");
        }
    }

    private void require(String key) {
        String value = environment.getProperty(key);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("缺少必要配置项：" + key);
        }
    }
}
