package com.lovespace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lovespace.common.Result;
import com.lovespace.entity.*;
import com.lovespace.mapper.*;
import com.lovespace.security.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MomentService extends ServiceImpl<MomentMapper, Moment> {
    
    private final MomentMediaMapper mediaMapper;
    private final CommentMapper commentMapper;
    private final MomentLikeMapper likeMapper;
    private final UserMapper userMapper;
    private final FileService fileService;
    private final SpaceService spaceService;
    private final HostSpaceService hostSpaceService;
    private final RoleService roleService;

    private static final String VISIBILITY_SPACE = "SPACE";
    private static final String VISIBILITY_PUBLIC = "PUBLIC";
    private static final String VISIBILITY_GUEST = "GUEST";
    
    /**
     * 发布动态
     */
    @Transactional
    public Result<Moment> publish(Long userId, String content, String location, String visibility, List<MomentMedia> mediaList) {
        // 创建动态
        Long spaceId = spaceService.getOrCreatePrimarySpaceId(userId);
        Moment moment = new Moment();
        moment.setSpaceId(spaceId);
        moment.setUserId(userId);
        moment.setContent(content);
        moment.setLocation(location);
        moment.setLikes(0);
        moment.setVisibility(normalizeVisibility(visibility));
        this.save(moment);
        
        // 保存媒体文件
        if (mediaList != null && !mediaList.isEmpty()) {
            for (int i = 0; i < mediaList.size(); i++) {
                MomentMedia media = mediaList.get(i);
                media.setMomentId(moment.getId());
                media.setSort(i);
                mediaMapper.insert(media);
            }
        }
        
        return Result.success("发布成功", moment);
    }
    
    /**
     * 获取动态列表（分页）
     */
    public Result<Page<Moment>> getList(Long currentUserId, Integer pageNum, Integer pageSize) {
        Page<Moment> page = new Page<>(pageNum, pageSize);
        Long spaceId = spaceService.getOrCreatePrimarySpaceId(currentUserId);
        
        // 查询动态
        Page<Moment> momentPage = this.page(page, new LambdaQueryWrapper<Moment>()
                .eq(Moment::getSpaceId, spaceId)
                .orderByDesc(Moment::getCreatedAt));
        
        // 填充关联数据
        for (Moment moment : momentPage.getRecords()) {
            fillMomentData(moment, currentUserId);
        }
        
        return Result.success(momentPage);
    }

    public Result<Page<Moment>> getPublicList(Long currentUserId, Integer pageNum, Integer pageSize) {
        Page<Moment> page = new Page<>(pageNum, pageSize);
        Long hostSpaceId = hostSpaceService.getHostSpaceId();
        Page<Moment> momentPage = this.page(page, new LambdaQueryWrapper<Moment>()
                .eq(Moment::getVisibility, VISIBILITY_PUBLIC)
                .eq(hostSpaceId != null, Moment::getSpaceId, hostSpaceId)
                .orderByDesc(Moment::getCreatedAt));

        for (Moment moment : momentPage.getRecords()) {
            fillMomentData(moment, currentUserId);
        }
        return Result.success(momentPage);
    }

    public Result<Page<Moment>> getGuestWall(Long hostSpaceId, Long viewerUserId, Integer pageNum, Integer pageSize) {
        Page<Moment> page = new Page<>(pageNum, pageSize);
        Page<Moment> momentPage = this.page(page, new LambdaQueryWrapper<Moment>()
                .eq(Moment::getSpaceId, hostSpaceId)
                .in(Moment::getVisibility, VISIBILITY_PUBLIC, VISIBILITY_GUEST)
                .orderByDesc(Moment::getCreatedAt));

        for (Moment moment : momentPage.getRecords()) {
            fillMomentData(moment, viewerUserId);
        }
        return Result.success(momentPage);
    }

    @Transactional
    public Result<Moment> publishGuestMoment(Long guestUserId, Long hostSpaceId, String content, String location, List<MomentMedia> mediaList) {
        Moment moment = new Moment();
        moment.setSpaceId(hostSpaceId);
        moment.setUserId(guestUserId);
        moment.setContent(content);
        moment.setLocation(location);
        moment.setLikes(0);
        moment.setVisibility(VISIBILITY_GUEST);
        this.save(moment);

        if (mediaList != null && !mediaList.isEmpty()) {
            for (int i = 0; i < mediaList.size(); i++) {
                MomentMedia media = mediaList.get(i);
                media.setMomentId(moment.getId());
                media.setSort(i);
                mediaMapper.insert(media);
            }
        }
        return Result.success("发布成功", moment);
    }
    
    /**
     * 获取动态详情
     */
    public Result<Moment> getDetail(Long momentId, Long currentUserId) {
        Moment moment = this.getById(momentId);
        if (moment == null) {
            return Result.error("动态不存在");
        }
        Long spaceId = spaceService.getOrCreatePrimarySpaceId(currentUserId);
        if (!canAccessMoment(moment, spaceId)) {
            return Result.error(403, "无权限访问");
        }
        fillMomentData(moment, currentUserId);
        return Result.success(moment);
    }
    
    /**
     * 删除动态
     */
    @Transactional
    public Result<Void> delete(Long momentId, Long userId) {
        Moment moment = this.getById(momentId);
        if (moment == null) {
            return Result.error("动态不存在");
        }
        Long spaceId = spaceService.getOrCreatePrimarySpaceId(userId);
        if (!canAccessMoment(moment, spaceId)) {
            return Result.error(403, "无权限操作");
        }
        if (!roleService.isOwner() && !moment.getUserId().equals(userId)) {
            return Result.error(403, "无权限操作");
        }

        List<MomentMedia> mediaList = mediaMapper.selectList(new LambdaQueryWrapper<MomentMedia>()
                .eq(MomentMedia::getMomentId, momentId));
        for (MomentMedia media : mediaList) {
            if (media.getUrl() != null && !media.getUrl().isBlank()) {
                fileService.deleteFile(media.getUrl());
            }
            if (media.getThumbnail() != null && !media.getThumbnail().isBlank()) {
                fileService.deleteFile(media.getThumbnail());
            }
        }

        mediaMapper.delete(new LambdaQueryWrapper<MomentMedia>().eq(MomentMedia::getMomentId, momentId));
        likeMapper.delete(new LambdaQueryWrapper<MomentLike>().eq(MomentLike::getMomentId, momentId));
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getMomentId, momentId));
        this.removeById(momentId);
        return Result.success("删除成功", null);
    }

    @Transactional
    public Result<Void> deleteGuestMoment(Long momentId, Long userId, Long hostSpaceId) {
        Moment moment = this.getById(momentId);
        if (moment == null) {
            return Result.error("动态不存在");
        }
        if (moment.getSpaceId() == null || !moment.getSpaceId().equals(hostSpaceId)) {
            return Result.error(403, "无权限操作");
        }
        if (!VISIBILITY_GUEST.equals(moment.getVisibility())) {
            return Result.error(403, "无权限操作");
        }
        if (!roleService.isOwner() && !moment.getUserId().equals(userId)) {
            return Result.error(403, "无权限操作");
        }

        List<MomentMedia> mediaList = mediaMapper.selectList(new LambdaQueryWrapper<MomentMedia>()
                .eq(MomentMedia::getMomentId, momentId));
        for (MomentMedia media : mediaList) {
            if (media.getUrl() != null && !media.getUrl().isBlank()) {
                fileService.deleteFile(media.getUrl());
            }
            if (media.getThumbnail() != null && !media.getThumbnail().isBlank()) {
                fileService.deleteFile(media.getThumbnail());
            }
        }

        mediaMapper.delete(new LambdaQueryWrapper<MomentMedia>().eq(MomentMedia::getMomentId, momentId));
        likeMapper.delete(new LambdaQueryWrapper<MomentLike>().eq(MomentLike::getMomentId, momentId));
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getMomentId, momentId));
        this.removeById(momentId);
        return Result.success("删除成功", null);
    }
    
    /**
     * 点赞/取消点赞
     */
    @Transactional
    public Result<Boolean> toggleLike(Long momentId, Long userId) {
        Moment moment = this.getById(momentId);
        if (moment == null) {
            return Result.error("动态不存在");
        }
        Long spaceId = spaceService.getOrCreatePrimarySpaceId(userId);
        if (!canAccessMoment(moment, spaceId)) {
            return Result.error(403, "无权限操作");
        }
        
        // 检查是否已点赞
        MomentLike existLike = likeMapper.selectOne(new LambdaQueryWrapper<MomentLike>()
                .eq(MomentLike::getMomentId, momentId)
                .eq(MomentLike::getUserId, userId));
        
        boolean liked;
        if (existLike != null) {
            // 取消点赞
            likeMapper.deleteById(existLike.getId());
            moment.setLikes(Math.max(0, moment.getLikes() - 1));
            liked = false;
        } else {
            // 点赞
            MomentLike like = new MomentLike();
            like.setMomentId(momentId);
            like.setUserId(userId);
            likeMapper.insert(like);
            moment.setLikes(moment.getLikes() + 1);
            liked = true;
        }
        this.updateById(moment);
        
        return Result.success(liked ? "点赞成功" : "取消点赞", liked);
    }
    
    /**
     * 添加评论/回复评论
     */
    public Result<Comment> addComment(Long momentId, Long userId, String content, Long replyToCommentId) {
        Moment moment = this.getById(momentId);
        if (moment == null) {
            return Result.error("动态不存在");
        }
        Long spaceId = spaceService.getOrCreatePrimarySpaceId(userId);
        if (!canAccessMoment(moment, spaceId)) {
            return Result.error(403, "无权限操作");
        }
        if (content == null || content.isBlank()) {
            return Result.error("评论内容不能为空");
        }
        String trimmed = content.trim();

        LambdaQueryWrapper<Comment> duplicateWrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getMomentId, momentId)
                .eq(Comment::getUserId, userId)
                .eq(Comment::getContent, trimmed)
                .eq(replyToCommentId != null, Comment::getParentId, replyToCommentId)
                .isNull(replyToCommentId == null, Comment::getParentId)
                .orderByDesc(Comment::getCreatedAt)
                .last("LIMIT 1");
        Comment latestSame = commentMapper.selectOne(duplicateWrapper);
        if (latestSame != null && latestSame.getCreatedAt() != null) {
            long seconds = Duration.between(latestSame.getCreatedAt(), java.time.LocalDateTime.now()).getSeconds();
            if (seconds >= 0 && seconds < 3) {
                return Result.error(400, "请勿重复发送");
            }
        }
        
        Comment comment = new Comment();
        comment.setMomentId(momentId);
        comment.setUserId(userId);
        comment.setContent(trimmed);
        if (replyToCommentId != null) {
            Comment parent = commentMapper.selectById(replyToCommentId);
            if (parent == null || !momentId.equals(parent.getMomentId())) {
                return Result.error("回复的评论不存在");
            }
            comment.setParentId(replyToCommentId);
            comment.setReplyToUserId(parent.getUserId());
        }
        commentMapper.insert(comment);
        
        // 填充用户信息
        comment.setUser(userMapper.selectById(userId));
        if (comment.getUser() != null) {
            comment.getUser().setPassword(null);
        }
        if (comment.getReplyToUserId() != null) {
            User replyUser = userMapper.selectById(comment.getReplyToUserId());
            if (replyUser != null) {
                replyUser.setPassword(null);
            }
            comment.setReplyToUser(replyUser);
        }
        
        return Result.success("评论成功", comment);
    }
    
    /**
     * 删除评论
     */
    public Result<Void> deleteComment(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return Result.error("评论不存在");
        }
        Moment moment = this.getById(comment.getMomentId());
        if (moment == null) {
            return Result.error("动态不存在");
        }
        Long spaceId = spaceService.getOrCreatePrimarySpaceId(userId);
        if (!canAccessMoment(moment, spaceId)) {
            return Result.error(403, "无权限操作");
        }
        boolean canDelete = comment.getUserId().equals(userId)
                || moment.getUserId().equals(userId)
                || roleService.isOwner();
        if (!canDelete) {
            return Result.error(403, "无权限操作");
        }
        
        commentMapper.deleteById(commentId);
        return Result.success("删除成功", null);
    }
    
    /**
     * 获取最新动态（首页用）
     */
    public List<Moment> getRecentMoments(Long currentUserId, int limit) {
        Long spaceId = spaceService.getOrCreatePrimarySpaceId(currentUserId);
        List<Moment> moments = this.list(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getSpaceId, spaceId)
                .orderByDesc(Moment::getCreatedAt)
                .last("LIMIT " + limit));
        
        for (Moment moment : moments) {
            fillMomentData(moment, currentUserId);
        }
        return moments;
    }

    public List<Moment> getRecentPublicMoments(Long hostUserId, Long viewerUserId, int limit) {
        Long spaceId = spaceService.getOrCreatePrimarySpaceId(hostUserId);
        List<Moment> moments = this.list(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getSpaceId, spaceId)
                .eq(Moment::getVisibility, VISIBILITY_PUBLIC)
                .orderByDesc(Moment::getCreatedAt)
                .last("LIMIT " + limit));

        for (Moment moment : moments) {
            fillMomentData(moment, viewerUserId);
        }
        return moments;
    }

    private String normalizeVisibility(String visibility) {
        if (visibility == null) {
            return VISIBILITY_SPACE;
        }
        String v = visibility.trim().toUpperCase();
        if (VISIBILITY_PUBLIC.equals(v)) {
            return VISIBILITY_PUBLIC;
        }
        if (VISIBILITY_GUEST.equals(v)) {
            return VISIBILITY_GUEST;
        }
        return VISIBILITY_SPACE;
    }

    private boolean canAccessMoment(Moment moment, Long currentSpaceId) {
        if (moment == null) {
            return false;
        }
        if (VISIBILITY_PUBLIC.equals(moment.getVisibility())) {
            return true;
        }
        if (VISIBILITY_GUEST.equals(moment.getVisibility())) {
            Long hostSpaceId = hostSpaceService.getHostSpaceId();
            return hostSpaceId != null && moment.getSpaceId() != null && moment.getSpaceId().equals(hostSpaceId);
        }
        if (currentSpaceId == null || moment.getSpaceId() == null) {
            return false;
        }
        return currentSpaceId.equals(moment.getSpaceId());
    }
    
    /**
     * 填充动态关联数据
     */
    private void fillMomentData(Moment moment, Long currentUserId) {
        // 用户信息
        User user = userMapper.selectById(moment.getUserId());
        if (user != null) {
            user.setPassword(null);
        }
        moment.setUser(user);
        
        // 媒体文件
        List<MomentMedia> mediaList = mediaMapper.selectList(new LambdaQueryWrapper<MomentMedia>()
                .eq(MomentMedia::getMomentId, moment.getId())
                .orderByAsc(MomentMedia::getSort));
        moment.setMediaList(mediaList);
        
        // 评论
        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getMomentId, moment.getId())
                .orderByAsc(Comment::getCreatedAt));
        java.util.Set<Long> replyUserIds = new java.util.HashSet<>();
        for (Comment comment : comments) {
            User commentUser = userMapper.selectById(comment.getUserId());
            if (commentUser != null) {
                commentUser.setPassword(null);
            }
            comment.setUser(commentUser);
            if (comment.getReplyToUserId() != null) {
                replyUserIds.add(comment.getReplyToUserId());
            }
        }
        if (!replyUserIds.isEmpty()) {
            java.util.Map<Long, User> replyUsers = new java.util.HashMap<>();
            for (User u : userMapper.selectBatchIds(replyUserIds)) {
                if (u != null) {
                    u.setPassword(null);
                    replyUsers.put(u.getId(), u);
                }
            }
            for (Comment comment : comments) {
                if (comment.getReplyToUserId() != null) {
                    comment.setReplyToUser(replyUsers.get(comment.getReplyToUserId()));
                }
            }
        }
        moment.setComments(comments);
        
        // 是否已点赞
        if (currentUserId != null) {
            MomentLike like = likeMapper.selectOne(new LambdaQueryWrapper<MomentLike>()
                    .eq(MomentLike::getMomentId, moment.getId())
                    .eq(MomentLike::getUserId, currentUserId));
            moment.setLiked(like != null);
        }
    }
}
