package com.lovespace.controller;

import com.lovespace.common.Result;
import com.lovespace.entity.User;
import com.lovespace.service.FileService;
import com.lovespace.service.UserService;
import com.lovespace.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FileService fileService;

    /**
     * 上传并设置头像
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        // 1. 上传文件
        Result<String> uploadResult = fileService.uploadFile(file);
        if (uploadResult.getCode() != 200) {
            return Result.error(uploadResult.getMessage());
        }
        String avatarUrl = uploadResult.getData();

        // 2. 更新用户信息
        Long userId = UserContext.getCurrentUserId();
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setAvatar(avatarUrl);
        
        Result<User> updateResult = userService.updateUserInfo(userId, updateUser);
        if (updateResult.getCode() != 200) {
            return Result.error(updateResult.getMessage());
        }

        return Result.success("头像设置成功", avatarUrl);
    }
}
