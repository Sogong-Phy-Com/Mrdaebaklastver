package com.mrdabak.dinnerservice.voice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VoiceOrderConfirmRequest {
    @NotBlank
    private String sessionId;
    
    @NotBlank
    private String password; // 비밀번호 필수
}


