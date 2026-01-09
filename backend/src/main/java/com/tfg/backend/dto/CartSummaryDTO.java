package com.tfg.backend.dto;

import lombok.Getter;
import lombok.Setter;

//This intermediate class between OrderItem and Order helps to send the cart information to frontend, which will send this information to the backend once order is confirmed.
@Getter
@Setter
public class CartSummaryDTO {
    private int totalItems;
    private double subtotalCost;
    private double totalDiscount;
    private double shippingCost;
    private double totalCost;

    public CartSummaryDTO() {
    }

    public CartSummaryDTO(int totalItems, double subtotalCost, double totalDiscount, double shippingCost, double totalCost) {
        this.totalItems = totalItems;
        this.subtotalCost = subtotalCost;
        this.totalDiscount = totalDiscount;
        this.shippingCost = shippingCost;
        this.totalCost = totalCost;
    }
}
