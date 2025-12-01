package com.mrdabak.dinnerservice.voice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VoiceUtteranceRequest {
    @NotBlank
    private String sessionId;

    @NotBlank
    private String userText;
}


