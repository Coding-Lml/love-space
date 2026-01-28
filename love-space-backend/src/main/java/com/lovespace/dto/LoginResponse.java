package com.lovespace.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lovespace.entity.User;
import lombok.Data;

@Data
public class LoginResponse {
    
    private String token;
    private User user;
    @JsonProperty("isOwner")
    private boolean owner;
    
    public LoginResponse(String token, User user) {
        this(token, user, false);
    }

    public LoginResponse(String token, User user, boolean isOwner) {
        this.token = token;
        this.user = user;
        this.owner = isOwner;
        // 隐藏密码
        this.user.setPassword(null);
    }
}
