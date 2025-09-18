package com.projects.wtg.model.converter;

import com.projects.wtg.model.PlanStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PlanStatusConverter implements AttributeConverter<PlanStatus, String> {

    @Override
    public String convertToDatabaseColumn(PlanStatus planStatus) {
        if (planStatus == null) {
            return null;
        }
        return planStatus.name().toLowerCase(); // Salva como "active", "inactive", etc.
    }

    @Override
    public PlanStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return PlanStatus.valueOf(dbData.toUpperCase()); // Converte "active" de volta para o enum ACTIVE
    }
}