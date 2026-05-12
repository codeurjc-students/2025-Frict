package com.tfg.backend.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.tfg.backend.config.NotificationWebSocketHandler;
import com.tfg.backend.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
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
                log.error("Error while transmitting change from MongoDB to WebSocket: ", e);
            }
        }
    }
}