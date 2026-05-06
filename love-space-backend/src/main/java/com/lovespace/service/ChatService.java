package com.lovespace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lovespace.common.Result;
import com.lovespace.entity.ChatMessage;
import com.lovespace.entity.User;
import com.lovespace.mapper.ChatMessageMapper;
import com.lovespace.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_AUDIO = "audio";
    public static final String TYPE_STICKER = "sticker";

    private static final String STATUS_SENT = "sent";
    private static final String STATUS_READ = "read";
    private static final int MAX_TEXT_LENGTH = 2000;

    private final ChatMessageMapper chatMessageMapper;
    private final SpaceService spaceService;

    @Autowired(required = false)
    private UserMapper userMapper;

    public ChatService(ChatMessageMapper chatMessageMapper, SpaceService spaceService) {
        this.chatMessageMapper = chatMessageMapper;
        this.spaceService = spaceService;
    }

    public Result<ChatMessage> sendTextOrSticker(Long userId, String type, String content, String mediaUrl) {
        String normalizedType = normalizeType(type);
        if (!TYPE_TEXT.equals(normalizedType) && !TYPE_STICKER.equals(normalizedType)) {
            return Result.error(400, "不支持的消息类型");
        }

        if (TYPE_TEXT.equals(normalizedType)) {
            Result<String> textResult = normalizeContent(content);
            if (textResult.getCode() != 200) {
                return Result.error(textResult.getCode(), textResult.getMessage());
            }
            content = textResult.getData();
            if (content == null) {
                return Result.error(400, "消息内容不能为空");
            }
        } else {
            Result<String> contentResult = normalizeOptionalContent(content);
            if (contentResult.getCode() != 200) {
                return Result.error(contentResult.getCode(), contentResult.getMessage());
            }
            content = contentResult.getData();
            if (mediaUrl == null || mediaUrl.isBlank()) {
                return Result.error(400, "表情包不能为空");
            }
        }

        ChatPeer peer = resolvePeer(userId);
        if (peer.error != null) {
            return Result.error(peer.errorCode, peer.error);
        }

        ChatMessage message = buildMessage(peer, normalizedType, content, mediaUrl, null);
        chatMessageMapper.insert(message);
        fillUsers(message);
        return Result.success("发送成功", message);
    }

    public Result<ChatMessage> sendMedia(Long userId, String type, String mediaUrl, String content, String extra) {
        String normalizedType = normalizeType(type);
        if (!TYPE_IMAGE.equals(normalizedType) && !TYPE_AUDIO.equals(normalizedType)) {
            return Result.error(400, "不支持的媒体类型");
        }
        if (mediaUrl == null || mediaUrl.isBlank()) {
            return Result.error(400, "媒体文件不能为空");
        }

        Result<String> contentResult = normalizeOptionalContent(content);
        if (contentResult.getCode() != 200) {
            return Result.error(contentResult.getCode(), contentResult.getMessage());
        }
        String normalizedContent = contentResult.getData();
        ChatPeer peer = resolvePeer(userId);
        if (peer.error != null) {
            return Result.error(peer.errorCode, peer.error);
        }

        ChatMessage message = buildMessage(peer, normalizedType, normalizedContent, mediaUrl, normalizeExtra(extra));
        chatMessageMapper.insert(message);
        fillUsers(message);
        return Result.success("发送成功", message);
    }

    public Result<Page<ChatMessage>> getMessages(Long userId, Integer pageNum, Integer pageSize) {
        ChatPeer peer = resolvePeer(userId);
        if (peer.error != null) {
            return Result.error(peer.errorCode, peer.error);
        }

        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        Page<ChatMessage> page = new Page<>(safePageNum, safePageSize);
        Page<ChatMessage> result = chatMessageMapper.selectPage(page, new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSpaceId, peer.spaceId)
                .and(w -> w
                        .and(a -> a.eq(ChatMessage::getFromUserId, userId).eq(ChatMessage::getToUserId, peer.partnerUserId))
                        .or(a -> a.eq(ChatMessage::getFromUserId, peer.partnerUserId).eq(ChatMessage::getToUserId, userId)))
                .orderByDesc(ChatMessage::getCreatedAt)
                .orderByDesc(ChatMessage::getId));
        fillUsers(result.getRecords());
        return Result.success(result);
    }

    public Result<Integer> markRead(Long userId) {
        ChatPeer peer = resolvePeer(userId);
        if (peer.error != null) {
            return Result.error(peer.errorCode, peer.error);
        }

        ChatMessage update = new ChatMessage();
        update.setStatus(STATUS_READ);
        int count = chatMessageMapper.update(update, new LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getSpaceId, peer.spaceId)
                .eq(ChatMessage::getFromUserId, peer.partnerUserId)
                .eq(ChatMessage::getToUserId, userId)
                .eq(ChatMessage::getStatus, STATUS_SENT));
        return Result.success(count);
    }

    public Result<Long> getUnreadCount(Long userId) {
        ChatPeer peer = resolvePeer(userId);
        if (peer.error != null) {
            return Result.error(peer.errorCode, peer.error);
        }
        Long count = chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSpaceId, peer.spaceId)
                .eq(ChatMessage::getFromUserId, peer.partnerUserId)
                .eq(ChatMessage::getToUserId, userId)
                .eq(ChatMessage::getStatus, STATUS_SENT));
        return Result.success(count == null ? 0L : count);
    }

    private ChatMessage buildMessage(ChatPeer peer, String type, String content, String mediaUrl, String extra) {
        ChatMessage message = new ChatMessage();
        message.setSpaceId(peer.spaceId);
        message.setFromUserId(peer.userId);
        message.setToUserId(peer.partnerUserId);
        message.setType(type);
        message.setContent(content);
        message.setMediaUrl(mediaUrl == null || mediaUrl.isBlank() ? null : mediaUrl.trim());
        message.setExtra(extra);
        message.setStatus(STATUS_SENT);
        return message;
    }

    private ChatPeer resolvePeer(Long userId) {
        if (userId == null) {
            return ChatPeer.error(401, "请先登录");
        }
        Long spaceId = spaceService.getOrCreatePrimarySpaceId(userId);
        Long partnerUserId = spaceService.getPartnerUserIdInPrimarySpace(userId);
        if (spaceId == null || partnerUserId == null) {
            return ChatPeer.error(403, "当前空间没有可聊天的另一半");
        }
        return ChatPeer.ok(userId, spaceId, partnerUserId);
    }

    private String normalizeType(String type) {
        return type == null ? "" : type.trim().toLowerCase();
    }

    private Result<String> normalizeContent(String content) {
        return normalizeOptionalContent(content);
    }

    private Result<String> normalizeOptionalContent(String content) {
        if (content == null) {
            return Result.success(null);
        }
        String trimmed = content.trim();
        if (trimmed.isEmpty()) {
            return Result.success(null);
        }
        if (trimmed.length() > MAX_TEXT_LENGTH) {
            return Result.error(400, "消息内容不能超过2000字");
        }
        return Result.success(trimmed);
    }

    private String normalizeExtra(String extra) {
        if (extra == null || extra.isBlank()) {
            return null;
        }
        return extra.trim();
    }

    private void fillUsers(List<ChatMessage> messages) {
        if (messages == null) {
            return;
        }
        for (ChatMessage message : messages) {
            fillUsers(message);
        }
    }

    private void fillUsers(ChatMessage message) {
        if (message == null || userMapper == null) {
            return;
        }
        User from = userMapper.selectById(message.getFromUserId());
        User to = userMapper.selectById(message.getToUserId());
        scrub(from);
        scrub(to);
        message.setFromUser(from);
        message.setToUser(to);
    }

    private void scrub(User user) {
        if (user != null) {
            user.setPassword(null);
        }
    }

    private static class ChatPeer {
        private Long userId;
        private Long spaceId;
        private Long partnerUserId;
        private Integer errorCode;
        private String error;

        private static ChatPeer ok(Long userId, Long spaceId, Long partnerUserId) {
            ChatPeer peer = new ChatPeer();
            peer.userId = userId;
            peer.spaceId = spaceId;
            peer.partnerUserId = partnerUserId;
            return peer;
        }

        private static ChatPeer error(Integer code, String message) {
            ChatPeer peer = new ChatPeer();
            peer.errorCode = code;
            peer.error = message;
            return peer;
        }
    }
}
