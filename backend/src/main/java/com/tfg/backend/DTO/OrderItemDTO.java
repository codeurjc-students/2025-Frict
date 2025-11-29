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

    private ProductDTO product;

    private Long userId;
    private int quantity;

    public OrderItemDTO() {
    }

    public OrderItemDTO(OrderItem i){
        this.id = i.getId();
        this.orderId = (i.getOrder() != null) ? i.getOrder().getId() : null;
        this.product = new ProductDTO(i.getProduct());
        this.userId = i.getUser().getId();
        this.quantity = i.getQuantity();
    }
}
