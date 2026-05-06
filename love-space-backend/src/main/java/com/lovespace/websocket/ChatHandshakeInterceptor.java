package com.lovespace.websocket;

import com.lovespace.service.SpaceService;
import com.lovespace.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final SpaceService spaceService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");
        if (token == null || token.isBlank() || !jwtUtil.validateToken(token)) {
            return false;
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        Long partnerUserId = spaceService.getPartnerUserIdInPrimarySpace(userId);
        if (partnerUserId == null) {
            return false;
        }
        attributes.put("userId", userId);
        attributes.put("username", jwtUtil.getUsernameFromToken(token));
        attributes.put("partnerUserId", partnerUserId);
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
    }
}
