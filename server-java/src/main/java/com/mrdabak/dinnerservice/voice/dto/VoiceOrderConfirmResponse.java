package com.mrdabak.dinnerservice.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VoiceOrderConfirmResponse {
    private String sessionId;
    private Long orderId;
    private Integer totalPrice;
    private VoiceOrderSummaryDto summary;
    private String confirmationMessage;
}


