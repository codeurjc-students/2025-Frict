package com.tfg.backend.notification;

import com.tfg.backend.dto.PageResponse;
import com.tfg.backend.utils.PageFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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


    @GetMapping("/")
    public ResponseEntity<PageResponse<NotificationDTO>> getNotificationsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Authentication authentication) {

        String username = authentication.getName();
        Page<Notification> notificationsPage = notificationService.getUserNotificationsPage(username, PageRequest.of(page, size));
        return ResponseEntity.ok(PageFormatter.toPageResponse(notificationsPage, NotificationDTO::new));
    }


    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(Authentication authentication) {
        String username = authentication.getName();

        // Check MongoDB
        List<Notification> unread = notificationService
                .findByUsernameAndIsReadFalseOrderByTimestampDesc(username);

        // Map to DTO
        List<NotificationDTO> dtos = unread.stream().map(NotificationDTO::new).collect(Collectors.toList());
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

    @PutMapping("/read-all")
    public ResponseEntity<Boolean> markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(authentication.getName());
        return ResponseEntity.ok(true);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteNotification(@PathVariable("id") String id, Authentication authentication) {
        notificationService.deleteNotification(id, authentication.getName());
        return ResponseEntity.ok(true);
    }
}
