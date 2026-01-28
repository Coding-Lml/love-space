package com.lovespace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lovespace.common.Result;
import com.lovespace.dto.LoginRequest;
import com.lovespace.dto.LoginResponse;
import com.lovespace.dto.RegisterRequest;
import com.lovespace.entity.User;
import com.lovespace.mapper.UserMapper;
import com.lovespace.util.JwtUtil;
import com.lovespace.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {
    
    private final JwtUtil jwtUtil;
    private final PasswordUtil passwordUtil;
    private final SpaceService spaceService;

    @Value("${couple.user1.username:}")
    private String ownerUsername1;

    @Value("${couple.user2.username:}")
    private String ownerUsername2;
    
    /**
     * 用户登录
     */
    public Result<LoginResponse> login(LoginRequest request) {
        // 查找用户
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        // 验证密码
        if (!passwordUtil.matches(request.getPassword(), user.getPassword())) {
            return Result.error("密码错误");
        }
        
        // 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        return Result.success("登录成功", new LoginResponse(token, user, isOwnerUsername(user.getUsername())));
    }

    public Result<LoginResponse> register(RegisterRequest request) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String password = request.getPassword();
        String nickname = request.getNickname() == null ? "" : request.getNickname().trim();

        User exist = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
        if (exist != null) {
            return Result.error("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordUtil.encode(password));
        user.setNickname(nickname.isBlank() ? username : nickname);
        user.setAvatar("/uploads/images/default-avatar-boy.png");
        this.save(user);

        spaceService.getOrCreatePrimarySpaceId(user.getId());

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        user.setPassword(null);
        return Result.success("注册成功", new LoginResponse(token, user, isOwnerUsername(user.getUsername())));
    }
    
    /**
     * 获取用户信息
     */
    public Result<User> getUserInfo(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }
    
    /**
     * 更新用户信息
     */
    public Result<User> updateUserInfo(Long userId, User updateUser) {
        User user = this.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        // 只允许更新昵称和头像
        if (updateUser.getNickname() != null) {
            user.setNickname(updateUser.getNickname());
        }
        if (updateUser.getAvatar() != null) {
            user.setAvatar(updateUser.getAvatar());
        }
        
        this.updateById(user);
        user.setPassword(null);
        return Result.success("更新成功", user);
    }
    
    /**
     * 修改密码
     */
    public Result<Void> changePassword(Long userId, String oldPassword, String newPassword) {
        User user = this.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        if (!passwordUtil.matches(oldPassword, user.getPassword())) {
            return Result.error("原密码错误");
        }
        
        user.setPassword(passwordUtil.encode(newPassword));
        this.updateById(user);
        
        return Result.success("密码修改成功", null);
    }
    
    /**
     * 获取另一半信息
     */
    public Result<User> getPartner(Long currentUserId) {
        Long partnerId = spaceService.getPartnerUserIdInPrimarySpace(currentUserId);
        User partner = partnerId == null ? null : this.getById(partnerId);
        if (partner != null) {
            partner.setPassword(null);
        }
        return Result.success(partner);
    }

    private boolean isOwnerUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        String normalized = username.trim();
        return normalized.equals(ownerUsername1) || normalized.equals(ownerUsername2);
    }
}
