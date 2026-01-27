package com.lovespace.controller;

import com.lovespace.common.Result;
import com.lovespace.entity.ChatMessage;
import com.lovespace.service.ChatMessageService;
import com.lovespace.websocket.ChatWebSocketHandler;
import com.lovespace.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@PreAuthorize("@roleService.isOwner()")
public class ChatController {

    private final ChatMessageService chatMessageService;
    private final ChatWebSocketHandler chatWebSocketHandler;

    @GetMapping("/history")
    public Result<List<ChatMessage>> history(
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = UserContext.getCurrentUserId();
        Long partnerId = chatMessageService.getPartnerId(userId);
        if (partnerId == null) {
            return Result.success(List.of());
        }
        int limit = size == null || size <= 0 ? 20 : Math.min(size, 100);
        List<ChatMessage> history = chatMessageService.getHistory(userId, partnerId, beforeId, limit);
        return Result.success(history);
    }

    @org.springframework.web.bind.annotation.PostMapping("/read")
    public Result<List<Long>> markRead() {
        Long userId = UserContext.getCurrentUserId();
        Long partnerId = chatMessageService.getPartnerId(userId);
        if (partnerId == null) {
            return Result.success(List.of());
        }
        List<Long> ids = chatMessageService.markAllRead(userId, partnerId);
        if (!ids.isEmpty()) {
            try {
                chatWebSocketHandler.notifyRead(userId, partnerId, ids);
            } catch (java.io.IOException ignored) {
            }
        }
        return Result.success(ids);
    }
}
