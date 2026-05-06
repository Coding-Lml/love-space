package com.lovespace.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lovespace.common.Result;
import com.lovespace.dto.ChatMessageRequest;
import com.lovespace.entity.ChatMessage;
import com.lovespace.service.ChatService;
import com.lovespace.service.FileService;
import com.lovespace.service.SpaceService;
import com.lovespace.util.UserContext;
import com.lovespace.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@PreAuthorize("@roleService.isOwner()")
public class ChatController {

    private final ChatService chatService;
    private final FileService fileService;
    private final SpaceService spaceService;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ObjectMapper objectMapper;

    @GetMapping("/messages")
    public Result<Page<ChatMessage>> getMessages(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return chatService.getMessages(UserContext.getCurrentUserId(), pageNum, pageSize);
    }

    @PostMapping("/messages")
    public Result<ChatMessage> sendMessage(@RequestBody ChatMessageRequest request) {
        Result<ChatMessage> result = chatService.sendTextOrSticker(
                UserContext.getCurrentUserId(),
                request.getType(),
                request.getContent(),
                request.getMediaUrl()
        );
        publishIfSuccessful(result);
        return result;
    }

    @PostMapping("/media")
    public Result<ChatMessage> sendMedia(
            @RequestParam("type") String type,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) Integer duration) {
        String normalizedType = type == null ? "" : type.trim().toLowerCase();
        Result<String> uploadResult = fileService.uploadFile(file);
        if (uploadResult.getCode() != 200) {
            return Result.error(uploadResult.getCode(), uploadResult.getMessage());
        }

        String fileType = fileService.getFileType(file.getContentType());
        if (!normalizedType.equals(fileType)) {
            fileService.deleteFile(uploadResult.getData());
            return Result.error(400, "文件类型与消息类型不匹配");
        }

        Result<ChatMessage> result = chatService.sendMedia(
                UserContext.getCurrentUserId(),
                normalizedType,
                uploadResult.getData(),
                content,
                buildExtra(normalizedType, uploadResult.getData(), duration)
        );
        if (result.getCode() != 200) {
            fileService.deleteFile(uploadResult.getData());
        }
        publishIfSuccessful(result);
        return result;
    }

    @PostMapping("/read")
    public Result<Integer> markRead() {
        Long userId = UserContext.getCurrentUserId();
        Result<Integer> result = chatService.markRead(userId);
        if (result.getCode() == 200) {
            Long partnerUserId = spaceService.getPartnerUserIdInPrimarySpace(userId);
            chatWebSocketHandler.sendReadEvent(userId, partnerUserId, result.getData());
            chatWebSocketHandler.sendUnreadUpdate(userId, 0L);
        }
        return result;
    }

    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        return chatService.getUnreadCount(UserContext.getCurrentUserId());
    }

    private void publishIfSuccessful(Result<ChatMessage> result) {
        if (result.getCode() != 200 || result.getData() == null) {
            return;
        }
        ChatMessage message = result.getData();
        chatWebSocketHandler.broadcastMessage(message);
        Result<Long> unread = chatService.getUnreadCount(message.getToUserId());
        if (unread.getCode() == 200) {
            chatWebSocketHandler.sendUnreadUpdate(message.getToUserId(), unread.getData());
        }
    }

    private String buildExtra(String type, String url, Integer duration) {
        ObjectNode node = objectMapper.createObjectNode();
        if (ChatService.TYPE_IMAGE.equals(type)) {
            node.put("thumbnail", fileService.buildThumbnailUrl(url));
        }
        if (ChatService.TYPE_AUDIO.equals(type) && duration != null && duration > 0) {
            node.put("duration", duration);
        }
        if (node.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
