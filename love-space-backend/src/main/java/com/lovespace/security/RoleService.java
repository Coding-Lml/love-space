package com.lovespace.security;

import com.lovespace.util.UserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("roleService")
public class RoleService {

    @Value("${couple.user1.username}")
    private String ownerUsername1;

    @Value("${couple.user2.username}")
    private String ownerUsername2;

    public boolean isOwner() {
        String username = UserContext.getCurrentUsername();
        if (username == null || username.isBlank()) {
            return false;
        }
        return username.equals(ownerUsername1) || username.equals(ownerUsername2);
    }
}

