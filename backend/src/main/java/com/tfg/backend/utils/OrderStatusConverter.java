package com.tfg.backend.utils;

import com.tfg.backend.model.OrderStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class OrderStatusConverter implements Converter<String, OrderStatus> {
    @Override
    public OrderStatus convert(String source) {
        return OrderStatus.fromDescription(source);
    }
}