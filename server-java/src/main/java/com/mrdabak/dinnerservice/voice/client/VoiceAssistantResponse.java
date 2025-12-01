package com.mrdabak.dinnerservice.voice.client;

import com.mrdabak.dinnerservice.voice.model.VoiceOrderState;

public record VoiceAssistantResponse(String assistantMessage,
                                     VoiceOrderState orderState,
                                     String rawContent) {
}


