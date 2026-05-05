package com.tfg.backend.config;

import com.tfg.backend.model.UserConnection;
import com.tfg.backend.repository.UserConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor // Added to inject Mongo
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    // Inject Mongo connections repository
    private final UserConnectionRepository connectionRepository;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = (String) session.getAttributes().get("userId");

        if (username != null) {
            // 1. Wrap the raw session
            WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator (
                    session,
                    10000,
                    65536
            );

            // 2. Save the session using its ID
            userSessions.computeIfAbsent(username, k -> new ConcurrentHashMap<>())
                    .put(session.getId(), safeSession);

            // 3. Connection Logic (MongoDB)
            updatePresenceOnConnect(username);

            log.info("✅ WebSocket opened and saved securely for user: {}", username);
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
            ConcurrentHashMap<String, WebSocketSession> sessions = userSessions.get(username);
            if (sessions != null) {
                sessions.remove(session.getId());

                if (sessions.isEmpty()) {
                    userSessions.remove(username);
                    // Connection Logic (MongoDB)
                    updatePresenceOnDisconnect(username);
                }
            }
            log.info("🔌 WS closed for user: {}", username);
        }
    }

    // ==========================================
    // MONGODB UTILITY METHODS
    // ==========================================

    private void updatePresenceOnConnect(String username) {
        // Execute in a separate virtual thread to avoid blocking the WebSocket establishment
        Thread.startVirtualThread(() -> {
            try {
                UserConnection conn = connectionRepository.findById(username)
                        .orElse(new UserConnection(username));

                // If already online (e.g., opened another tab), just update the timestamp
                conn.setOnline(true);
                conn.setLastConnected(LocalDateTime.now());

                connectionRepository.save(conn);
                log.debug("MongoDB: User {} marked as ONLINE", username);
            } catch (Exception e) {
                log.error("Error updating presence in Mongo on connect for: {}", username, e);
            }
        });
    }

    private void updatePresenceOnDisconnect(String username) {
        // Execute asynchronously
        Thread.startVirtualThread(() -> {
            try {
                connectionRepository.findById(username).ifPresent(conn -> {
                    LocalDateTime now = LocalDateTime.now();

                    conn.setOnline(false);
                    conn.setLastDisconnected(now);

                    // Calculate session duration
                    if (conn.getLastConnected() != null) {
                        long durationInSeconds = Duration.between(conn.getLastConnected(), now).getSeconds();
                        conn.setLastSessionDurationSeconds(durationInSeconds);
                        conn.setTotalAccumulatedTimeSeconds(conn.getTotalAccumulatedTimeSeconds() + durationInSeconds);
                    }

                    connectionRepository.save(conn);
                    log.debug("MongoDB: User {} marked as OFFLINE (Session lasted {}s)",
                            username, conn.getLastSessionDurationSeconds());
                });
            } catch (Exception e) {
                log.error("Error updating presence in Mongo on disconnect for: {}", username, e);
            }
        });
    }

    public void sendMessageToUser(String username, String messageJson) {
        ConcurrentHashMap<String, WebSocketSession> sessions = userSessions.get(username);
        if (sessions != null) {
            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(messageJson));
                    } catch (IOException e) {
                        log.error("Error sending message via WS to user: {}", username, e);
                    }
                }
            }
        }
    }
}