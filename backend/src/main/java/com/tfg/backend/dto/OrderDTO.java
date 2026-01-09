package com.tfg.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tfg.backend.model.Order;
import com.tfg.backend.model.OrderItem;
import com.tfg.backend.model.StatusLog;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class OrderDTO {
    private Long id;
    private String referenceCode;
    private List<StatusLogDTO> history = new ArrayList<>();
    private Long userId;
    private List<OrderItemDTO> orderItems = new ArrayList<>();
    private Long assignedTruckId;
    private int estimatedCompletionTime;

    private int totalItems;
    private double subtotalCost;
    private double totalDiscount;
    private double shippingCost;
    private double totalCost;

    private String cardNumberEnding;
    private String fullSendingAddress;

    @JsonFormat(pattern = "dd/MM/yy HH:mm")
    private LocalDateTime createdAt;

    public OrderDTO() {
    }

    public OrderDTO(Order o){
        this.id = o.getId();
        this.referenceCode = o.getReferenceCode();
        for (StatusLog l : o.getHistory()) {
            this.history.add(new StatusLogDTO(l));
        }
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
        this.createdAt = o.getCreatedAt();
    }
}
