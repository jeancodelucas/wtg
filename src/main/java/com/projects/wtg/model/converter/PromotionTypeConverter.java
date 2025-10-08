package com.projects.wtg.model.converter;

import com.projects.wtg.model.PromotionType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PromotionTypeConverter implements AttributeConverter<PromotionType, String> {

    @Override
    public String convertToDatabaseColumn(PromotionType promotionType) {
        if (promotionType == null) {
            return null;
        }
        return promotionType.name().toLowerCase();
    }

    @Override
    public PromotionType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return PromotionType.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Lidar com valores inválidos do banco, se necessário
            return null;
        }
    }
}