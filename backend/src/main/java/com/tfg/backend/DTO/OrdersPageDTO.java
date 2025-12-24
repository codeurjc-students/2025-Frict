package com.tfg.backend.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrdersPageDTO {
    private List<OrderDTO> orders;
    private Long totalItems;
    private int currentPage;
    private int lastPage;
    private int pageSize;

    public OrdersPageDTO(List<OrderDTO> orders, Long totalItems, int currentPage, int lastPage, int pageSize) {
        this.orders = orders;
        this.totalItems = totalItems;
        this.currentPage = currentPage;
        this.lastPage = lastPage;
        this.pageSize = pageSize;
    }
}
