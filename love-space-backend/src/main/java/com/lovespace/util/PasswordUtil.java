package com.lovespace.util;

import cn.hutool.crypto.digest.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtil {
    
    /**
     * 加密密码
     */
    public String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }
    
    /**
     * 验证密码
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }
}
