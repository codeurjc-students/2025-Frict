package com.tfg.backend.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderItemsPageDTO {
    private List<OrderItemDTO> products;
    private Long totalProducts;
    private int currentPage;
    private int lastPage;
    private int pageSize;

    public OrderItemsPageDTO(List<OrderItemDTO> products, Long totalProducts, int currentPage, int lastPage, int pageSize) {
        this.products = products;
        this.totalProducts = totalProducts;
        this.currentPage = currentPage;
        this.lastPage = lastPage;
        this.pageSize = pageSize;
    }
}
