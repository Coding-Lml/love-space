package com.lovespace.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lovespace.common.Result;
import com.lovespace.entity.Comment;
import com.lovespace.entity.Moment;
import com.lovespace.entity.MomentMedia;
import com.lovespace.service.FileService;
import com.lovespace.service.MomentService;
import com.lovespace.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/moments")
@RequiredArgsConstructor
public class MomentController {
    
    private final MomentService momentService;
    private final FileService fileService;
    
    /**
     * 发布动态
     */
    @PostMapping
    public Result<Moment> publish(
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "SPACE") String visibility,
            @RequestParam(required = false) MultipartFile[] files) {
        
        Long userId = UserContext.getCurrentUserId();
        
        // 处理上传的文件
        List<MomentMedia> mediaList = new ArrayList<>();
        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    Result<String> uploadResult = fileService.uploadFile(file);
                    if (uploadResult.getCode() == 200) {
                        MomentMedia media = new MomentMedia();
                        String url = uploadResult.getData();
                        String type = fileService.getFileType(file.getContentType());
                        media.setUrl(url);
                        media.setType(type);
                        if ("image".equals(type)) {
                            media.setThumbnail(fileService.buildThumbnailUrl(url));
                        }
                        mediaList.add(media);
                    }
                }
            }
        }
        
        return momentService.publish(userId, content, location, visibility, mediaList);
    }
    
    /**
     * 获取动态列表
     */
    @GetMapping
    public Result<Page<Moment>> getList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = UserContext.getCurrentUserId();
        return momentService.getList(userId, pageNum, pageSize);
    }

    @GetMapping("/public")
    public Result<Page<Moment>> getPublicList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = UserContext.getCurrentUserId();
        return momentService.getPublicList(userId, pageNum, pageSize);
    }
    
    /**
     * 获取动态详情
     */
    @GetMapping("/{id}")
    public Result<Moment> getDetail(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        return momentService.getDetail(id, userId);
    }
    
    /**
     * 删除动态
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        return momentService.delete(id, userId);
    }
    
    /**
     * 点赞/取消点赞
     */
    @PostMapping("/{id}/like")
    public Result<Boolean> toggleLike(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        return momentService.toggleLike(id, userId);
    }
    
    /**
     * 添加评论
     */
    @PostMapping("/{id}/comments")
    public Result<Comment> addComment(@PathVariable Long id, @RequestBody Map<String, String> params) {
        Long userId = UserContext.getCurrentUserId();
        String content = params.get("content");
        return momentService.addComment(id, userId, content);
    }
    
    /**
     * 删除评论
     */
    @DeleteMapping("/comments/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        Long userId = UserContext.getCurrentUserId();
        return momentService.deleteComment(commentId, userId);
    }
}
