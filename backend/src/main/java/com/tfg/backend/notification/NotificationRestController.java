package com.tfg.backend.notification;

import com.tfg.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationRestController {

    private final NotificationService notificationService;

    @PostMapping("/test")
    public ResponseEntity<?> testNotification(Authentication authentication) {
        String username = authentication.getName();

        notificationService.createAndSendNotification(
                username,
                "Prueba correcta",
                "Spring y Angular se están comunicando correctamente mediante WebSockets.",
                NotificationType.USER
        );

        return ResponseEntity.ok(Map.of("message", "Notificación disparada al usuario: " + username));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(Authentication authentication) {
        String username = authentication.getName();

        // Check MongoDB
        List<Notification> unread = notificationService
                .findByUsernameAndIsReadFalseOrderByTimestampDesc(username);

        // Map to DTO
        List<NotificationDTO> dtos = unread.stream()
                .map(n -> new NotificationDTO(n.getId(), n.getSubject(), n.getDescription(), n.getTimestamp(), n.isRead(), n.getType()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Boolean> markNotificationAsRead(@PathVariable("id") String id, Authentication authentication) {
        String username = authentication.getName();

        try {
            notificationService.markAsRead(id, username);
            return ResponseEntity.ok(true);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }
}
