package com.tfg.backend.utils;

import com.tfg.backend.model.*;
import com.tfg.backend.service.NotificationService;
import com.tfg.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationRoutingListener {

    private final NotificationService notificationService;
    private final UserService userService;

    // ==========================================
    // 1. ORDER NOTIFICATIONS HANDLER
    // ==========================================
    @Async
    @EventListener
    public void handleOrderEvent(OrderEvent event) {
        String orderId = event.getOrderId();

        // Set involved audience: customer, manager, driver (if any) and all admins
        Set<String> involvedUsers = new HashSet<>(userService.getAllAdminUsernames());
        if (event.getManagerUsername() != null) involvedUsers.add(event.getManagerUsername());
        if (event.getDriverUsername() != null) involvedUsers.add(event.getDriverUsername());
        if (event.getCustomerUsername() != null) involvedUsers.add(event.getCustomerUsername());

        // Set message
        String subject = "";
        String description = "";

        switch (event.getAction()){
            case CREATED -> {
                subject = "Nuevo pedido recibido";
                description = "El usuario " + event.getActorUsername() + " ha realizado el pedido #" + orderId + ".";
            }
            case ASSIGNED -> {
                subject = "Nuevo pedido asignado";
                description = "El pedido #" + orderId + " ha sido asignado a tu camión.";
            }
            case STATUS_CHANGED -> {
                subject = "Estado de pedido #" + orderId + " actualizado";
                description = "El estado ha pasado de " + event.getOldStatus() + " a " + event.getNewStatus() + ".";
            }
            case NEW_COMMENT -> {
                subject = "Nuevo comentario en pedido #" + orderId;
                description = "El pedido #" + orderId + " tiene una nueva actualización.";
            }
            case DELETED -> {
                subject = "Pedido histórico #" + orderId + " eliminado";
                description = "El pedido ha sido eliminado del historial de compras.";
            }
        }

        // Notify users
        if (!subject.isEmpty()) {
            notifySafe(involvedUsers, subject, description, NotificationType.ORDER, event.getActorUsername());
        }
    }

    // ==========================================
    // 2. TRUCK NOTIFICATIONS HANDLER
    // ==========================================
    @Async
    @EventListener
    public void handleTruckEvent(TruckEvent event) {
        Set<String> involvedUsers = new HashSet<>(userService.getAllAdminUsernames());
        if (event.getDriverUsername() != null) involvedUsers.add(event.getDriverUsername());
        if (event.getTargetStoreManagerUsername() != null) involvedUsers.add(event.getTargetStoreManagerUsername());

        String subject = "";
        String description = "";

        if (event.getAction() == EventAction.ASSIGNED) {
            subject = "Truck Assignment";
            description = "You have been assigned to Truck " + event.getLicensePlate();
        }
        else if (event.getAction() == EventAction.STATUS_CHANGED) {
            subject = "Truck Alert: " + event.getLicensePlate();
            description = "Truck status has been updated.";
        }

        if (!subject.isEmpty()) {
            notifySafe(involvedUsers, subject, description, NotificationType.TRUCK, event.getActorUsername());
        }
    }

    // ==========================================
    // 3. SHOPS NOTIFICATIONS HANDLER
    // ==========================================
    @Async
    @EventListener
    public void handleStoreEvent(ShopEvent event) {
        Set<String> involvedUsers = new HashSet<>(userService.getAllAdminUsernames());
        if (event.getNewManagerUsername() != null) involvedUsers.add(event.getNewManagerUsername());

        if (event.getAction() == EventAction.ASSIGNED) {
            String subject = "Store Management Assignment";
            String description = "You are now the manager of " + event.getShopName();
            notifySafe(involvedUsers, subject, description, NotificationType.SHOP, event.getActorUsername());
        }
    }

    // ==========================================
    // SAFE NOTIFICATION SENDING METHOD
    // ==========================================
    private void notifySafe(Iterable<String> recipients, String subject, String description, NotificationType type, String excludedUser) {
        if (recipients == null) return;

        for (String recipient : recipients) {
            // If the recipient is the excluded user (actor), ignore them
            if (recipient != null && !recipient.isBlank() && !recipient.equals(excludedUser)) {
                notificationService.createAndSendNotification(recipient, subject, description, type);
            }
        }
    }
}