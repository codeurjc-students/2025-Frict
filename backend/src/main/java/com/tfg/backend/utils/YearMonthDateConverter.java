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
        // Save the YearMonth as the first day of that month in DB
        return Date.valueOf(attribute.atDay(1));
    }

    @Override
    public YearMonth convertToEntityAttribute(Date dbData) {
        if (dbData == null) return null;
        // To read, ignore the day and get month and year only
        return YearMonth.from(dbData.toLocalDate());
    }
}