package com.tfg.backend.model;

public enum OrderStatus {
    //IN_CART, //Usable if cart summary logic is finally implemented
    ORDER_MADE,
    SENT,
    ON_DELIVERY,
    COMPLETED,
    CANCELLED;

    @Override
    public String toString() {
        return switch (this) {
            case ORDER_MADE -> "Pedido realizado";
            case SENT -> "Enviado";
            case ON_DELIVERY -> "En Reparto";
            case COMPLETED -> "Completado";
            case CANCELLED -> "Cancelado";
        };
    }
}
