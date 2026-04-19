package com.tfg.backend.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    public List<Notification> findByUsernameAndIsReadFalseOrderByTimestampDesc(String username){
        return this.notificationRepository.findByUsernameAndIsReadFalseOrderByTimestampDesc(username);
    }

    public void createAndSendNotification(String username, String subject, String description, NotificationType type) {

        // 1. Save in MongoDB
        Notification notification = new Notification(username, subject, description, type);
        Notification savedNotification = notificationRepository.save(notification);

        // 2. Map to NotificationDTO
        NotificationDTO payloadDto = new NotificationDTO(
                savedNotification.getId(),
                savedNotification.getSubject(),
                savedNotification.getDescription(),
                savedNotification.getTimestamp(),
                savedNotification.isRead(),
                savedNotification.getType()
        );

        // 3. Emit via WebSocket
        try {
            Map<String, Object> message = Map.of(
                    "topic", "NOTIFICATIONS",
                    "action", "NEW",
                    "payload", payloadDto
            );

            String jsonMessage = objectMapper.writeValueAsString(message);
            webSocketHandler.sendMessageToUser(username, jsonMessage);

        } catch (Exception e) {
            log.error("Error al procesar el mensaje de notificación para el usuario: {}", username, e);
        }
    }


    public void markAsRead(String notificationId, String username) {
        // 1. Find the notification
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));

        // 2. Check the user is the owner of the notification
        if (!notification.getUsername().equals(username)) {
            throw new RuntimeException("No tienes permiso para modificar esta notificación");
        }

        // 3. If it was already read do not send the request to backend
        if (notification.isRead()) {
            return;
        }

        // 4. Update and save in MongoDB
        notification.setRead(true);
        notificationRepository.save(notification);
    }
}