package com.tfg.backend.DTO;

import com.tfg.backend.model.OrderItem;
import com.tfg.backend.model.ShopStock;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemDTO {
    private Long id;
    private Long orderId;

    private Long productId;
    private double previousPrice;
    private double currentPrice;

    private Long userId;
    private int quantity;
    private double totalPrice;

    public OrderItemDTO() {
    }

    public OrderItemDTO(OrderItem i){
        this.id = i.getId();
        this.orderId = i.getOrder().getId();
        this.productId = i.getProduct().getId();
        this.previousPrice = i.getProduct().getPreviousPrice();
        this.currentPrice = i.getProduct().getCurrentPrice();
        this.userId = i.getUser().getId();
        this.quantity = i.getQuantity();
        this.totalPrice = i.getProduct().getCurrentPrice() * i.getQuantity();
    }
}
