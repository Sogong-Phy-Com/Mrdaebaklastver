package com.mrdabak.dinnerservice.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Converter(autoApply = true)
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, String> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String convertToDatabaseColumn(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.format(FORMATTER);
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        
        // Try to parse with the standard format first
        try {
            return LocalDateTime.parse(dbData, FORMATTER);
        } catch (DateTimeParseException e) {
            // If that fails, try ISO format (with T separator)
            try {
                return LocalDateTime.parse(dbData.replace(' ', 'T'));
            } catch (DateTimeParseException e2) {
                // If that also fails, try parsing as ISO-8601 with optional timezone
                try {
                    String cleaned = dbData.replace(' ', 'T');
                    if (cleaned.contains("+") || cleaned.contains("Z")) {
                        // Remove timezone info for LocalDateTime
                        int tzIndex = cleaned.indexOf('+');
                        if (tzIndex == -1) tzIndex = cleaned.indexOf('Z');
                        if (tzIndex == -1) tzIndex = cleaned.indexOf('-', 10); // Skip date part
                        if (tzIndex > 0) {
                            cleaned = cleaned.substring(0, tzIndex);
                        }
                    }
                    return LocalDateTime.parse(cleaned);
                } catch (DateTimeParseException e3) {
                    // If all parsing fails, log and return null
                    System.err.println("[LocalDateTimeConverter] Failed to parse timestamp: " + dbData);
                    System.err.println("[LocalDateTimeConverter] Error: " + e3.getMessage());
                    return null;
                }
            }
        }
    }
}

