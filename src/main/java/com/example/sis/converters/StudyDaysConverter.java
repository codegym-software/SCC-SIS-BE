package com.example.sis.converters;

import com.example.sis.enums.StudyDay;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class StudyDaysConverter implements AttributeConverter<List<StudyDay>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<StudyDay> studyDays) {
        if (studyDays == null || studyDays.isEmpty()) {
            return null;
        }
        try {
            // Chuyển List<StudyDay> thành JSON: ["MONDAY", "THURSDAY"]
            return objectMapper.writeValueAsString(studyDays);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting study days to JSON", e);
        }
    }

    @Override
    public List<StudyDay> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            // Chuyển JSON thành List<StudyDay>
            return objectMapper.readValue(dbData, new TypeReference<List<StudyDay>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error parsing study days from JSON", e);
        }
    }
}
