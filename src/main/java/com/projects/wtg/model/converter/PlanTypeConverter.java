package com.projects.wtg.model.converter;

import com.projects.wtg.model.PlanType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PlanTypeConverter implements AttributeConverter<PlanType, String> {

    @Override
    public String convertToDatabaseColumn(PlanType planType) {
        if (planType == null) {
            return null;
        }
        // Converte para minúsculas antes de salvar, para manter o padrão do banco
        return planType.name().toLowerCase();
    }

    @Override
    public PlanType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        // CORREÇÃO: Converte a string do banco para maiúsculas antes de procurar no enum
        return PlanType.valueOf(dbData.toUpperCase());
    }
}