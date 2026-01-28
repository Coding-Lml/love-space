package com.lovespace.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lovespace.common.Result;
import com.lovespace.dto.DashboardData;
import com.lovespace.dto.GuestDashboardResponse;
import com.lovespace.entity.Comment;
import com.lovespace.entity.Moment;
import com.lovespace.entity.MomentMedia;
import com.lovespace.entity.User;
import com.lovespace.mapper.UserMapper;
import com.lovespace.service.DashboardService;
import com.lovespace.service.FileService;
import com.lovespace.service.HostSpaceService;
import com.lovespace.service.MomentService;
import com.lovespace.service.SpaceService;
import com.lovespace.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/guest")
@RequiredArgsConstructor
public class GuestController {

    private final HostSpaceService hostSpaceService;
    private final DashboardService dashboardService;
    private final MomentService momentService;
    private final FileService fileService;
    private final UserMapper userMapper;
    private final SpaceService spaceService;

    @GetMapping("/dashboard")
    public Result<GuestDashboardResponse> getDashboard() {
        Long viewerUserId = UserContext.getCurrentUserId();
        Long hostUserId = hostSpaceService.getHostUserId();
        if (hostUserId == null) {
            return Result.error("主人未配置或不存在");
        }

        DashboardData dashboard = dashboardService.getHostDashboardData(hostUserId, viewerUserId);
        User user1 = userMapper.selectById(hostUserId);
        if (user1 != null) {
            user1.setPassword(null);
        }

        User user2 = null;
        Long partnerId = spaceService.getPartnerUserIdInPrimarySpace(hostUserId);
        if (partnerId != null) {
            user2 = userMapper.selectById(partnerId);
            if (user2 != null) {
                user2.setPassword(null);
            }
        }

        GuestDashboardResponse.Couple couple = new GuestDashboardResponse.Couple();
        couple.setUser1(user1);
        couple.setUser2(user2);

        GuestDashboardResponse response = new GuestDashboardResponse();
        response.setDashboard(dashboard);
        response.setCouple(couple);
        return Result.success(response);
    }

    @GetMapping("/moments")
    public Result<Page<Moment>> getMoments(@RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize) {
        Long viewerUserId = UserContext.getCurrentUserId();
        Long hostSpaceId = hostSpaceService.getHostSpaceId();
        if (hostSpaceId == null) {
            return Result.error("主人空间不存在");
        }
        return momentService.getGuestWall(hostSpaceId, viewerUserId, pageNum, pageSize);
    }

    @PostMapping("/moments")
    public Result<Moment> publish(@RequestParam(required = false) String content,
                                  @RequestParam(required = false) String location,
                                  @RequestParam(required = false) MultipartFile[] files) {
        Long guestUserId = UserContext.getCurrentUserId();
        Long hostSpaceId = hostSpaceService.getHostSpaceId();
        if (hostSpaceId == null) {
            return Result.error("主人空间不存在");
        }

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

        return momentService.publishGuestMoment(guestUserId, hostSpaceId, content, location, mediaList);
    }

    @DeleteMapping("/moments/{id}")
    public Result<Void> deleteMoment(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        Long hostSpaceId = hostSpaceService.getHostSpaceId();
        if (hostSpaceId == null) {
            return Result.error("主人空间不存在");
        }
        return momentService.deleteGuestMoment(id, userId, hostSpaceId);
    }

    @PostMapping("/moments/{id}/comments")
    public Result<Comment> addComment(@PathVariable Long id, @RequestBody Map<String, String> params) {
        Long userId = UserContext.getCurrentUserId();
        String content = params.get("content");
        String replyToCommentIdStr = params.get("replyToCommentId");
        Long replyToCommentId = null;
        if (replyToCommentIdStr != null && !replyToCommentIdStr.isBlank()) {
            try {
                replyToCommentId = Long.parseLong(replyToCommentIdStr);
            } catch (NumberFormatException ignored) {
            }
        }
        return momentService.addComment(id, userId, content, replyToCommentId);
    }

    @DeleteMapping("/moments/comments/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        Long userId = UserContext.getCurrentUserId();
        return momentService.deleteComment(commentId, userId);
    }
}
