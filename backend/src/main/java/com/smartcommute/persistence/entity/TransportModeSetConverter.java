package com.smartcommute.persistence.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommute.domain.graph.TransportMode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Converts a {@code Set<TransportMode>} to/from a JSON array string for storage in a JSONB column
 * (preferred_modes / avoided_modes). Using a converter keeps the entity clean and avoids pulling in
 * a Hibernate-specific JSON type dependency.
 */
@Converter
public class TransportModeSetConverter implements AttributeConverter<Set<TransportMode>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Set<TransportMode> attribute) {
        try {
            Set<TransportMode> value = attribute == null ? Collections.emptySet() : attribute;
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("failed to serialize transport modes", e);
        }
    }

    @Override
    public Set<TransportMode> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return EnumSet.noneOf(TransportMode.class);
        }
        try {
            List<TransportMode> list = MAPPER.readValue(dbData, new TypeReference<List<TransportMode>>() {
            });
            return list.isEmpty() ? EnumSet.noneOf(TransportMode.class) : EnumSet.copyOf(list);
        } catch (Exception e) {
            throw new IllegalStateException("failed to deserialize transport modes: " + dbData, e);
        }
    }
}
