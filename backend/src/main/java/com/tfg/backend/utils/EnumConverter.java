package com.tfg.backend.utils;

import com.tfg.backend.dto.EntityType;
import com.tfg.backend.model.OrderStatus;
import com.tfg.backend.model.RegistryType;
import com.tfg.backend.model.TruckStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class EnumConverter {

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

    // --- NUEVOS CONVERSORES PARA REGISTROS Y NOTIFICACIONES ---

    @Component
    public static class ToEntityType implements Converter<String, EntityType> {
        @Override
        public EntityType convert(String source) {
            return EntityType.fromTranslation(source);
        }
    }

    @Component
    public static class ToRegistryType implements Converter<String, RegistryType> {
        @Override
        public RegistryType convert(String source) {
            return RegistryType.fromTranslation(source);
        }
    }
}