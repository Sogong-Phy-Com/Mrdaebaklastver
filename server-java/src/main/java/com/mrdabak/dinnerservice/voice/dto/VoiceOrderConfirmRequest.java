package com.mrdabak.dinnerservice.voice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VoiceOrderConfirmRequest {
    @NotBlank
    private String sessionId;
}


