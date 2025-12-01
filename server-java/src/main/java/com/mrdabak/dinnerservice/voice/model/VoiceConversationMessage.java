package com.mrdabak.dinnerservice.voice.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single conversational turn that can optionally be hidden from the UI
 * (e.g. synthetic system messages).
 */
public final class VoiceConversationMessage {

    private final String id;
    private final String role;
    private final String content;
    private final Instant timestamp;
    private final boolean visibleToClient;

    private VoiceConversationMessage(String id, String role, String content, Instant timestamp, boolean visibleToClient) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
        this.visibleToClient = visibleToClient;
    }

    public static VoiceConversationMessage of(String role, String content, boolean visibleToClient) {
        return new VoiceConversationMessage(UUID.randomUUID().toString(), role, content, Instant.now(), visibleToClient);
    }

    public String getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean isVisibleToClient() {
        return visibleToClient;
    }
}


