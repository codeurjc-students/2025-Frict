package com.tfg.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderItemsPageDTO {
    private List<OrderItemDTO> orderItems;
    private Long totalItems;
    private int currentPage;
    private int lastPage;
    private int pageSize;

    public OrderItemsPageDTO(List<OrderItemDTO> items, Long totalItems, int currentPage, int lastPage, int pageSize) {
        this.orderItems = items;
        this.totalItems = totalItems;
        this.currentPage = currentPage;
        this.lastPage = lastPage;
        this.pageSize = pageSize;
    }
}
