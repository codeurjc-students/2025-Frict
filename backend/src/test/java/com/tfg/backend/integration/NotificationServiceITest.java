package com.tfg.backend.integration;

import com.tfg.backend.dto.EntityType;
import com.tfg.backend.model.Notification;
import com.tfg.backend.repository.NotificationRepository;
import com.tfg.backend.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for NotificationService.
 * Validates the full lifecycle of notifications against a real MongoDB instance:
 * creation, querying (with filters and pagination), ownership-based access control,
 * bulk and individual marking as read, and deletion.
 */
@SpringBootTest
@ActiveProfiles("test")
public class NotificationServiceITest {

    @Autowired private NotificationService notificationService;
    @Autowired private NotificationRepository notificationRepository;

    private static final String USERNAME = "notif-itest-user";
    private static final String OTHER_USERNAME = "notif-itest-other";

    @AfterEach
    void cleanUp() {
        notificationRepository.deleteAll();
    }

    // --- CREATE ---

    @Test
    @DisplayName("createAndSendNotification: Persists notification with all correct fields in MongoDB")
    void testCreateAndSendNotification_PersistsCorrectFields() {
        notificationService.createAndSendNotification(USERNAME, "Test Subject", "Test Description", EntityType.ORDER);

        List<Notification> saved = notificationRepository.findByUsernameAndIsReadFalseOrderByTimestampDesc(USERNAME);

        assertAll(
                () -> assertEquals(1, saved.size()),
                () -> assertEquals(USERNAME, saved.get(0).getUsername()),
                () -> assertEquals("Test Subject", saved.get(0).getSubject()),
                () -> assertEquals("Test Description", saved.get(0).getDescription()),
                () -> assertEquals(EntityType.ORDER, saved.get(0).getType()),
                () -> assertFalse(saved.get(0).isRead(), "New notification must default to unread"),
                () -> assertNotNull(saved.get(0).getTimestamp(), "Timestamp must be set on creation")
        );
    }

    // --- FIND UNREAD ---

    @Test
    @DisplayName("findByUsernameAndIsReadFalseOrderByTimestampDesc: Returns only unread notifications for the target user")
    void testFindUnread_ReturnsOnlyUnreadForTargetUser() {
        notificationService.createAndSendNotification(USERNAME, "Unread 1", "Desc", EntityType.ORDER);
        notificationService.createAndSendNotification(USERNAME, "Unread 2", "Desc", EntityType.PRODUCT);
        notificationService.createAndSendNotification(OTHER_USERNAME, "Other user unread", "Desc", EntityType.ORDER);

        Notification alreadyRead = new Notification(USERNAME, "Already read", "Desc", EntityType.SHOP);
        alreadyRead.setRead(true);
        notificationRepository.save(alreadyRead);

        List<Notification> result = notificationService.findByUsernameAndIsReadFalseOrderByTimestampDesc(USERNAME);

        assertAll(
                () -> assertEquals(2, result.size(), "Must return only the 2 unread notifications for this user"),
                () -> assertTrue(result.stream().allMatch(n -> !n.isRead()), "All returned notifications must be unread"),
                () -> assertTrue(result.stream().allMatch(n -> USERNAME.equals(n.getUsername())), "Results must belong only to the target user")
        );
    }

    // --- PAGINATION ---

    @Test
    @DisplayName("getUserNotificationsPage: Returns correctly paginated results scoped to the user")
    void testGetUserNotificationsPage_ReturnsPaginatedResults() {
        for (int i = 0; i < 5; i++) {
            notificationService.createAndSendNotification(USERNAME, "Subject " + i, "Desc", EntityType.ORDER);
        }
        notificationService.createAndSendNotification(OTHER_USERNAME, "Other user", "Desc", EntityType.PRODUCT);

        Page<Notification> firstPage = notificationService.getUserNotificationsPage(USERNAME, PageRequest.of(0, 3));

        assertAll(
                () -> assertEquals(5, firstPage.getTotalElements(), "Total must count only this user's notifications"),
                () -> assertEquals(3, firstPage.getContent().size(), "First page must contain 3 elements"),
                () -> assertTrue(firstPage.getContent().stream().allMatch(n -> USERNAME.equals(n.getUsername())))
        );
    }

    @Test
    @DisplayName("getNotificationsByTypePage: Filters results by entity type")
    void testGetNotificationsByTypePage_FiltersByType() {
        notificationService.createAndSendNotification(USERNAME, "Order 1", "Desc", EntityType.ORDER);
        notificationService.createAndSendNotification(USERNAME, "Order 2", "Desc", EntityType.ORDER);
        notificationService.createAndSendNotification(USERNAME, "Product 1", "Desc", EntityType.PRODUCT);

        Page<Notification> orderPage = notificationService.getNotificationsByTypePage(
                USERNAME, EntityType.ORDER, PageRequest.of(0, 10));

        assertAll(
                () -> assertEquals(2, orderPage.getTotalElements(), "Must count only ORDER notifications"),
                () -> assertTrue(orderPage.getContent().stream().allMatch(n -> EntityType.ORDER.equals(n.getType())))
        );
    }

    // --- MARK AS READ ---

    @Test
    @DisplayName("markAsRead: Persists isRead=true in MongoDB when the owner marks it")
    void testMarkAsRead_PersistsChange() {
        notificationService.createAndSendNotification(USERNAME, "Subject", "Desc", EntityType.ORDER);
        Notification saved = notificationRepository.findByUsernameAndIsReadFalseOrderByTimestampDesc(USERNAME).get(0);

        notificationService.markAsRead(saved.getId(), USERNAME);

        Notification dbNotification = notificationRepository.findById(saved.getId()).orElseThrow();
        assertTrue(dbNotification.isRead(), "Notification must be marked as read in the database");
    }

    @Test
    @DisplayName("markAsRead: Throws RuntimeException when the notification does not exist")
    void testMarkAsRead_ThrowsWhenNotFound() {
        assertThrows(RuntimeException.class,
                () -> notificationService.markAsRead("non-existent-id", USERNAME));
    }

    @Test
    @DisplayName("markAsRead: Throws RuntimeException and does not modify the notification when user is not the owner")
    void testMarkAsRead_ThrowsAndKeepsUnread_WhenNotOwner() {
        notificationService.createAndSendNotification(OTHER_USERNAME, "Subject", "Desc", EntityType.ORDER);
        Notification saved = notificationRepository.findByUsernameAndIsReadFalseOrderByTimestampDesc(OTHER_USERNAME).get(0);

        assertThrows(RuntimeException.class,
                () -> notificationService.markAsRead(saved.getId(), USERNAME));

        Notification dbNotification = notificationRepository.findById(saved.getId()).orElseThrow();
        assertFalse(dbNotification.isRead(), "Notification must remain unread after a failed permission check");
    }

    @Test
    @DisplayName("markAsRead: Is a no-op when the notification is already read (does not fail)")
    void testMarkAsRead_NoOp_WhenAlreadyRead() {
        Notification alreadyRead = new Notification(USERNAME, "Already read", "Desc", EntityType.ORDER);
        alreadyRead.setRead(true);
        notificationRepository.save(alreadyRead);

        assertDoesNotThrow(() -> notificationService.markAsRead(alreadyRead.getId(), USERNAME));
    }

    // --- MARK ALL AS READ ---

    @Test
    @DisplayName("markAllAsRead: Marks every unread notification for a user as read in MongoDB")
    void testMarkAllAsRead_PersistsAllChanges() {
        notificationService.createAndSendNotification(USERNAME, "S1", "Desc", EntityType.ORDER);
        notificationService.createAndSendNotification(USERNAME, "S2", "Desc", EntityType.PRODUCT);
        notificationService.createAndSendNotification(USERNAME, "S3", "Desc", EntityType.SHOP);

        notificationService.markAllAsRead(USERNAME);

        List<Notification> stillUnread = notificationRepository
                .findByUsernameAndIsReadFalseOrderByTimestampDesc(USERNAME);
        assertTrue(stillUnread.isEmpty(), "All notifications must be marked as read after bulk update");
    }

    @Test
    @DisplayName("markAllAsRead: Does not affect notifications belonging to other users")
    void testMarkAllAsRead_DoesNotAffectOtherUsers() {
        notificationService.createAndSendNotification(OTHER_USERNAME, "Other's S1", "Desc", EntityType.ORDER);
        notificationService.createAndSendNotification(USERNAME, "My S1", "Desc", EntityType.ORDER);

        notificationService.markAllAsRead(USERNAME);

        List<Notification> otherStillUnread = notificationRepository
                .findByUsernameAndIsReadFalseOrderByTimestampDesc(OTHER_USERNAME);
        assertEquals(1, otherStillUnread.size(), "Other user's notification must remain unread");
    }

    // --- DELETE ---

    @Test
    @DisplayName("deleteNotification: Removes the notification from MongoDB when owner deletes it")
    void testDeleteNotification_RemovesFromDatabase() {
        notificationService.createAndSendNotification(USERNAME, "Subject", "Desc", EntityType.ORDER);
        Notification saved = notificationRepository.findByUsernameAndIsReadFalseOrderByTimestampDesc(USERNAME).get(0);

        notificationService.deleteNotification(saved.getId(), USERNAME);

        assertFalse(notificationRepository.existsById(saved.getId()),
                "Notification must be removed from the database");
    }

    @Test
    @DisplayName("deleteNotification: Does not remove a notification belonging to another user (deleteByIdAndUsername guard)")
    void testDeleteNotification_DoesNotDeleteOtherUsersNotification() {
        notificationService.createAndSendNotification(OTHER_USERNAME, "Other's notif", "Desc", EntityType.ORDER);
        Notification saved = notificationRepository.findByUsernameAndIsReadFalseOrderByTimestampDesc(OTHER_USERNAME).get(0);

        notificationService.deleteNotification(saved.getId(), USERNAME);

        assertTrue(notificationRepository.existsById(saved.getId()),
                "Notification must remain in MongoDB because it belongs to another user");
    }
}
