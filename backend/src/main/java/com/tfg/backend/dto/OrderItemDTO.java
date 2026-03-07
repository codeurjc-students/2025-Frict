package com.tfg.backend.dto;

import com.tfg.backend.model.OrderItem;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemDTO {
    private Long id;
    private Long orderId;

    private ProductDTO product; //Always null when the item is part of an order

    private String productName;
    private String productImageUrl;
    private double productPrice;

    private Long userId;

    private int quantity;

    //Stores the price of an item at the moment of the OrderItem creation (cart items = updated with currentPrice * quantity, historic items = original price preserved)
    private double itemsCost;

    public OrderItemDTO(OrderItem i){
        this.id = i.getId();
        this.orderId = (i.getOrder() != null) ? i.getOrder().getId() : null;

        if (i.getProduct() != null){
            this.product = new ProductDTO(i.getProduct());
            this.itemsCost = i.getProduct().getCurrentPrice() * i.getQuantity();
        }
        else {
            this.itemsCost = i.getProductPrice() * i.getQuantity();
        }

        this.productName = i.getProductName();
        this.productImageUrl = i.getProductImageUrl();
        this.productPrice = i.getProductPrice();

        this.userId = i.getUser().getId();
        this.quantity = i.getQuantity();
    }
}
