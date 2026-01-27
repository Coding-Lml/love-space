package com.lovespace.controller;

import com.lovespace.common.Result;
import com.lovespace.dto.LoginRequest;
import com.lovespace.dto.LoginResponse;
import com.lovespace.dto.RegisterRequest;
import com.lovespace.entity.User;
import com.lovespace.security.LoginRateLimiter;
import com.lovespace.service.UserService;
import com.lovespace.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    private final LoginRateLimiter loginRateLimiter;
    
    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(HttpServletRequest httpServletRequest, @Valid @RequestBody LoginRequest request) {
        String clientIp = getClientIp(httpServletRequest);
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String key = clientIp + ":" + username;
        if (!loginRateLimiter.tryAcquire(key)) {
            return Result.error(429, "登录过于频繁，请稍后再试");
        }
        return userService.login(request);
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(HttpServletRequest httpServletRequest, @Valid @RequestBody RegisterRequest request) {
        String clientIp = getClientIp(httpServletRequest);
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String key = "register:" + clientIp + ":" + username;
        if (!loginRateLimiter.tryAcquire(key)) {
            return Result.error(429, "注册过于频繁，请稍后再试");
        }
        return userService.register(request);
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

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int commaIndex = xff.indexOf(',');
            return (commaIndex > 0 ? xff.substring(0, commaIndex) : xff).trim();
        }
        return request.getRemoteAddr();
    }
}
