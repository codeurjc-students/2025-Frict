package com.tfg.backend.utils;

import com.tfg.backend.model.Notification;
import com.tfg.backend.utils.NotificationWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document; // IMPORTANTE: Añadir este import
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import org.springframework.data.mongodb.core.messaging.Message;
import org.springframework.data.mongodb.core.messaging.MessageListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
// CAMBIO AQUÍ: El primer genérico ahora usa org.bson.Document
public class NotificationChangeStreamListener implements MessageListener<ChangeStreamDocument<Document>, Notification> {

    private final NotificationWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    @Override
    // CAMBIO AQUÍ: Actualizamos la firma del método para que coincida
    public void onMessage(Message<ChangeStreamDocument<Document>, Notification> message) {

        // Spring ya ha hecho la magia por nosotros, el getBody() sí devuelve tu Notification
        Notification notification = message.getBody();

        if (notification != null) {
            String destinatarioReal = notification.getUsername();
            log.info("Evento detectado en MongoDB para el usuario: {}", destinatarioReal);

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