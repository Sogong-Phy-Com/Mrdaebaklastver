package com.mrdabak.dinnerservice.service;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class TravelTimeEstimator {

    private static final Map<String, Integer> REGION_BASELINE_MINUTES = Map.of(
            "강남", 28,
            "강북", 38,
            "서초", 32,
            "송파", 34,
            "관악", 30,
            "마포", 36,
            "용산", 35
    );

    private static final int DEFAULT_MINUTES = 40;

    public int estimateOneWayMinutes(String address, LocalDateTime deliveryTime) {
        if (address == null || address.isBlank()) {
            return DEFAULT_MINUTES;
        }

        int baseline = REGION_BASELINE_MINUTES.entrySet().stream()
                .filter(entry -> address.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(DEFAULT_MINUTES);

        int rushHourBuffer = isRushHour(deliveryTime) ? 12 : 5;
        int weekendBuffer = isWeekend(deliveryTime) ? 8 : 0;
        int distanceHeuristic = Math.min(15, Math.max(0, address.length() / 5 - 10));

        int total = baseline + rushHourBuffer + weekendBuffer + distanceHeuristic;
        return Math.max(20, Math.min(total, 75));
    }

    private boolean isRushHour(LocalDateTime deliveryTime) {
        if (deliveryTime == null) {
            return false;
        }
        int hour = deliveryTime.getHour();
        return hour >= 16 && hour <= 20;
    }

    private boolean isWeekend(LocalDateTime deliveryTime) {
        if (deliveryTime == null) {
            return false;
        }
        DayOfWeek day = deliveryTime.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}

