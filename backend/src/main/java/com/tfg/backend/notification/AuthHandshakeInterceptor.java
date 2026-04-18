package com.tfg.backend.notification;

import com.tfg.backend.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            Cookie[] cookies = servletRequest.getServletRequest().getCookies();

            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("AuthToken".equals(cookie.getName())) {
                        String jwtToken = cookie.getValue();

                        try {
                            var claims = jwtTokenProvider.validateToken(jwtToken);
                            String username = claims.getSubject();

                            if (username != null) {
                                attributes.put("userId", username);
                                log.info("WS Handshake accepted for user: {}", username);
                                return true;
                            }
                        } catch (Exception e) {
                            log.error("Error validating AuthToken in WS: {}", e.getMessage());
                        }
                    }
                }
            }
            log.warn("WS Handshake rejected: Valid AuthToken cookie not found.");
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {}
}