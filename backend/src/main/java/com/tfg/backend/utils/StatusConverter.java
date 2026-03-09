package com.tfg.backend.utils;

import com.tfg.backend.model.OrderStatus;
import com.tfg.backend.model.TruckStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StatusConverter {

    @Component
    public static class ToOrderStatus implements Converter<String, OrderStatus> {
        @Override
        public OrderStatus convert(String source) {
            return OrderStatus.fromDescription(source);
        }
    }

    @Component
    public static class ToTruckStatus implements Converter<String, TruckStatus> {
        @Override
        public TruckStatus convert(String source) {
            return TruckStatus.fromDescription(source);
        }
    }
}