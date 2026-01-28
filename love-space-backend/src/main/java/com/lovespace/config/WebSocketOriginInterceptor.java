package com.lovespace.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.net.InetSocketAddress;
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
                String originHost = originUri.getHost();
                String hostString = host.getHostString();
                if (originHost != null && hostString != null) {
                    if (originHost.equalsIgnoreCase(hostString)) {
                        if (isPortCheckSafe(request, originUri, host)) {
                            int originPort = normalizePort(originUri.getScheme(), originUri.getPort());
                            int hostPort = resolveExternalPort(request, host);
                            if (originPort == hostPort) {
                                return true;
                            }
                        } else {
                            return true;
                        }
                    }
                    if (isLocalHost(hostString) && isLocalHost(originHost)) {
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

    private static boolean isLocalHost(String host) {
        if (!StringUtils.hasText(host)) {
            return false;
        }
        String h = host.trim().toLowerCase();
        return "localhost".equals(h) || "127.0.0.1".equals(h) || "0.0.0.0".equals(h);
    }

    private static int resolveExternalPort(ServerHttpRequest request, InetSocketAddress host) {
        if (host.getPort() > 0) {
            return host.getPort();
        }

        String forwardedPort = request.getHeaders().getFirst("X-Forwarded-Port");
        if (StringUtils.hasText(forwardedPort)) {
            try {
                int p = Integer.parseInt(forwardedPort.trim());
                if (p > 0) {
                    return p;
                }
            } catch (Exception ignored) {
            }
        }

        String forwardedProto = request.getHeaders().getFirst("X-Forwarded-Proto");
        String scheme = StringUtils.hasText(forwardedProto)
                ? forwardedProto.trim()
                : (request.getURI() != null ? request.getURI().getScheme() : null);
        return normalizePort(scheme, -1);
    }

    private static boolean isPortCheckSafe(ServerHttpRequest request, URI originUri, InetSocketAddress host) {
        if (originUri == null || host == null) {
            return false;
        }
        if (originUri.getPort() >= 0) {
            return true;
        }
        if (host.getPort() > 0) {
            return true;
        }
        String forwardedPort = request.getHeaders().getFirst("X-Forwarded-Port");
        return StringUtils.hasText(forwardedPort);
    }
}
