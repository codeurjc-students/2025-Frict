package com.tfg.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tfg.backend.model.Order;
import com.tfg.backend.model.OrderItem;
import com.tfg.backend.model.OrderStatusLog;
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
    private List<OrderStatusLogDTO> history = new ArrayList<>();
    private UserDTO user;
    private List<OrderItemDTO> orderItems = new ArrayList<>();
    private Long assignedShopId;
    private Long assignedTruckId;
    private int estimatedCompletionTime;

    private int totalItems;
    private double subtotalCost;
    private double totalDiscount;
    private double shippingCost;
    private double totalCost;
    private double totalCapacity;

    private String cardNumberEnding;
    private String sendingAddress;
    private double sendingAddressLat;
    private double sendingAddressLng;

    @JsonFormat(pattern = "dd/MM/yy HH:mm")
    private LocalDateTime createdAt;

    public OrderDTO() {
    }

    public OrderDTO(Order o){
        this.id = o.getId();
        this.referenceCode = o.getReferenceCode();
        for (OrderStatusLog l : o.getHistory()) {
            this.history.add(new OrderStatusLogDTO(l));
        }
        this.user = new UserDTO(o.getUser());
        for (OrderItem item : o.getItems()) {
            orderItems.add(new OrderItemDTO(item));
        }
        if(o.getAssignedShop() != null){
            this.assignedShopId = o.getAssignedShop().getId();
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
        this.totalCapacity = o.getTotalCapacity();

        this.cardNumberEnding = o.getCardNumberEnding();
        this.sendingAddress = o.getFullSendingAddress();
        this.sendingAddressLat = o.getSendingAddressLat();
        this.sendingAddressLng = o.getSendingAddressLng();
        this.createdAt = o.getCreatedAt();
    }
}
