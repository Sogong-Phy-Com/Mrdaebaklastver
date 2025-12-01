package com.mrdabak.dinnerservice.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VoiceMessageDto {
    private String id;
    private String role;
    private String content;
    private String timestamp;
}


