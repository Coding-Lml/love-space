package com.lovespace.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lovespace.entity.ChatMessage;
import com.lovespace.service.ChatMessageService;
import com.lovespace.service.SpaceService;
import com.lovespace.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final ChatMessageService chatMessageService;
    private final SpaceService spaceService;

    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connected, sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long fromUserId = (Long) session.getAttributes().get("userId");
        JsonNode node = objectMapper.readTree(message.getPayload());
        String type = node.path("type").asText();

        if (fromUserId == null) {
            if (!"auth".equals(type)) {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("unauthorized"));
                return;
            }

            String token = node.path("token").asText(null);
            Long authedUserId = authenticateToken(token);
            if (authedUserId == null) {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("unauthorized"));
                return;
            }

            session.getAttributes().put("userId", authedUserId);
            userSessions
                    .computeIfAbsent(authedUserId, k -> ConcurrentHashMap.newKeySet())
                    .add(session);

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("event", "auth");
            payload.put("status", "ok");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            log.info("WebSocket authed, userId={}, sessionId={}", authedUserId, session.getId());
            return;
        }

        String content = node.path("content").asText(null);
        String mediaUrl = node.path("mediaUrl").asText(null);
        String extra = node.path("extra").isMissingNode() || node.path("extra").isNull()
                ? null
                : node.path("extra").toString();

        Long toUserId = chatMessageService.getPartnerId(fromUserId);
        if (toUserId == null) {
            return;
        }
        Long spaceId = spaceService.getOrCreatePrimarySpaceId(fromUserId);
        if (spaceId == null) {
            return;
        }

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSpaceId(spaceId);
        chatMessage.setFromUserId(fromUserId);
        chatMessage.setToUserId(toUserId);
        chatMessage.setType(type);
        chatMessage.setContent(content);
        chatMessage.setMediaUrl(mediaUrl);
        chatMessage.setExtra(extra);
        chatMessage.setStatus("sent");
        chatMessage.setCreatedAt(LocalDateTime.now());
        chatMessageService.save(chatMessage);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("id", chatMessage.getId());
        payload.put("fromUserId", chatMessage.getFromUserId());
        payload.put("toUserId", chatMessage.getToUserId());
        payload.put("type", chatMessage.getType());
        payload.put("content", chatMessage.getContent());
        payload.put("mediaUrl", chatMessage.getMediaUrl());
        payload.put("extra", chatMessage.getExtra());
        payload.put("status", chatMessage.getStatus());
        payload.put("createdAt", chatMessage.getCreatedAt().toString());

        broadcastToUser(fromUserId, payload);
        broadcastToUser(toUserId, payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
        }
        log.info("WebSocket closed, userId={}, sessionId={}, reason={}", userId, session.getId(), status);
    }

    private Long authenticateToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        if (!jwtUtil.validateToken(token)) {
            return null;
        }
        return jwtUtil.getUserIdFromToken(token);
    }

    private void broadcastToUser(Long userId, ObjectNode payload) throws IOException {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(payload));
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(textMessage);
            }
        }
    }

    public void notifyRead(Long readerId, Long partnerId, java.util.List<Long> messageIds) throws IOException {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        Set<WebSocketSession> sessions = userSessions.get(partnerId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("event", "read");
        payload.put("readerId", readerId);
        payload.put("partnerId", partnerId);
        var array = payload.putArray("messageIds");
        for (Long id : messageIds) {
            array.add(id);
        }
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(payload));
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(textMessage);
            }
        }
    }
}
