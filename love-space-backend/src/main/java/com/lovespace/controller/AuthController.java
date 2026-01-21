package com.lovespace.controller;

import com.lovespace.common.Result;
import com.lovespace.dto.LoginRequest;
import com.lovespace.dto.LoginResponse;
import com.lovespace.entity.User;
import com.lovespace.service.UserService;
import com.lovespace.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    
    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<User> getCurrentUser() {
        Long userId = UserContext.getCurrentUserId();
        return userService.getUserInfo(userId);
    }
    
    /**
     * 获取另一半信息
     */
    @GetMapping("/partner")
    public Result<User> getPartner() {
        Long userId = UserContext.getCurrentUserId();
        return userService.getPartner(userId);
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/me")
    public Result<User> updateCurrentUser(@RequestBody User user) {
        Long userId = UserContext.getCurrentUserId();
        return userService.updateUserInfo(userId, user);
    }
    
    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody Map<String, String> params) {
        Long userId = UserContext.getCurrentUserId();
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        return userService.changePassword(userId, oldPassword, newPassword);
    }
}
