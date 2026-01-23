package com.lovespace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lovespace.common.Result;
import com.lovespace.entity.*;
import com.lovespace.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MomentService extends ServiceImpl<MomentMapper, Moment> {
    
    private final MomentMediaMapper mediaMapper;
    private final CommentMapper commentMapper;
    private final MomentLikeMapper likeMapper;
    private final UserMapper userMapper;
    private final FileService fileService;
    
    /**
     * 发布动态
     */
    @Transactional
    public Result<Moment> publish(Long userId, String content, String location, List<MomentMedia> mediaList) {
        // 创建动态
        Moment moment = new Moment();
        moment.setUserId(userId);
        moment.setContent(content);
        moment.setLocation(location);
        moment.setLikes(0);
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
        
        // 查询动态
        Page<Moment> momentPage = this.page(page, new LambdaQueryWrapper<Moment>()
                .orderByDesc(Moment::getCreatedAt));
        
        // 填充关联数据
        for (Moment moment : momentPage.getRecords()) {
            fillMomentData(moment, currentUserId);
        }
        
        return Result.success(momentPage);
    }
    
    /**
     * 获取动态详情
     */
    public Result<Moment> getDetail(Long momentId, Long currentUserId) {
        Moment moment = this.getById(momentId);
        if (moment == null) {
            return Result.error("动态不存在");
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
        if (!moment.getUserId().equals(userId)) {
            return Result.error("只能删除自己的动态");
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
     * 添加评论
     */
    public Result<Comment> addComment(Long momentId, Long userId, String content) {
        Moment moment = this.getById(momentId);
        if (moment == null) {
            return Result.error("动态不存在");
        }
        
        Comment comment = new Comment();
        comment.setMomentId(momentId);
        comment.setUserId(userId);
        comment.setContent(content);
        commentMapper.insert(comment);
        
        // 填充用户信息
        comment.setUser(userMapper.selectById(userId));
        
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
        if (!comment.getUserId().equals(userId)) {
            return Result.error("只能删除自己的评论");
        }
        
        commentMapper.deleteById(commentId);
        return Result.success("删除成功", null);
    }
    
    /**
     * 获取最新动态（首页用）
     */
    public List<Moment> getRecentMoments(Long currentUserId, int limit) {
        List<Moment> moments = this.list(new LambdaQueryWrapper<Moment>()
                .orderByDesc(Moment::getCreatedAt)
                .last("LIMIT " + limit));
        
        for (Moment moment : moments) {
            fillMomentData(moment, currentUserId);
        }
        return moments;
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
        for (Comment comment : comments) {
            User commentUser = userMapper.selectById(comment.getUserId());
            if (commentUser != null) {
                commentUser.setPassword(null);
            }
            comment.setUser(commentUser);
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
