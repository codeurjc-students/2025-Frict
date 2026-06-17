package com.tfg.backend.event;

import com.tfg.backend.dto.EntityType;
import com.tfg.backend.dto.EventAction;
import com.tfg.backend.model.*;
import com.tfg.backend.service.*;
import com.tfg.backend.service.NotificationMessageBuilder.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationRoutingListener {

    private final NotificationService notificationService;
    private final NotificationMessageBuilder messageBuilder;
    private final UserService userService;
    private final OrderService orderService;
    private final TruckService truckService;
    private final ShopService shopService;
    private final ProductService productService;
    private final ReviewService reviewService;

    // ==========================================
    // 1. ORDER NOTIFICATIONS HANDLER
    // ==========================================
    @Async
    @TransactionalEventListener
    public void handleOrderEvent(OrderEvent event) {
        Order order = parseLong(event.getOrderId()).flatMap(orderService::findById).orElse(null);

        OrderMessageContext ctx = new OrderMessageContext(
                order != null ? order.getReferenceCode() : null,
                event.getOldStatus(),
                event.getNewStatus(),
                getSafeActorUsername(),
                event.getCustomerUsername() != null ? event.getCustomerUsername() : (order != null && order.getUser() != null ? order.getUser().getUsername() : null),
                order != null && order.getAssignedTruck() != null ? order.getAssignedTruck().getPlateNumber() : null,
                order != null && order.getAssignedShop() != null ? order.getAssignedShop().getName() : null
        );

        Set<String> notified = new HashSet<>();
        notifyForRole(userService.getAllAdminUsernames(), NotificationRole.ADMIN, EntityType.ORDER,
                () -> messageBuilder.buildForOrder(event.getAction(), NotificationRole.ADMIN, ctx), notified);
        notifyForRole(single(event.getManagerUsername()), NotificationRole.MANAGER, EntityType.ORDER,
                () -> messageBuilder.buildForOrder(event.getAction(), NotificationRole.MANAGER, ctx), notified);
        notifyForRole(single(event.getDriverUsername()), NotificationRole.DRIVER, EntityType.ORDER,
                () -> messageBuilder.buildForOrder(event.getAction(), NotificationRole.DRIVER, ctx), notified);
        notifyForRole(single(event.getCustomerUsername()), NotificationRole.CUSTOMER, EntityType.ORDER,
                () -> messageBuilder.buildForOrder(event.getAction(), NotificationRole.CUSTOMER, ctx), notified);
    }

    // ==========================================
    // 2. TRUCK NOTIFICATIONS HANDLER
    // ==========================================
    @Async
    @TransactionalEventListener
    public void handleTruckEvent(TruckEvent event) {
        Truck truck = parseLong(event.getTruckId()).flatMap(truckService::findById).orElse(null);

        TruckMessageContext ctx = new TruckMessageContext(
                truck != null ? truck.getPlateNumber() : null,
                truck != null ? truck.getReferenceCode() : null,
                event.getOldStatus(),
                event.getNewStatus(),
                getSafeActorUsername(),
                event.getDriverUsername(),
                truck != null && truck.getAssignedDriver() != null ? truck.getAssignedDriver().getUsername() : null,
                truck != null && truck.getAssignedShop() != null ? truck.getAssignedShop().getName() : null
        );

        Set<String> notified = new HashSet<>();
        notifyForRole(userService.getAllAdminUsernames(), NotificationRole.ADMIN, EntityType.TRUCK,
                () -> messageBuilder.buildForTruck(event.getAction(), NotificationRole.ADMIN, ctx), notified);
        notifyForRole(single(event.getManagerUsername()), NotificationRole.MANAGER, EntityType.TRUCK,
                () -> messageBuilder.buildForTruck(event.getAction(), NotificationRole.MANAGER, ctx), notified);
        notifyForRole(single(event.getDriverUsername()), NotificationRole.DRIVER, EntityType.TRUCK,
                () -> messageBuilder.buildForTruck(event.getAction(), NotificationRole.DRIVER, ctx), notified);
    }

    // ==========================================
    // 3. SHOP NOTIFICATIONS HANDLER
    // ==========================================
    @Async
    @TransactionalEventListener
    public void handleShopEvent(ShopEvent event) {
        Shop shop = parseLong(event.getShopId()).flatMap(shopService::findById).orElse(null);

        ShopMessageContext ctx = new ShopMessageContext(
                shop != null ? shop.getName() : null,
                shop != null ? shop.getReferenceCode() : null,
                null,
                null,
                getSafeActorUsername(),
                event.getManagerUsername(),
                null
        );

        Set<String> notified = new HashSet<>();
        notifyForRole(userService.getAllAdminUsernames(), NotificationRole.ADMIN, EntityType.SHOP,
                () -> messageBuilder.buildForShop(event.getAction(), NotificationRole.ADMIN, ctx), notified);
        notifyForRole(single(event.getManagerUsername()), NotificationRole.MANAGER, EntityType.SHOP,
                () -> messageBuilder.buildForShop(event.getAction(), NotificationRole.MANAGER, ctx), notified);
        notifyForRole(event.getDriverUsernames(), NotificationRole.DRIVER, EntityType.SHOP,
                () -> messageBuilder.buildForShop(event.getAction(), NotificationRole.DRIVER, ctx), notified);

        if (event.isNotifyCustomers() && shop != null) {
            List<String> subscribedCustomers = userService.getUsernamesBySelectedShop(shop.getId());
            notifyForRole(subscribedCustomers, NotificationRole.CUSTOMER, EntityType.SHOP,
                    () -> messageBuilder.buildForShop(event.getAction(), NotificationRole.CUSTOMER, ctx), notified);
        }
    }

    // ==========================================
    // 4. PRODUCT NOTIFICATIONS HANDLER
    // ==========================================
    @Async
    @TransactionalEventListener
    public void handleProductEvent(ProductEvent event) {
        Optional<Long> pIdOpt = parseLong(event.getProductId());
        if (pIdOpt.isEmpty()) {
            log.error("Error parsing productId {} to Long.", event.getProductId());
            return;
        }
        Long pIdLong = pIdOpt.get();
        Product product = productService.findById(pIdLong).orElse(null);

        ProductMessageContext ctx = new ProductMessageContext(
                product != null ? product.getName() : null,
                product != null ? product.getReferenceCode() : null,
                null,
                null,
                getSafeActorUsername(),
                null
        );

        Set<String> notified = new HashSet<>();
        notifyForRole(userService.getAllAdminUsernames(), NotificationRole.ADMIN, EntityType.PRODUCT,
                () -> messageBuilder.buildForProduct(event.getAction(), NotificationRole.ADMIN, ctx), notified);
        notifyForRole(event.getManagerUsernames(), NotificationRole.MANAGER, EntityType.PRODUCT,
                () -> messageBuilder.buildForProduct(event.getAction(), NotificationRole.MANAGER, ctx), notified);

        Set<String> customers = new HashSet<>();
        List<String> favoritedCustomers = userService.getUsernamesByFavoritedProduct(pIdLong);
        if (favoritedCustomers != null) customers.addAll(favoritedCustomers);
        List<String> cartCustomers = userService.getUsernamesWithProductInCart(pIdLong);
        if (cartCustomers != null) customers.addAll(cartCustomers);
        notifyForRole(customers, NotificationRole.CUSTOMER, EntityType.PRODUCT,
                () -> messageBuilder.buildForProduct(event.getAction(), NotificationRole.CUSTOMER, ctx), notified);
    }

    // ==========================================
    // 5. USER NOTIFICATIONS HANDLER
    // ==========================================
    @Async
    @TransactionalEventListener
    public void handleUserEvent(UserEvent event) {
        String targetUsername = event.getTargetUsername();

        User targetUser = userService.findByUsername(targetUsername).orElse(null);
        String targetRole = targetUser != null ? targetUser.getRole() : null;
        String targetDisplayName = targetUser != null ? targetUser.getName() : null;
        String shopName = targetUser != null && targetUser.getSelectedShop() != null ? targetUser.getSelectedShop().getName() : null;

        UserMessageContext ctx = new UserMessageContext(
                targetUsername,
                targetDisplayName,
                targetRole,
                null,
                null,
                getSafeActorUsername(),
                shopName,
                null
        );

        EventAction action = event.getAction();
        Set<String> notified = new HashSet<>();

        notifyForRole(userService.getAllAdminUsernames(), NotificationRole.ADMIN, EntityType.USER,
                () -> messageBuilder.buildForUser(action, NotificationRole.ADMIN, ctx), notified);

        // For ASSIGNED action where target is a MANAGER, also notify the target user
        if (action == EventAction.ASSIGNED) {
            String loggedUserRole = userService.getLoggedUserRole();
            if (loggedUserRole != null && !"USER".equals(loggedUserRole)) {
                notifyForRole(single(targetUsername), NotificationRole.MANAGER, EntityType.USER,
                        () -> messageBuilder.buildForUser(action, NotificationRole.MANAGER, ctx), notified);
            }
        }

        // For NEW_COMMENT (ban), also notify the managers of the target's selected shop
        if (action == EventAction.NEW_COMMENT && targetUser != null && targetUser.getSelectedShop() != null) {
            User assignedManager = targetUser.getSelectedShop().getAssignedManager();
            if (assignedManager != null) {
                notifyForRole(single(assignedManager.getUsername()), NotificationRole.MANAGER, EntityType.USER,
                        () -> messageBuilder.buildForUser(action, NotificationRole.MANAGER, ctx), notified);
            }
        }
    }

    // ==========================================
    // 6. REVIEW NOTIFICATIONS HANDLER
    // ==========================================
    @Async
    @TransactionalEventListener
    public void handleReviewEvent(ReviewEvent event) {
        if (event.getProductId() == null) {
            log.error("ReviewEvent missing productId. Cannot determine audience.");
            return;
        }
        Optional<Long> pIdOpt = parseLong(event.getProductId());
        if (pIdOpt.isEmpty()) {
            log.error("Error parsing productId {} to Long.", event.getProductId());
            return;
        }
        Long pIdLong = pIdOpt.get();
        Product product = productService.findById(pIdLong).orElse(null);

        Review review = null;
        if (event.getReviewId() != null) {
            review = parseLong(event.getReviewId()).flatMap(reviewService::findById).orElse(null);
        }

        ReviewMessageContext ctx = new ReviewMessageContext(
                review != null && review.getUser() != null ? review.getUser().getUsername() : getSafeActorUsername(),
                product != null ? product.getName() : null,
                product != null ? product.getReferenceCode() : null,
                review != null ? review.getRating() : null,
                getSafeActorUsername()
        );

        Set<String> notified = new HashSet<>();
        notifyForRole(userService.getAllAdminUsernames(), NotificationRole.ADMIN, EntityType.REVIEW,
                () -> messageBuilder.buildForReview(event.getAction(), NotificationRole.ADMIN, ctx), notified);
        notifyForRole(event.getManagerUsernames(), NotificationRole.MANAGER, EntityType.REVIEW,
                () -> messageBuilder.buildForReview(event.getAction(), NotificationRole.MANAGER, ctx), notified);

        Set<String> customers = new HashSet<>();
        List<String> favoritedCustomers = userService.getUsernamesByFavoritedProduct(pIdLong);
        if (favoritedCustomers != null) customers.addAll(favoritedCustomers);
        List<String> cartCustomers = userService.getUsernamesWithProductInCart(pIdLong);
        if (cartCustomers != null) customers.addAll(cartCustomers);
        notifyForRole(customers, NotificationRole.CUSTOMER, EntityType.REVIEW,
                () -> messageBuilder.buildForReview(event.getAction(), NotificationRole.CUSTOMER, ctx), notified);
    }

    // ==========================================
    // 7. SHOP STOCK NOTIFICATIONS HANDLER
    // ==========================================
    @Async
    @TransactionalEventListener
    public void handleShopStockEvent(ShopStockEvent event) {
        ShopStockMessageContext ctx = new ShopStockMessageContext(
                event.getShopName(),
                event.getShopRefCode(),
                event.getProductName(),
                event.getProductRefCode(),
                event.getOldUnits(),
                event.getNewUnits(),
                event.getThreshold(),
                getSafeActorUsername()
        );

        Set<String> notified = new HashSet<>();

        switch (event.getAction()) {
            case RESTOCKED -> {
                notifyForRole(userService.getAllAdminUsernames(), NotificationRole.ADMIN, EntityType.SHOP,
                        () -> messageBuilder.buildForStockRestocked(NotificationRole.ADMIN, ctx), notified);
                if (event.getShopId() != null && event.getProductId() != null) {
                    List<String> targetCustomers = userService.getUsernamesByFavoritedProductAndSelectedShop(event.getShopId(), event.getProductId());
                    notifyForRole(targetCustomers, NotificationRole.CUSTOMER, EntityType.SHOP,
                            () -> messageBuilder.buildForStockRestocked(NotificationRole.CUSTOMER, ctx), notified);
                }
            }
            case LOW_STOCK -> {
                notifyForRole(single(event.getManagerUsername()), NotificationRole.MANAGER, EntityType.SHOP,
                        () -> messageBuilder.buildForStockLow(NotificationRole.MANAGER, ctx), notified);
                if (event.getShopId() != null && event.getProductId() != null) {
                    List<String> targetCustomers = userService.getUsernamesBySelectedShopAndProductInFavoritesOrCart(event.getShopId(), event.getProductId());
                    notifyForRole(targetCustomers, NotificationRole.CUSTOMER, EntityType.SHOP,
                            () -> messageBuilder.buildForStockLow(NotificationRole.CUSTOMER, ctx), notified);
                }
            }
        }
    }

    // ==========================================
    // UTILITY METHODS
    // ==========================================
    private void notifyForRole(Collection<String> recipients,
                               NotificationRole role,
                               EntityType type,
                               java.util.function.Supplier<NotificationMessage> messageSupplier,
                               Set<String> alreadyNotified) {
        if (recipients == null || recipients.isEmpty()) return;
        String excludedUser = userService.getLoggedUserUsername();

        NotificationMessage message = null;
        for (String recipient : recipients) {
            if (recipient == null || recipient.isBlank()) continue;
            if (recipient.equals(excludedUser)) continue;
            if (!alreadyNotified.add(recipient)) continue;

            if (message == null) message = messageSupplier.get();

            log.info("Sending {} notification to: {}", role, recipient);
            notificationService.createAndSendNotification(recipient, message.subject(), message.description(), type);
        }
    }

    private List<String> single(String username) {
        return username == null || username.isBlank() ? List.of() : List.of(username);
    }

    private Optional<Long> parseLong(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return Optional.of(Long.valueOf(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private String getSafeActorUsername() {
        String username = userService.getLoggedUserUsername();
        return (username != null && !username.isBlank()) ? username : "Sistema";
    }
}
