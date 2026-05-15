package com.tfg.backend.service;

import com.tfg.backend.dto.EventAction;
import com.tfg.backend.event.ShopStockEvent;
import com.tfg.backend.model.NotificationRole;
import org.springframework.stereotype.Service;

@Service
public class NotificationMessageBuilder {

    public record NotificationMessage(String subject, String description) {}

    public record OrderMessageContext(
            String orderRefCode,
            String oldStatus,
            String newStatus,
            String actorUsername,
            String customerUsername,
            String truckPlate,
            String shopName
    ) {}

    public record TruckMessageContext(
            String truckPlate,
            String truckRefCode,
            String oldStatus,
            String newStatus,
            String actorUsername,
            String newDriverUsername,
            String oldDriverUsername,
            String shopName
    ) {}

    public record ShopMessageContext(
            String shopName,
            String shopRefCode,
            String oldStatus,
            String newStatus,
            String actorUsername,
            String managerUsername,
            String truckPlate
    ) {}

    public record ProductMessageContext(
            String productName,
            String productRefCode,
            String oldStatus,
            String newStatus,
            String actorUsername,
            String shopName
    ) {}

    public record UserMessageContext(
            String targetUsername,
            String targetDisplayName,
            String targetRole,
            String oldStatus,
            String newStatus,
            String actorUsername,
            String shopName,
            String truckPlate
    ) {}

    public record ReviewMessageContext(
            String reviewerUsername,
            String productName,
            String productRefCode,
            Integer rating,
            String actorUsername
    ) {}

    public record ShopStockMessageContext(
            String shopName,
            String shopRefCode,
            String productName,
            String productRefCode,
            int oldUnits,
            int newUnits,
            int threshold,
            String actorUsername
    ) {}

    public NotificationMessage buildForOrder(EventAction action, NotificationRole role, OrderMessageContext ctx) {
        String ref = display(ctx.orderRefCode(), "el pedido");
        String actor = display(ctx.actorUsername(), "el sistema");
        String customer = display(ctx.customerUsername(), "un cliente");
        String shop = display(ctx.shopName(), "tu tienda");
        String truck = display(ctx.truckPlate(), "un camión");
        String fromTo = "de " + display(ctx.oldStatus(), "—") + " a " + display(ctx.newStatus(), "—");

        return switch (action) {
            case CREATED -> switch (role) {
                case ADMIN -> new NotificationMessage(
                        "Nuevo pedido " + ref + " registrado",
                        "El cliente " + customer + " ha realizado el pedido " + ref + ".");
                case MANAGER -> new NotificationMessage(
                        "Nuevo pedido " + ref + " en " + shop,
                        "El cliente " + customer + " ha realizado el pedido " + ref + " en tu tienda " + shop + ".");
                case DRIVER -> new NotificationMessage(
                        "Nuevo pedido " + ref + " pendiente de asignación",
                        "Se ha registrado el pedido " + ref + " en la tienda " + shop + ".");
                case CUSTOMER -> new NotificationMessage(
                        "Tu pedido " + ref + " se ha registrado",
                        "Hemos recibido tu pedido " + ref + " en la tienda " + shop + ".");
            };
            case ASSIGNED -> switch (role) {
                case ADMIN, MANAGER -> new NotificationMessage(
                        "Cambio de camión en pedido " + ref,
                        "El pedido " + ref + " ahora lo lleva el camión " + truck + ".");
                case DRIVER -> new NotificationMessage(
                        "Se te ha asignado el pedido " + ref,
                        "Llevarás el pedido " + ref + " en el camión " + truck + ".");
                case CUSTOMER -> new NotificationMessage(
                        "Tu pedido " + ref + " ya tiene camión asignado",
                        "El pedido " + ref + " lo entregará el camión " + truck + ".");
            };
            case STATUS_CHANGED -> switch (role) {
                case ADMIN -> new NotificationMessage(
                        "Pedido " + ref + " cambió de estado",
                        "El pedido " + ref + " (cliente " + customer + ") ha pasado " + fromTo + ".");
                case MANAGER -> new NotificationMessage(
                        "Pedido " + ref + " de tu tienda actualizado",
                        "El pedido " + ref + " de la tienda " + shop + " ha pasado " + fromTo + ".");
                case DRIVER -> new NotificationMessage(
                        "Pedido " + ref + " actualizado",
                        "El pedido " + ref + " que llevas en el camión " + truck + " ha pasado " + fromTo + ".");
                case CUSTOMER -> new NotificationMessage(
                        "Estado de tu pedido " + ref + " actualizado",
                        "Tu pedido " + ref + " ha pasado " + fromTo + ".");
            };
            case NEW_COMMENT -> switch (role) {
                case CUSTOMER -> new NotificationMessage(
                        "Nueva actualización de tu pedido " + ref,
                        "Hay una nueva nota sobre tu pedido " + ref + ".");
                case DRIVER -> new NotificationMessage(
                        "Nueva nota en pedido " + ref,
                        "El pedido " + ref + " que llevas tiene una nueva actualización.");
                default -> new NotificationMessage(
                        "Nuevo comentario en pedido " + ref,
                        "El pedido " + ref + " tiene una nueva actualización registrada por " + actor + ".");
            };
            case DELETED -> switch (role) {
                case CUSTOMER -> new NotificationMessage(
                        "Tu pedido " + ref + " se ha eliminado del historial",
                        "El pedido " + ref + " ya no figura en tu historial de compras.");
                default -> new NotificationMessage(
                        "Pedido " + ref + " eliminado",
                        "El pedido " + ref + " ha sido eliminado del historial.");
            };
            default -> defaultMessage("Pedido " + ref, action);
        };
    }

    public NotificationMessage buildForTruck(EventAction action, NotificationRole role, TruckMessageContext ctx) {
        String plate = display(ctx.truckPlate(), display(ctx.truckRefCode(), "el camión"));
        String actor = display(ctx.actorUsername(), "el sistema");
        String newDriver = display(ctx.newDriverUsername(), "—");
        String oldDriver = display(ctx.oldDriverUsername(), "—");
        String shop = display(ctx.shopName(), "su tienda");
        String fromTo = "de " + display(ctx.oldStatus(), "—") + " a " + display(ctx.newStatus(), "—");

        return switch (action) {
            case CREATED -> switch (role) {
                case ADMIN -> new NotificationMessage(
                        "Nuevo camión " + plate + " registrado",
                        "El administrador " + actor + " ha registrado el camión " + plate + ".");
                default -> new NotificationMessage(
                        "Nuevo camión " + plate + " en la flota",
                        "Se ha registrado el camión " + plate + ".");
            };
            case ASSIGNED -> switch (role) {
                case ADMIN -> new NotificationMessage(
                        "Cambio de conductor en camión " + plate,
                        "El conductor del camión " + plate + " pasa de " + oldDriver + " a " + newDriver + ".");
                case MANAGER -> new NotificationMessage(
                        "Camión " + plate + " tiene nuevo conductor",
                        "El camión " + plate + " asignado a tu tienda " + shop + " ahora lo conduce " + newDriver + ".");
                case DRIVER -> new NotificationMessage(
                        "Se te ha asignado el camión " + plate,
                        "A partir de ahora conduces el camión " + plate + " (asignado a " + shop + ").");
                default -> new NotificationMessage(
                        "Cambio de conductor en camión " + plate,
                        "El conductor del camión " + plate + " ha cambiado.");
            };
            case STATUS_CHANGED -> switch (role) {
                case DRIVER -> new NotificationMessage(
                        "Estado de tu camión " + plate + " actualizado",
                        "Tu camión " + plate + " ha pasado " + fromTo + ".");
                case MANAGER -> new NotificationMessage(
                        "Camión " + plate + " de tu tienda actualizado",
                        "El camión " + plate + " de la tienda " + shop + " ha pasado " + fromTo + ".");
                default -> new NotificationMessage(
                        "Estado del camión " + plate + " actualizado",
                        "El camión " + plate + " ha pasado " + fromTo + ".");
            };
            case NEW_COMMENT -> new NotificationMessage(
                    "Nuevo comentario en camión " + plate,
                    "El camión " + plate + " tiene una nueva actualización registrada por " + actor + ".");
            case UPDATED -> new NotificationMessage(
                    "Actualización del camión " + plate,
                    "Los datos del camión " + plate + " han cambiado.");
            case DELETED -> new NotificationMessage(
                    "Camión " + plate + " eliminado",
                    "El camión " + plate + " ha sido eliminado de la flota.");
            default -> defaultMessage("Camión " + plate, action);
        };
    }

    public NotificationMessage buildForShop(EventAction action, NotificationRole role, ShopMessageContext ctx) {
        String name = display(ctx.shopName(), display(ctx.shopRefCode(), "la tienda"));
        String actor = display(ctx.actorUsername(), "el sistema");
        String manager = display(ctx.managerUsername(), "—");
        String truck = display(ctx.truckPlate(), "un camión");

        return switch (action) {
            case CREATED -> new NotificationMessage(
                    "Nueva tienda " + name + " registrada",
                    "El administrador " + actor + " ha registrado la tienda " + name + ".");
            case ASSIGNED -> switch (role) {
                case ADMIN -> new NotificationMessage(
                        "Nuevo camión en tienda " + name,
                        "Se ha asignado el camión " + truck + " a la tienda " + name + ".");
                case MANAGER -> new NotificationMessage(
                        "Tu tienda " + name + " recibe un nuevo camión",
                        "A partir de ahora la tienda " + name + " cuenta con el camión " + truck + ".");
                case DRIVER -> new NotificationMessage(
                        "Tu camión " + truck + " va a la tienda " + name,
                        "El camión " + truck + " se ha asignado a la tienda " + name + ".");
                default -> new NotificationMessage(
                        "Nuevo camión en tienda " + name,
                        "El camión " + truck + " ha sido asignado a la tienda " + name + ".");
            };
            case STATUS_CHANGED -> switch (role) {
                case MANAGER -> new NotificationMessage(
                        "Stock actualizado en tu tienda " + name,
                        "Se ha actualizado el stock de la tienda " + name + ".");
                case CUSTOMER -> new NotificationMessage(
                        "Stock actualizado en " + name,
                        "El stock de la tienda " + name + " ha cambiado.");
                default -> new NotificationMessage(
                        "Stock de tienda " + name + " actualizado",
                        "Se ha actualizado el stock de la tienda " + name + ".");
            };
            case UPDATED -> new NotificationMessage(
                    "Actualización de la tienda " + name,
                    "Los datos de configuración de la tienda " + name + " han cambiado.");
            case DELETED -> new NotificationMessage(
                    "Tienda " + name + " eliminada",
                    "La tienda " + name + " ha sido eliminada del sistema.");
            default -> defaultMessage("Tienda " + name, action);
        };
    }

    public NotificationMessage buildForProduct(EventAction action, NotificationRole role, ProductMessageContext ctx) {
        String name = quote(display(ctx.productName(), display(ctx.productRefCode(), "el producto")));
        String ref = display(ctx.productRefCode(), "");
        String actor = display(ctx.actorUsername(), "el sistema");
        String suffix = ref.isEmpty() ? "" : " (" + ref + ")";

        return switch (action) {
            case CREATED -> switch (role) {
                case CUSTOMER -> new NotificationMessage(
                        "Nuevo producto " + name + " disponible",
                        "Ya puedes encontrar el producto " + name + " en el catálogo.");
                default -> new NotificationMessage(
                        "Nuevo producto " + name + " registrado",
                        "El administrador " + actor + " ha añadido " + name + suffix + " al catálogo.");
            };
            case STATUS_CHANGED -> switch (role) {
                case ADMIN -> new NotificationMessage(
                        "Producto " + name + " ha cambiado de disponibilidad",
                        "El administrador " + actor + " ha cambiado la disponibilidad global del producto " + name + suffix + ".");
                case MANAGER -> new NotificationMessage(
                        "Producto " + name + " disponible en catálogo",
                        "El producto " + name + " ha cambiado su disponibilidad global y puede afectar a tu tienda.");
                case CUSTOMER -> new NotificationMessage(
                        "Disponibilidad de " + name + " actualizada",
                        "La disponibilidad del producto " + name + " ha cambiado.");
                default -> new NotificationMessage(
                        "Producto " + name + " actualizado",
                        "La disponibilidad global del producto " + name + " ha cambiado.");
            };
            case UPDATED -> new NotificationMessage(
                    "Actualización del producto " + name,
                    "El producto " + name + suffix + " ha sido actualizado.");
            case DELETED -> new NotificationMessage(
                    "Producto " + name + " retirado",
                    "El producto " + name + suffix + " ha sido eliminado definitivamente del catálogo.");
            default -> defaultMessage("Producto " + name, action);
        };
    }

    public NotificationMessage buildForUser(EventAction action, NotificationRole role, UserMessageContext ctx) {
        String target = display(ctx.targetUsername(), "un usuario");
        String displayName = display(ctx.targetDisplayName(), target);
        String shop = display(ctx.shopName(), "—");

        return switch (action) {
            case CREATED -> new NotificationMessage(
                    "Nuevo usuario " + target + " registrado",
                    "El usuario " + target + " (" + displayName + ") se ha registrado en el sistema.");
            case STATUS_CHANGED -> switch (role) {
                case ADMIN -> new NotificationMessage(
                        "Cuenta de usuario " + target + " anonimizada",
                        "La cuenta del usuario " + target + " ha sido anonimizada.");
                default -> new NotificationMessage(
                        "Cuenta de usuario " + target + " eliminada",
                        "La cuenta vinculada al usuario " + target + " ha sido anonimizada.");
            };
            case NEW_COMMENT -> switch (role) {
                case ADMIN -> new NotificationMessage(
                        "Usuario " + target + " baneado",
                        "La cuenta del usuario " + target + " (" + displayName + ") ha sido baneada.");
                case MANAGER -> new NotificationMessage(
                        "Cliente de tu tienda baneado",
                        "El cliente " + target + " de tu tienda " + shop + " ha sido baneado.");
                default -> new NotificationMessage(
                        "Usuario " + target + " baneado",
                        "La cuenta del usuario " + target + " ha sido baneada.");
            };
            case ASSIGNED -> switch (role) {
                case ADMIN -> new NotificationMessage(
                        "Cambio de asignación de manager " + target,
                        "Las tiendas asignadas al manager " + target + " han cambiado.");
                case MANAGER -> new NotificationMessage(
                        "Tu asignación de tiendas ha cambiado",
                        "Las tiendas que tienes asignadas han sido actualizadas.");
                case CUSTOMER -> new NotificationMessage(
                        "Cambio de tienda seleccionada",
                        "El usuario " + target + " ha cambiado su tienda seleccionada.");
                default -> new NotificationMessage(
                        "Cambio de asignación de " + target,
                        "Las asignaciones del usuario " + target + " han cambiado.");
            };
            case UPDATED -> new NotificationMessage(
                    "Contraseña del usuario " + target + " actualizada",
                    "La clave de acceso del usuario " + target + " ha sido modificada.");
            case DELETED -> new NotificationMessage(
                    "Usuario " + target + " eliminado",
                    "La cuenta del usuario " + target + " ha sido eliminada del sistema.");
            default -> defaultMessage("Usuario " + target, action);
        };
    }

    public NotificationMessage buildForReview(EventAction action, NotificationRole role, ReviewMessageContext ctx) {
        String reviewer = display(ctx.reviewerUsername(), "un usuario");
        String product = quote(display(ctx.productName(), display(ctx.productRefCode(), "el producto")));
        String stars = ctx.rating() == null ? "" : ctx.rating() + "★ ";

        return switch (action) {
            case CREATED -> switch (role) {
                case ADMIN, MANAGER -> new NotificationMessage(
                        "Nueva reseña en " + product,
                        reviewer + " ha publicado una reseña " + stars + "sobre " + product + ".");
                case CUSTOMER -> new NotificationMessage(
                        "Nueva reseña en un producto que sigues",
                        reviewer + " ha dejado una reseña " + stars + "sobre " + product + ".");
                default -> new NotificationMessage(
                        "Nueva reseña en " + product,
                        reviewer + " ha dejado una nueva reseña.");
            };
            case UPDATED -> new NotificationMessage(
                    "Reseña actualizada en " + product,
                    reviewer + " ha modificado su reseña sobre " + product + ".");
            case DELETED -> new NotificationMessage(
                    "Reseña eliminada en " + product,
                    "Una reseña asociada al producto " + product + " ha sido eliminada.");
            default -> defaultMessage("Reseña en " + product, action);
        };
    }

    public NotificationMessage buildForStockRestocked(NotificationRole role, ShopStockMessageContext ctx) {
        String product = quote(display(ctx.productName(), display(ctx.productRefCode(), "el producto")));
        String shop = display(ctx.shopName(), display(ctx.shopRefCode(), "la tienda"));
        int added = Math.max(0, ctx.newUnits() - ctx.oldUnits());

        return switch (role) {
            case ADMIN -> new NotificationMessage(
                    product + " repuesto en " + shop,
                    "Se han añadido " + added + " unidades de " + product + " a la tienda " + shop + " (antes: " + ctx.oldUnits() + ", ahora: " + ctx.newUnits() + ").");
            case CUSTOMER -> new NotificationMessage(
                    product + " vuelve a estar disponible",
                    product + " ya está disponible de nuevo en tu tienda " + shop + ".");
            case MANAGER -> new NotificationMessage(
                    "Reposición de " + product + " en " + shop,
                    "Quedan " + ctx.newUnits() + " unidades de " + product + " tras la reposición.");
            case DRIVER -> new NotificationMessage(
                    "Reposición en " + shop,
                    "El producto " + product + " ha sido repuesto en " + shop + ".");
        };
    }

    public NotificationMessage buildForStockLow(NotificationRole role, ShopStockMessageContext ctx) {
        String product = quote(display(ctx.productName(), display(ctx.productRefCode(), "el producto")));
        String shop = display(ctx.shopName(), display(ctx.shopRefCode(), "la tienda"));

        return switch (role) {
            case MANAGER -> new NotificationMessage(
                    "Stock bajo de " + product + " en tu tienda",
                    "Quedan " + ctx.newUnits() + " unidades de " + product + " en " + shop + " (umbral: " + ctx.threshold() + "). Conviene reponer.");
            case CUSTOMER -> new NotificationMessage(
                    "Pocas unidades de " + product,
                    "Solo quedan " + ctx.newUnits() + " unidades de " + product + " en " + shop + ". Si te interesa, date prisa.");
            case ADMIN -> new NotificationMessage(
                    "Stock bajo: " + product + " en " + shop,
                    "Quedan " + ctx.newUnits() + " unidades de " + product + " en " + shop + " (umbral: " + ctx.threshold() + ").");
            case DRIVER -> new NotificationMessage(
                    "Stock bajo en " + shop,
                    "Quedan pocas unidades de " + product + " en " + shop + ".");
        };
    }

    public NotificationMessage buildForStock(ShopStockEvent.StockAction action, NotificationRole role, ShopStockMessageContext ctx) {
        return switch (action) {
            case RESTOCKED -> buildForStockRestocked(role, ctx);
            case LOW_STOCK -> buildForStockLow(role, ctx);
        };
    }

    // --- helpers ---

    private String display(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String quote(String value) {
        return "\"" + value + "\"";
    }

    private NotificationMessage defaultMessage(String entityLabel, EventAction action) {
        return new NotificationMessage(
                entityLabel + " — actualización",
                "Se ha producido un evento (" + action.name() + ") relacionado con " + entityLabel + "."
        );
    }
}
