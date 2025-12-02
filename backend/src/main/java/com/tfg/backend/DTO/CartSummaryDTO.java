package com.tfg.backend.DTO;

import lombok.Getter;
import lombok.Setter;

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
