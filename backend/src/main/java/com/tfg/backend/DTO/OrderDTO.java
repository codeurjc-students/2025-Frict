package com.tfg.backend.DTO;

import com.tfg.backend.model.Order;
import com.tfg.backend.model.OrderItem;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class OrderDTO {
    private Long id;
    private String referenceCode;
    private String status;
    private Long userId;
    List<OrderItemDTO> orderItems = new ArrayList<>();
    private Long assignedTruckId;
    private int estimatedCompletionTime;

    private int totalItems;
    private double subtotalCost;
    private double totalDiscount;
    private double shippingCost;
    private double totalCost;

    private String cardNumberEnding;
    private String fullSendingAddress;

    public OrderDTO() {
    }

    public OrderDTO(Order o){
        this.id = o.getId();
        this.referenceCode = o.getReferenceCode();
        this.status = o.getStatus().toString();
        this.userId = o.getUser().getId();
        for (OrderItem item : o.getItems()) {
            orderItems.add(new OrderItemDTO(item));
        }
        if(o.getAssignedTruck() != null){
            this.assignedTruckId = o.getAssignedTruck().getId();
        }
        this.estimatedCompletionTime = o.getEstimatedCompletionTime();

        this.totalItems = o.getTotalItems();
        this.subtotalCost = o.getSubtotalCost();
        this.totalDiscount = o.getTotalDiscount();
        this.shippingCost = o.getShippingCost();
        this.totalCost = o.getTotalCost();

        this.cardNumberEnding = o.getCardNumberEnding();
        this.fullSendingAddress = o.getFullSendingAddress();
    }
}
