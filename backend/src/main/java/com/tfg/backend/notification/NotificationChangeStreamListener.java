package com.tfg.backend.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import org.springframework.data.mongodb.core.messaging.Message;
import org.springframework.data.mongodb.core.messaging.MessageListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationChangeStreamListener implements MessageListener<ChangeStreamDocument<Document>, Notification> {

    private final NotificationWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message<ChangeStreamDocument<Document>, Notification> message) {
        Notification notification = message.getBody();

        if (notification != null) {
            String destinatarioReal = notification.getUsername();
            try {
                Map<String, Object> wsMessage = Map.of(
                        "topic", "NOTIFICATIONS",
                        "action", "NEW",
                        "payload", notification
                );

                String jsonMessage = objectMapper.writeValueAsString(wsMessage);
                webSocketHandler.sendMessageToUser(destinatarioReal, jsonMessage);

            } catch (Exception e) {
                log.error("Error al retransmitir cambio de MongoDB a WebSocket", e);
            }
        }
    }
}