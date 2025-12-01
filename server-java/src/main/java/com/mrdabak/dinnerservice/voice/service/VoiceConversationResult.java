package com.mrdabak.dinnerservice.voice.service;

import com.mrdabak.dinnerservice.voice.client.VoiceAssistantResponse;
import com.mrdabak.dinnerservice.voice.model.VoiceConversationMessage;
import com.mrdabak.dinnerservice.voice.model.VoiceOrderSession;

public record VoiceConversationResult(
        VoiceOrderSession session,
        VoiceConversationMessage userMessage,
        VoiceConversationMessage agentMessage,
        VoiceAssistantResponse assistantResponse) {
}


