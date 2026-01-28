package com.lovespace.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WebSocketOriginInterceptor implements HandshakeInterceptor {

    private final CorsConfiguration corsConfiguration;

    public WebSocketOriginInterceptor(String allowedOriginPatternsCsv) {
        this.corsConfiguration = new CorsConfiguration();
        List<String> patterns = parseCsv(allowedOriginPatternsCsv);
        if (!patterns.isEmpty()) {
            this.corsConfiguration.setAllowedOriginPatterns(patterns);
        }
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String origin = request.getHeaders().getOrigin();
        if (!StringUtils.hasText(origin)) {
            return true;
        }

        if (corsConfiguration.getAllowedOriginPatterns() != null && !corsConfiguration.getAllowedOriginPatterns().isEmpty()) {
            String allowed = corsConfiguration.checkOrigin(origin);
            if (allowed != null) {
                return true;
            }
        }

        var host = request.getHeaders().getHost();
        if (host != null) {
            try {
                URI originUri = URI.create(origin);
                if (originUri.getHost() != null && originUri.getHost().equalsIgnoreCase(host.getHostString())) {
                    int originPort = normalizePort(originUri.getScheme(), originUri.getPort());
                    String requestScheme = request.getURI() != null ? request.getURI().getScheme() : null;
                    int hostPort = normalizePort(requestScheme, host.getPort());
                    if (originPort == hostPort) {
                        return true;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        response.setStatusCode(HttpStatus.FORBIDDEN);
        return false;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }

    private static List<String> parseCsv(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private static int normalizePort(String scheme, int port) {
        if (port > 0) {
            return port;
        }
        if ("https".equalsIgnoreCase(scheme)) {
            return 443;
        }
        if ("http".equalsIgnoreCase(scheme)) {
            return 80;
        }
        return -1;
    }
}
