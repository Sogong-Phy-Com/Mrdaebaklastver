package com.mrdabak.dinnerservice.voice.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Session state for a single authenticated customer voice order.
 */
public class VoiceOrderSession {

    private final String sessionId;
    private final Long userId;
    private final String customerName;
    private final String customerDefaultAddress;
    private final String customerPhone;
    private final List<VoiceConversationMessage> messages = new ArrayList<>();

    private VoiceOrderState currentState = new VoiceOrderState();
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private boolean orderPlaced;
    private Long createdOrderId;

    public VoiceOrderSession(String sessionId, Long userId, String customerName, String customerDefaultAddress, String customerPhone) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.customerName = customerName;
        this.customerDefaultAddress = customerDefaultAddress;
        this.customerPhone = customerPhone;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerDefaultAddress() {
        return customerDefaultAddress;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public VoiceOrderState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(VoiceOrderState currentState) {
        this.currentState = currentState;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public List<VoiceConversationMessage> getMessages() {
        return messages;
    }

    public void addMessage(VoiceConversationMessage message, int maxMessages) {
        messages.add(message);
        if (messages.size() > maxMessages) {
            messages.sort(Comparator.comparing(VoiceConversationMessage::getTimestamp));
            while (messages.size() > maxMessages) {
                messages.remove(0);
            }
        }
        touch();
    }

    public List<VoiceConversationMessage> getVisibleMessages() {
        return messages.stream()
                .filter(VoiceConversationMessage::isVisibleToClient)
                .sorted(Comparator.comparing(VoiceConversationMessage::getTimestamp))
                .collect(Collectors.toList());
    }

    public boolean isOrderPlaced() {
        return orderPlaced;
    }

    public void markOrderPlaced(Long orderId) {
        this.orderPlaced = true;
        this.createdOrderId = orderId;
    }

    public Long getCreatedOrderId() {
        return createdOrderId;
    }
}


