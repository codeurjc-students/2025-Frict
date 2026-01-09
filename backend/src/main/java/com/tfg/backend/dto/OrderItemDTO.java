package com.tfg.backend.dto;

import com.tfg.backend.model.OrderItem;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemDTO {
    private Long id;
    private Long orderId;

    private ProductDTO product;

    private Long userId;

    //Maximum number of items of this product that a user can select (cart)
    private int maxQuantity; // units selected by the user + (stock - reserved)
    private int quantity;

    //Stores the price of an item at the moment of the OrderItem creation (cart items = updated with currentPrice * quantity, historic items = original price preserved)
    private double itemsCost;

    public OrderItemDTO() {
    }

    public OrderItemDTO(OrderItem i){
        this.id = i.getId();
        this.orderId = (i.getOrder() != null) ? i.getOrder().getId() : null;
        this.product = new ProductDTO(i.getProduct());
        this.userId = i.getUser().getId();
        this.quantity = i.getQuantity();
        this.maxQuantity = this.quantity + (this.product.getAvailableUnits()); // availableUnits = total - reserved
        this.itemsCost = i.getProduct().getCurrentPrice() * i.getQuantity();
    }
}
