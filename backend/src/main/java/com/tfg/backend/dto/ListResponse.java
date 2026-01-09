package com.tfg.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ListResponse<T> {
    private List<T> items;

    public ListResponse(List<T> items) {
        this.items = items;
    }
}
