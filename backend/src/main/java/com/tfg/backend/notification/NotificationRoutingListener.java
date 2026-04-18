package com.tfg.backend.notification;

import com.tfg.backend.service.NotificationService;
import com.tfg.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
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

        Set<String> involvedUsers = new HashSet<>(userService.getAllAdminUsernames());
        if (event.getManagerUsername() != null) involvedUsers.add(event.getManagerUsername());
        if (event.getDriverUsername() != null) involvedUsers.add(event.getDriverUsername());
        if (event.getCustomerUsername() != null) involvedUsers.add(event.getCustomerUsername());

        String subject = "";
        String description = "";

        switch (event.getAction()){
            case CREATED -> {
                subject = "Nuevo pedido recibido";
                description = "El usuario " + getSafeActorUsername() + " ha realizado un nuevo pedido.";
            }
            case ASSIGNED -> {
                subject = "Nuevo pedido asignado";
                description = "El camión asignado al pedido #" + orderId + " ha cambiado.";
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

        if (!subject.isEmpty()) {
            notifySafe(involvedUsers, subject, description, NotificationType.ORDER);
        }
    }

    // ==========================================
    // 2. TRUCK NOTIFICATIONS HANDLER
    // ==========================================
    @Async
    @EventListener
    public void handleTruckEvent(TruckEvent event) {
        String truckId = event.getTruckId();

        Set<String> involvedUsers = new HashSet<>(userService.getAllAdminUsernames());
        if (event.getDriverUsername() != null) involvedUsers.add(event.getDriverUsername());
        if (event.getManagerUsername() != null) involvedUsers.add(event.getManagerUsername());

        String subject = "";
        String description = "";

        switch (event.getAction()){
            case CREATED -> {
                subject = "Nuevo camión registrado";
                description = "El administrador " + getSafeActorUsername() + " ha registrado un nuevo camión.";
            }
            case ASSIGNED -> {
                subject = "Cambio de conductor del camión " + truckId;
                description = "El conductor asociado al camión " + truckId + " ha cambiado.";
            }
            case STATUS_CHANGED -> {
                subject = "Estado de camión " + truckId + " actualizado";
                description = "El estado ha pasado de " + event.getOldStatus() + " a " + event.getNewStatus() + ".";
            }
            case NEW_COMMENT -> {
                subject = "Nuevo comentario en camión " + truckId;
                description = "El camión " + truckId + " tiene una nueva actualización.";
            }
            case UPDATED -> {
                subject = "Actualización del camión " + truckId;
                description = "Los datos del camión " + truckId + " han cambiado.";
            }
            case DELETED -> {
                subject = "Camión " + truckId + " eliminado";
                description = "El camión " + truckId + " ha sido eliminado de la flota.";
            }
        }

        if (!subject.isEmpty()) {
            notifySafe(involvedUsers, subject, description, NotificationType.TRUCK);
        }
    }

    // ==========================================
    // 3. SHOP NOTIFICATIONS HANDLER
    // ==========================================
    @Async
    @EventListener
    public void handleShopEvent(ShopEvent event) {
        String shopId = event.getShopId();

        Set<String> involvedUsers = new HashSet<>(userService.getAllAdminUsernames());
        if (event.getManagerUsername() != null) involvedUsers.add(event.getManagerUsername());
        if (event.getDriverUsernames() != null && !event.getDriverUsernames().isEmpty()) {
            involvedUsers.addAll(event.getDriverUsernames());
        }

        // Protegemos el parseo a Long comprobando que el ID no sea null (necesario para updates/deletes)
        if (event.isNotifyCustomers() && shopId != null) {
            try {
                List<String> subscribedCustomers = userService.getUsernamesBySelectedShop(Long.valueOf(shopId));
                if (subscribedCustomers != null && !subscribedCustomers.isEmpty()) {
                    involvedUsers.addAll(subscribedCustomers);
                }
            } catch (NumberFormatException e) {
                log.error("Error parsing shopId {} to Long while searching for customers.", shopId, e);
            }
        }

        String subject = "";
        String description = "";

        switch (event.getAction()){
            case CREATED -> {
                subject = "Nueva tienda registrada";
                description = "El administrador " + getSafeActorUsername() + " ha registrado una nueva tienda.";
            }
            case ASSIGNED -> {
                subject = "Nuevo camión asignado a tienda #" + shopId;
                description = "Un nuevo camión está disponible para el reparto en la tienda "+ shopId + ".";
            }
            case STATUS_CHANGED -> {
                subject = "Actualización de stock de la tienda #" + shopId;
                description = "Se ha repuesto o activado el stock de la tienda #" + shopId + ".";
            }
            case UPDATED -> {
                subject = "Actualización de la tienda #" + shopId;
                description = "Los datos de configuración de la tienda #" + shopId + " han cambiado.";
            }
            case DELETED -> {
                subject = "Tienda #" + shopId + " eliminada";
                description = "La tienda #" + shopId + " ha sido eliminada del sistema.";
            }
        }

        if (!subject.isEmpty()) {
            notifySafe(involvedUsers, subject, description, NotificationType.SHOP);
        }
    }


    // ==========================================
    // 4. PRODUCT NOTIFICATIONS HANDLER
    // ==========================================
    @Async
    @EventListener
    public void handleProductEvent(ProductEvent event) {
        String productId = event.getProductId();
        Long pIdLong;

        try {
            pIdLong = Long.valueOf(productId);
        } catch (NumberFormatException e) {
            log.error("Error parsing productId {} to Long.", productId, e);
            return; // Cannot proceed without a valid ID
        }

        // 1. Involved audience: admins, managers from the event, users as favourite, and users with the product in cart.
        Set<String> involvedUsers = new HashSet<>(userService.getAllAdminUsernames());

        // Add managers calculated in the service
        if (event.getManagerUsernames() != null && !event.getManagerUsernames().isEmpty()) {
            involvedUsers.addAll(event.getManagerUsernames());
        }

        // Add users who have it as favorite
        List<String> favoritedCustomers = userService.getUsernamesByFavoritedProduct(pIdLong);
        if (favoritedCustomers != null) involvedUsers.addAll(favoritedCustomers);

        // Add users who have it in their cart (OrderItems)
        List<String> cartCustomers = userService.getUsernamesWithProductInCart(pIdLong);
        if (cartCustomers != null) involvedUsers.addAll(cartCustomers);

        // 2. Build message content
        String subject = "";
        String description = "";

        switch (event.getAction()) {
            case CREATED -> {
                subject = "Nuevo producto registrado";
                description = "El administrador " + getSafeActorUsername() + " ha añadido un nuevo producto al catálogo.";
            }
            case STATUS_CHANGED -> {
                subject = "Activación global del producto #" + productId;
                description = "La disponibilidad global del producto #" + productId + " ha cambiado.";
            }
            case UPDATED -> {
                subject = "Actualización del producto #" + productId;
                description = "El producto #" + productId + " ha sido actualizado.";
            }
            case DELETED -> {
                subject = "Producto #" + productId + " retirado";
                description = "El producto #" + productId + " ha sido eliminado definitivamente del catálogo.";
            }
        }

        // 3. Dispatch notifications
        if (!subject.isEmpty()) {
            notifySafe(involvedUsers, subject, description, NotificationType.PRODUCT);
        }
    }

    // ==========================================
    // 5. USER NOTIFICATIONS HANDLER
    // ==========================================
    @Async
    @EventListener
    public void handleUserEvent(UserEvent event) {
        String targetUsername = event.getTargetUsername(); // The user affected by the action

        // 1. Base audience: All administrators
        Set<String> involvedUsers = new HashSet<>(userService.getAllAdminUsernames());

        // 2. Build the message content
        String subject = "";
        String description = "";

        switch (event.getAction()) {
            case CREATED -> {
                subject = "Nuevo usuario registrado";
                description = "El usuario " + targetUsername + " se ha registrado en el sistema.";
            }
            case STATUS_CHANGED -> {
                String loggedUserRole = userService.getLoggedUserRole();
                if (loggedUserRole != null){
                    if (loggedUserRole.equals("USER")){
                        subject = "Cuenta de usuario " + targetUsername + " eliminada por el usuario";
                        description = "La cuenta vinculada al usuario " + targetUsername + " ha sido anonimizada.";
                    }
                    else {
                        subject = "Cuenta de usuario " + targetUsername + " anonimizada";
                        description = "La cuenta vinculada al usuario " + targetUsername + " ha sido anonimizada.";
                    }
                }
            }

            case NEW_COMMENT -> { //User ban notifications
                subject = "Usuario " + targetUsername + " baneado";
                description = "La cuenta vinculada al usuario " + targetUsername + " ha sido baneada.";
            }

            case ASSIGNED -> {
                String loggedUserRole = userService.getLoggedUserRole();
                if (loggedUserRole != null){
                    if (loggedUserRole.equals("USER")){
                        subject = "Cambio de asignación de tienda";
                        description = "El usuario " + targetUsername + " ha cambiado su tienda seleccionada";
                    }
                    else {
                        involvedUsers.add(targetUsername);
                        subject = "Cambio de asignación de manager";
                        description = "Las tiendas asignadas al manager " + targetUsername + " han cambiado.";
                    }
                }
            }

            case UPDATED -> {
                subject = "Contraseña de usuario " + targetUsername + " actualizada";
                description = "La clave de acceso del usuario interno " + targetUsername + " ha sido modificada";
            }

            case DELETED -> {
                subject = "Usuario eliminado";
                description = "La cuenta del usuario " + targetUsername + " ha sido eliminada del sistema.";
            }
        }

        // 3. Dispatch notifications
        if (!subject.isEmpty()) {
            notifySafe(involvedUsers, subject, description, NotificationType.USER);
        }
    }

    // ==========================================
    // UTILITY METHODS (DRY)
    // ==========================================
    private void notifySafe(Iterable<String> recipients, String subject, String description, NotificationType type) {
        if (recipients == null) return;
        String excludedUser = userService.getLoggedUserUsername();

        for (String recipient : recipients) {
            if (recipient != null && !recipient.isBlank() && !recipient.equals(excludedUser)) {
                log.info("Sending notification to: {}", recipient);
                notificationService.createAndSendNotification(recipient, subject, description, type);
            }
        }
    }

    private String getSafeActorUsername() {
        String username = userService.getLoggedUserUsername();
        return (username != null && !username.isBlank()) ? username : "Sistema";
    }
}