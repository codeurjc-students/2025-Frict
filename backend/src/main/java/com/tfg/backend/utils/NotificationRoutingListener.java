package com.tfg.backend.utils;

import com.tfg.backend.model.*;
import com.tfg.backend.service.NotificationService;
import com.tfg.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationRoutingListener {

    private final NotificationService notificationService;
    private final UserService userService; // Necesario para buscar la lista global de ADMINS

    // ==========================================
    // 1. MANEJADOR DE PEDIDOS (ORDER)
    // ==========================================
    @Async
    @EventListener
    public void handleOrderEvent(OrderEvent event) {
        String orderId = event.getOrderId();
        List<String> allAdmins = userService.getAllAdminUsernames();

        if (event.getAction() == EventAction.STATUS_CHANGED) {
            String subject = "Order #" + orderId + " Status Updated";
            String description = "Status changed from " + event.getOldStatus() + " to " + event.getNewStatus();

            log.info("Routing ORDER STATUS notification triggered by: {}", event.getActorRole());

            switch (event.getActorRole()) {
                case "MANAGER":
                    notifySafe(allAdmins, subject, description, NotificationType.ORDER, event.getActorUsername());
                    notifySafe(List.of(event.getDriverUsername()), subject, description, NotificationType.ORDER, event.getActorUsername());
                    break;
                case "ADMIN":
                    notifySafe(allAdmins, subject, description, NotificationType.ORDER, event.getActorUsername());
                    notifySafe(List.of(event.getManagerUsername()), subject, description, NotificationType.ORDER, event.getActorUsername());
                    notifySafe(List.of(event.getDriverUsername()), subject, description, NotificationType.ORDER, event.getActorUsername());
                    break;
                default:
                    notifySafe(allAdmins, subject, description, NotificationType.ORDER, event.getActorUsername());
                    notifySafe(List.of(event.getManagerUsername()), subject, description, NotificationType.ORDER, event.getActorUsername());
                    break;
            }
        }
        else if (event.getAction() == EventAction.ASSIGNED) {
            String subject = "New Order Assigned";
            String description = "Order #" + orderId + " has been assigned to your store.";
            notifySafe(List.of(event.getManagerUsername()), subject, description, NotificationType.ORDER, event.getActorUsername());
            notifySafe(allAdmins, subject, description, NotificationType.ORDER, event.getActorUsername());
        }
    }

    // ==========================================
    // 2. MANEJADOR DE CAMIONES (TRUCK)
    // ==========================================
    @Async
    @EventListener
    public void handleTruckEvent(TruckEvent event) {
        List<String> allAdmins = userService.getAllAdminUsernames();

        if (event.getAction() == EventAction.ASSIGNED) {
            String subject = "Truck Assignment";
            String description = "You have been assigned to Truck " + event.getLicensePlate();

            notifySafe(List.of(event.getDriverUsername()), subject, description, NotificationType.TRUCK, event.getActorUsername());
            notifySafe(allAdmins, subject, description, NotificationType.TRUCK, event.getActorUsername());
        }
        else if (event.getAction() == EventAction.STATUS_CHANGED) {
            String subject = "Truck Alert: " + event.getLicensePlate();
            String description = "Truck status has been updated.";

            notifySafe(List.of(event.getDriverUsername()), subject, description, NotificationType.TRUCK, event.getActorUsername());
            notifySafe(List.of(event.getTargetStoreManagerUsername()), subject, description, NotificationType.TRUCK, event.getActorUsername());
        }
    }

    // ==========================================
    // 3. MANEJADOR DE TIENDAS (STORE)
    // ==========================================
    @Async
    @EventListener
    public void handleStoreEvent(ShopEvent event) {
        if (event.getAction() == EventAction.ASSIGNED) {
            String subject = "Store Management Assignment";
            String description = "You are now the manager of " + event.getShopName();

            notifySafe(List.of(event.getNewManagerUsername()), subject, description, NotificationType.SHOP, event.getActorUsername());
        }
    }

    // ==========================================
    // MÉTODO AUXILIAR DE ENVÍO SEGURO
    // ==========================================
    private void notifySafe(List<String> recipients, String subject, String description, NotificationType type, String excludedUser) {
        if (recipients == null || recipients.isEmpty()) return;

        for (String recipient : recipients) {
            if (recipient != null && !recipient.isBlank() && !recipient.equals(excludedUser)) {
                notificationService.createAndSendNotification(recipient, subject, description, type);
            }
        }
    }
}
