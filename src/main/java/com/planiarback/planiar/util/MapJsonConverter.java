package com.planiarback.planiar.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Converter(autoApply = false)
public class MapJsonConverter implements AttributeConverter<Map<String, List<String>>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, List<String>> attribute) {
        try {
            if (attribute == null) return null;
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert map to JSON", e);
        }
    }

    @Override
    public Map<String, List<String>> convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isEmpty()) return Collections.emptyMap();
            return MAPPER.readValue(dbData, new TypeReference<Map<String, List<String>>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert JSON to map", e);
        }
    }
}
