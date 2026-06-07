package com.tahaberkamcadev.inventory_service.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.tahaberkamcadev.inventory_service.dto.ReviewSummary;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Converter
public class ReviewSummaryListConverter implements AttributeConverter<List<ReviewSummary>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<ReviewSummary> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize review summaries", e);
        }
    }

    @Override
    public List<ReviewSummary> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(Arrays.asList(MAPPER.readValue(dbData, ReviewSummary[].class)));
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to deserialize review summaries", e);
        }
    }
}
