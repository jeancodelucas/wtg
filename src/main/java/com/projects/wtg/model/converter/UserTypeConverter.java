package com.projects.wtg.model.converter;

import com.projects.wtg.model.UserType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserTypeConverter implements AttributeConverter<UserType, String> {

    @Override
    public String convertToDatabaseColumn(UserType userType) {
        if (userType == null) {
            return null;
        }
        // Converte para minúsculas para corresponder aos valores no banco (ex: 'usuario')
        return userType.name().toLowerCase();
    }

    @Override
    public UserType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        // Converte a string do banco para maiúsculas antes de procurar no enum
        return UserType.valueOf(dbData.toUpperCase());
    }
}