package com.mrdabak.dinnerservice.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class VoiceOrderStartResponse {
    private String sessionId;
    private List<VoiceMessageDto> messages;
    private VoiceOrderSummaryDto summary;
}


