package com.mrdabak.dinnerservice.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VoiceUtteranceResponse {
    private String sessionId;
    private VoiceMessageDto userMessage;
    private VoiceMessageDto agentMessage;
    private VoiceOrderSummaryDto summary;
    private boolean readyForConfirmation;
}


