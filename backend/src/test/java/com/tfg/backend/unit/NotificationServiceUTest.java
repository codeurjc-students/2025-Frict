package com.tfg.backend.unit;

import com.tfg.backend.dto.EntityType;
import com.tfg.backend.model.Notification;
import com.tfg.backend.repository.NotificationRepository;
import com.tfg.backend.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceUTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private static final String USERNAME = "testuser";
    private static final String OTHER_USERNAME = "otheruser";

    private Notification buildNotification(String id, String username, boolean isRead) {
        Notification n = new Notification(username, "Subject", "Description", EntityType.ORDER);
        n.setId(id);
        n.setRead(isRead);
        return n;
    }

    // --- QUERY DELEGATION ---
    @Nested
    @DisplayName("Tests for read-only query delegation")
    class QueryDelegationTests {

        @Test
        @DisplayName("findByUsernameAndIsReadFalseOrderByTimestampDesc delegates to repository")
        void findUnread_DelegatesToRepository() {
            List<Notification> expected = List.of(buildNotification("1", USERNAME, false));
            when(notificationRepository.findByUsernameAndIsReadFalseOrderByTimestampDesc(USERNAME)).thenReturn(expected);

            List<Notification> result = notificationService.findByUsernameAndIsReadFalseOrderByTimestampDesc(USERNAME);

            assertEquals(expected, result);
            verify(notificationRepository).findByUsernameAndIsReadFalseOrderByTimestampDesc(USERNAME);
        }

        @Test
        @DisplayName("getUserNotificationsPage delegates to repository and returns the page")
        void getUserNotificationsPage_DelegatesToRepository() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Notification> expected = new PageImpl<>(List.of(buildNotification("1", USERNAME, false)));
            when(notificationRepository.findByUsernameOrderByTimestampDesc(USERNAME, pageable)).thenReturn(expected);

            Page<Notification> result = notificationService.getUserNotificationsPage(USERNAME, pageable);

            assertEquals(expected, result);
            verify(notificationRepository).findByUsernameOrderByTimestampDesc(USERNAME, pageable);
        }

        @Test
        @DisplayName("getNotificationsByTypePage delegates to repository filtering by type")
        void getNotificationsByTypePage_DelegatesToRepository() {
            Pageable pageable = PageRequest.of(0, 5);
            Page<Notification> expected = new PageImpl<>(List.of(buildNotification("1", USERNAME, false)));
            when(notificationRepository.findByUsernameAndTypeOrderByTimestampDesc(USERNAME, EntityType.ORDER, pageable))
                    .thenReturn(expected);

            Page<Notification> result = notificationService.getNotificationsByTypePage(USERNAME, EntityType.ORDER, pageable);

            assertEquals(expected, result);
            verify(notificationRepository).findByUsernameAndTypeOrderByTimestampDesc(USERNAME, EntityType.ORDER, pageable);
        }
    }

    // --- CREATE ---
    @Nested
    @DisplayName("Tests for createAndSendNotification")
    class CreateNotificationTests {

        @Test
        @DisplayName("createAndSendNotification builds a Notification with the correct fields and saves it")
        void createAndSendNotification_SavesWithCorrectFields() {
            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

            notificationService.createAndSendNotification(USERNAME, "My Subject", "My Desc", EntityType.PRODUCT);

            verify(notificationRepository).save(captor.capture());
            Notification saved = captor.getValue();
            assertEquals(USERNAME, saved.getUsername());
            assertEquals("My Subject", saved.getSubject());
            assertEquals("My Desc", saved.getDescription());
            assertEquals(EntityType.PRODUCT, saved.getType());
            assertFalse(saved.isRead(), "New notifications must default to unread");
            assertNotNull(saved.getTimestamp(), "Timestamp must be set on creation");
        }
    }

    // --- MARK AS READ ---
    @Nested
    @DisplayName("Tests for markAsRead")
    class MarkAsReadTests {

        @Test
        @DisplayName("markAsRead sets isRead to true and saves when the notification is unread and owned by user")
        void markAsRead_Success() {
            Notification notification = buildNotification("notif-1", USERNAME, false);
            when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(notification));

            notificationService.markAsRead("notif-1", USERNAME);

            assertTrue(notification.isRead());
            verify(notificationRepository).save(notification);
        }

        @Test
        @DisplayName("markAsRead throws RuntimeException when the notification is not found")
        void markAsRead_ThrowsWhenNotFound() {
            when(notificationRepository.findById("missing")).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> notificationService.markAsRead("missing", USERNAME));
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("markAsRead throws RuntimeException when the notification belongs to another user")
        void markAsRead_ThrowsWhenNotOwner() {
            Notification notification = buildNotification("notif-1", OTHER_USERNAME, false);
            when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(notification));

            assertThrows(RuntimeException.class, () -> notificationService.markAsRead("notif-1", USERNAME));
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("markAsRead is a no-op when the notification is already read")
        void markAsRead_NoOp_WhenAlreadyRead() {
            Notification notification = buildNotification("notif-1", USERNAME, true);
            when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(notification));

            notificationService.markAsRead("notif-1", USERNAME);

            verify(notificationRepository, never()).save(any());
        }
    }

    // --- MARK ALL AS READ ---
    @Nested
    @DisplayName("Tests for markAllAsRead")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("markAllAsRead sets all unread notifications to read and saves them in bulk")
        void markAllAsRead_Success() {
            Notification n1 = buildNotification("1", USERNAME, false);
            Notification n2 = buildNotification("2", USERNAME, false);
            when(notificationRepository.findByUsernameAndIsReadFalseOrderByTimestampDesc(USERNAME))
                    .thenReturn(List.of(n1, n2));

            notificationService.markAllAsRead(USERNAME);

            assertTrue(n1.isRead());
            assertTrue(n2.isRead());
            verify(notificationRepository).saveAll(List.of(n1, n2));
        }

        @Test
        @DisplayName("markAllAsRead is a no-op when there are no unread notifications")
        void markAllAsRead_NoOp_WhenNoneUnread() {
            when(notificationRepository.findByUsernameAndIsReadFalseOrderByTimestampDesc(USERNAME))
                    .thenReturn(List.of());

            notificationService.markAllAsRead(USERNAME);

            verify(notificationRepository, never()).saveAll(any());
        }
    }

    // --- DELETE ---
    @Nested
    @DisplayName("Tests for deleteNotification")
    class DeleteNotificationTests {

        @Test
        @DisplayName("deleteNotification delegates to repository with both id and username for security")
        void deleteNotification_DelegatesToRepository() {
            notificationService.deleteNotification("notif-1", USERNAME);

            verify(notificationRepository).deleteByIdAndUsername("notif-1", USERNAME);
        }
    }
}
