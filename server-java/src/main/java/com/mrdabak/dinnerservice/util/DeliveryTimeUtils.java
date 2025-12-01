package com.mrdabak.dinnerservice.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility helper for parsing delivery/reservation timestamps that are provided by the clients.
 */
public final class DeliveryTimeUtils {

    private static final DateTimeFormatter[] SUPPORTED_FORMATS = new DateTimeFormatter[] {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    };

    private DeliveryTimeUtils() {
    }

    public static LocalDateTime parseDeliveryTime(String deliveryTime) {
        if (deliveryTime == null || deliveryTime.trim().isEmpty()) {
            throw new IllegalArgumentException("배달 시간은 필수입니다.");
        }
        for (DateTimeFormatter formatter : SUPPORTED_FORMATS) {
            try {
                return LocalDateTime.parse(deliveryTime, formatter);
            } catch (DateTimeParseException ignored) {
                // try next formatter
            }
        }
        throw new RuntimeException("잘못된 배달 시간 형식입니다. (예: 2025-11-19T18:00 또는 2025-11-19T18:00:00)");
    }

    public static LocalDate extractReservationDate(String deliveryTime) {
        return parseDeliveryTime(deliveryTime).toLocalDate();
    }
}

