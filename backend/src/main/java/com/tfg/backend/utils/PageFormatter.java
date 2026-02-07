package com.tfg.backend.utils;

import com.tfg.backend.dto.PageResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PageFormatter {

    // <T> Original entity (Product, Shop, etc.)
    // <R> Resulting DTO (ProductDTO, ShopDTO, etc.)
    public static  <T, R> PageResponse<R> toPageResponse(Page<T> page, Function<T, R> mapper) {
        List<R> dtos = page.getContent().stream()
                .map(mapper)
                .collect(Collectors.toList());

        return new PageResponse<>(
                dtos,
                page.getTotalElements(),
                page.getNumber(),
                page.getTotalPages() - 1,
                page.getSize()
        );
    }
}
