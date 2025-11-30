package com.tfg.backend.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.Date;
import java.time.YearMonth;

@Converter(autoApply = true) // Always applies when working with YearMonth
public class YearMonthDateConverter implements AttributeConverter<YearMonth, Date> {

    @Override
    public Date convertToDatabaseColumn(YearMonth attribute) {
        if (attribute == null) return null;
        // Truco: Guardamos el YearMonth como el PRIMER día de ese mes en la BD
        return Date.valueOf(attribute.atDay(1));
    }

    @Override
    public YearMonth convertToEntityAttribute(Date dbData) {
        if (dbData == null) return null;
        // Al leer, ignoramos el día y nos quedamos solo con Mes y Año
        return YearMonth.from(dbData.toLocalDate());
    }
}