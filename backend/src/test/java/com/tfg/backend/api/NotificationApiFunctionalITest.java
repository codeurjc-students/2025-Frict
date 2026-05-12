package com.tfg.backend.api;

import com.tfg.backend.dto.EntityType;
import com.tfg.backend.model.Notification;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.NotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class NotificationApiFunctionalITest extends BaseApiFunctionalITest {

    private static final String BASE_URL_NOTIFICATIONS = "/api/v1/notifications";

    @Autowired
    private NotificationRepository notificationRepository;

    private String userCookie;
    private String otherUserCookie;
    private String ownNotificationId;
    private String otherUserNotificationId;

    @BeforeEach
    public void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            User testUser = new User("Test User", "user_notif", "user@notif.com", passwordEncoder.encode("pass"), "USER");
            userRepository.saveAndFlush(testUser);

            User otherUser = new User("Other User", "other_notif", "other@notif.com", passwordEncoder.encode("pass"), "USER");
            userRepository.saveAndFlush(otherUser);
        });

        // Create MongoDB notifications: 2 for testUser (different types) + 1 for otherUser
        Notification n1 = notificationRepository.save(new Notification("user_notif", "Order Confirmed", "Your order has been confirmed", EntityType.ORDER));
        notificationRepository.save(new Notification("user_notif", "New Product", "A product you may like is available", EntityType.PRODUCT));
        Notification n3 = notificationRepository.save(new Notification("other_notif", "Other Notif", "Not for this user", EntityType.USER));

        ownNotificationId = n1.getId();
        otherUserNotificationId = n3.getId();

        userCookie = loginAndGetCookie("user_notif", "pass");
        otherUserCookie = loginAndGetCookie("other_notif", "pass");
    }

    @AfterEach
    public void tearDownMongoDB() {
        notificationRepository.deleteAll();
    }

    // --- GET PAGE ---

    @Test
    public void getNotificationsPage_AsUser_ReturnsScopedPage() {
        given().spec(getSpec(BASE_URL_NOTIFICATIONS, userCookie))
                .queryParam("page", 0).queryParam("size", 10)
                .when().get("/")
                .then().statusCode(200)
                .body("totalItems", equalTo(2))
                .body("items[0].subject", notNullValue());
    }

    @Test
    public void getNotificationsPage_ByType_FiltersResults() {
        given().spec(getSpec(BASE_URL_NOTIFICATIONS, userCookie))
                .queryParam("type", "ORDER")
                .when().get("/")
                .then().statusCode(200)
                .body("totalItems", equalTo(1))
                .body("items[0].subject", equalTo("Order Confirmed"));
    }

    @Test
    public void getNotificationsPage_Unauthenticated_Returns401() {
        given().spec(getSpec(BASE_URL_NOTIFICATIONS, null))
                .when().get("/")
                .then().statusCode(401);
    }

    // --- GET UNREAD ---

    @Test
    public void getUnreadNotifications_AsUser_ReturnsUnreadList() {
        given().spec(getSpec(BASE_URL_NOTIFICATIONS, userCookie))
                .when().get("/unread")
                .then().statusCode(200)
                .body("size()", equalTo(2))
                .body("[0].read", equalTo(false));
    }

    @Test
    public void getUnreadNotifications_Unauthenticated_Returns401() {
        given().spec(getSpec(BASE_URL_NOTIFICATIONS, null))
                .when().get("/unread")
                .then().statusCode(401);
    }

    // --- TEST NOTIFICATION (trigger) ---

    @Test
    public void testNotification_AsUser_CreatesNotification() {
        given().spec(getSpec(BASE_URL_NOTIFICATIONS, userCookie))
                .when().post("/test")
                .then().statusCode(200)
                .body("message", containsString("user_notif"));
    }

    // --- MARK AS READ ---

    @Test
    public void markNotificationAsRead_AsOwner_ReturnsTrue() {
        given().spec(getSpec(BASE_URL_NOTIFICATIONS, userCookie))
                .pathParam("id", ownNotificationId)
                .when().put("/{id}/read")
                .then().statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    public void markNotificationAsRead_AsNonOwner_ReturnsBadRequest() {
        // userCookie attempts to mark a notification that belongs to otherUser
        given().spec(getSpec(BASE_URL_NOTIFICATIONS, userCookie))
                .pathParam("id", otherUserNotificationId)
                .when().put("/{id}/read")
                .then().statusCode(400)
                .body(equalTo("false"));
    }

    // --- MARK ALL AS READ ---

    @Test
    public void markAllAsRead_AsUser_ReturnsTrue() {
        given().spec(getSpec(BASE_URL_NOTIFICATIONS, userCookie))
                .when().put("/read-all")
                .then().statusCode(200)
                .body(equalTo("true"));
    }

    // --- DELETE ---

    @Test
    public void deleteNotification_AsOwner_DeletesAndReturnsTrue() {
        given().spec(getSpec(BASE_URL_NOTIFICATIONS, userCookie))
                .pathParam("id", ownNotificationId)
                .when().delete("/{id}")
                .then().statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    public void deleteNotification_Unauthenticated_Returns401() {
        given().spec(getSpec(BASE_URL_NOTIFICATIONS, null))
                .pathParam("id", ownNotificationId)
                .when().delete("/{id}")
                .then().statusCode(401);
    }
}