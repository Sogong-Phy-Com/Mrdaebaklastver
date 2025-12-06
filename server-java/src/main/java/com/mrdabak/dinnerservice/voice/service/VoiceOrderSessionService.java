package com.mrdabak.dinnerservice.voice.service;

import com.mrdabak.dinnerservice.voice.VoiceOrderException;
import com.mrdabak.dinnerservice.voice.model.VoiceOrderSession;
import com.mrdabak.dinnerservice.voice.model.VoiceOrderState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VoiceOrderSessionService {

    private final Map<String, VoiceOrderSession> sessions = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final int historyLimit;

    public VoiceOrderSessionService(
            @Value("${voice.session.ttl-minutes:45}") long ttlMinutes,
            @Value("${voice.history.max-messages:40}") int historyLimit) {
        this.ttl = Duration.ofMinutes(ttlMinutes);
        this.historyLimit = historyLimit;
    }

    public VoiceOrderSession createSession(Long userId, String customerName, String address, String phone, Boolean hasConsent) {
        purgeExpiredSessions();
        String sessionId = UUID.randomUUID().toString();
        // 개인정보 동의가 없으면 null로 설정
        String sessionName = Boolean.TRUE.equals(hasConsent) ? customerName : null;
        String sessionAddress = Boolean.TRUE.equals(hasConsent) ? address : null;
        String sessionPhone = Boolean.TRUE.equals(hasConsent) ? phone : null;
        VoiceOrderSession session = new VoiceOrderSession(sessionId, userId, sessionName, sessionAddress, sessionPhone);
        session.setCurrentState(new VoiceOrderState());
        sessions.put(sessionId, session);
        return session;
    }

    public Optional<VoiceOrderSession> findSession(String sessionId) {
        purgeExpiredSessions();
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public VoiceOrderSession requireSession(String sessionId) {
        return findSession(sessionId)
                .orElseThrow(() -> new VoiceOrderException("유효하지 않은 음성 주문 세션입니다. 다시 시작해 주세요."));
    }

    public int historyLimit() {
        return historyLimit;
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }

    private void purgeExpiredSessions() {
        Instant now = Instant.now();
        sessions.values().removeIf(session -> now.isAfter(session.getUpdatedAt().plus(ttl)));
    }
}


