package com.lovespace.dto;

import com.lovespace.entity.User;
import lombok.Data;

@Data
public class LoginResponse {
    
    private String token;
    private User user;
    
    public LoginResponse(String token, User user) {
        this.token = token;
        this.user = user;
        // 隐藏密码
        this.user.setPassword(null);
    }
}
