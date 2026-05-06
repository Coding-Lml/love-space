package com.lovespace.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovespace.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Map<Long, Set<WebSocketSession>> sessionsByUserId = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            closeQuietly(session, CloseStatus.NOT_ACCEPTABLE.withReason("missing user"));
            return;
        }
        sessionsByUserId.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
        sendEvent(userId, "socket:ready", Map.of("userId", userId));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> sessions = sessionsByUserId.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByUserId.remove(userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if ("ping".equalsIgnoreCase(message.getPayload())) {
            sendSessionEvent(session, "socket:pong", Map.of("time", System.currentTimeMillis()));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("聊天 WebSocket 连接异常: {}", exception.getMessage());
        closeQuietly(session, CloseStatus.SERVER_ERROR);
    }

    public void broadcastMessage(ChatMessage message) {
        if (message == null) {
            return;
        }
        sendEvent(message.getFromUserId(), "message:new", message);
        sendEvent(message.getToUserId(), "message:new", message);
    }

    public void sendReadEvent(Long readerUserId, Long partnerUserId, Integer count) {
        if (readerUserId == null || partnerUserId == null) {
            return;
        }
        Map<String, Object> payload = Map.of(
                "readerUserId", readerUserId,
                "partnerUserId", partnerUserId,
                "count", count == null ? 0 : count
        );
        sendEvent(readerUserId, "message:read", payload);
        sendEvent(partnerUserId, "message:read", payload);
    }

    public void sendUnreadUpdate(Long userId, Long count) {
        if (userId == null) {
            return;
        }
        sendEvent(userId, "unread:update", Map.of("count", count == null ? 0 : count));
    }

    private void sendEvent(Long userId, String event, Object data) {
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> sessions = sessionsByUserId.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (WebSocketSession session : sessions) {
            sendSessionEvent(session, event, data);
        }
    }

    private void sendSessionEvent(WebSocketSession session, String event, Object data) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            String body = objectMapper.writeValueAsString(Map.of("event", event, "data", data));
            session.sendMessage(new TextMessage(body));
        } catch (IOException e) {
            log.warn("发送聊天 WebSocket 消息失败: {}", e.getMessage());
            closeQuietly(session, CloseStatus.SERVER_ERROR);
        }
    }

    private Long getUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        if (userId instanceof Long id) {
            return id;
        }
        return null;
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            if (session != null && session.isOpen()) {
                session.close(status);
            }
        } catch (IOException ignored) {
        }
    }
}
