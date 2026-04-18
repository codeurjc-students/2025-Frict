package com.tfg.backend.notification; // Ajusta tu paquete

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Slf4j
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = (String) session.getAttributes().get("userId");

        if (username != null) {
            // 1. Envolvemos la sesión cruda para protegerla de colisiones (TEXT_PARTIAL_WRITING)
            WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator (
                    session,
                    10000, // Tiempo límite si hay atasco (10s)
                    65536  // Tamaño de la cola de espera (64KB)
            );

            // 2. Guardamos la sesión PROTEGIDA en tu estructura de múltiples dispositivos
            userSessions.computeIfAbsent(username, k -> new CopyOnWriteArraySet<>()).add(safeSession);

            log.info("✅ WebSocket opened and saved securely for user: " + username);
        } else {
            try {
                log.warn("❌ Closing WS session: userId not found in attributes.");
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthenticated"));
            } catch (IOException e) {
                log.error("Error logging out", e);
            }
        }
    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String username = (String) session.getAttributes().get("userId");
        if (username != null) {
            Set<WebSocketSession> sessions = userSessions.get(username);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(username);
                }
            }
            log.info("🔌 WS closed for user: " + username);
        }
    }

    public void sendMessageToUser(String username, String messageJson) {
        Set<WebSocketSession> sessions = userSessions.get(username);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        synchronized (session) {
                            session.sendMessage(new TextMessage(messageJson));
                        }
                    } catch (IOException e) {
                        log.error("Error sending message via WS to user: " + username, e);
                    }
                }
            }
        }
    }
}