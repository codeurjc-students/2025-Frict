package com.tfg.backend.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrdersPageDTO {
    private List<OrderDTO> orders;
    private Long totalOrders;
    private int currentPage;
    private int lastPage;
    private int pageSize;

    public OrdersPageDTO(List<OrderDTO> orders, Long totalOrders, int currentPage, int lastPage, int pageSize) {
        this.orders = orders;
        this.totalOrders = totalOrders;
        this.currentPage = currentPage;
        this.lastPage = lastPage;
        this.pageSize = pageSize;
    }
}
