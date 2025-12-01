package com.mrdabak.dinnerservice.voice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VoiceOrderSummaryDto {
    private String dinnerName;
    private String servingStyle;
    private List<SummaryItem> items = new ArrayList<>();
    private String deliverySlot;
    private String deliveryAddress;
    private String contactPhone;
    private String specialRequests;
    private boolean readyForConfirmation;
    private List<String> missingFields = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryItem {
        private String name;
        private Integer quantity;
    }
}


