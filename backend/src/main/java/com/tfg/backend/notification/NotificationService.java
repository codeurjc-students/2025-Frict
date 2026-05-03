package com.tfg.backend.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public Page<Notification> getNotificationsByTypePage(String username, EntityType type, Pageable pageable) {
        return notificationRepository.findByUsernameAndTypeOrderByTimestampDesc(username, type, pageable);
    }

    public void createAndSendNotification(String username, String subject, String description, EntityType type) {
        Notification notification = new Notification(username, subject, description, type);
        notificationRepository.save(notification);
    }


    public Page<Notification> getUserNotificationsPage(String username, Pageable pageable) {
        return notificationRepository.findByUsernameOrderByTimestampDesc(username, pageable);
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

    public void markAllAsRead(String username) {
        List<Notification> unread = notificationRepository.findByUsernameAndIsReadFalseOrderByTimestampDesc(username);
        if (!unread.isEmpty()) {
            unread.forEach(n -> n.setRead(true));
            notificationRepository.saveAll(unread);
        }
    }

    public void deleteNotification(String id, String username) {
        // El repositorio se encarga de verificar que coincida el id y el username por seguridad
        notificationRepository.deleteByIdAndUsername(id, username);
    }

}