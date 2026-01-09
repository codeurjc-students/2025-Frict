package com.tfg.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PageResponse<T> {
    private List<T> items;
    private Long totalItems;
    private int currentPage;
    private int lastPage;
    private int pageSize;

    public PageResponse(List<T> items, Long totalItems, int currentPage, int lastPage, int pageSize) {
        this.items = items;
        this.totalItems = totalItems;
        this.currentPage = currentPage;
        this.lastPage = lastPage;
        this.pageSize = pageSize;
    }
}
