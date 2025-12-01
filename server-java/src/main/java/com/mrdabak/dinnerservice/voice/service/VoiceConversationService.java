package com.mrdabak.dinnerservice.voice.service;

import com.mrdabak.dinnerservice.model.User;
import com.mrdabak.dinnerservice.voice.client.VoiceAssistantResponse;
import com.mrdabak.dinnerservice.voice.model.VoiceConversationMessage;
import com.mrdabak.dinnerservice.voice.model.VoiceOrderSession;
import com.mrdabak.dinnerservice.voice.model.VoiceOrderState;
import com.mrdabak.dinnerservice.voice.util.DomainVocabularyNormalizer;
import org.springframework.stereotype.Service;

@Service
public class VoiceConversationService {

    private final VoiceOrderSessionService sessionService;
    private final VoiceOrderAssistantClient assistantClient;
    private final VoiceOrderStateMerger stateMerger;
    private final DomainVocabularyNormalizer normalizer;
    private final VoiceMenuCatalogService menuCatalogService;

    public VoiceConversationService(VoiceOrderSessionService sessionService,
                                    VoiceOrderAssistantClient assistantClient,
                                    VoiceOrderStateMerger stateMerger,
                                    DomainVocabularyNormalizer normalizer,
                                    VoiceMenuCatalogService menuCatalogService) {
        this.sessionService = sessionService;
        this.assistantClient = assistantClient;
        this.stateMerger = stateMerger;
        this.normalizer = normalizer;
        this.menuCatalogService = menuCatalogService;
    }

    public VoiceConversationResult startSession(User user) {
        VoiceOrderSession session = sessionService.createSession(
                user.getId(), user.getName(), user.getAddress(), user.getPhone());
        String greetingPrompt = """
                고객 이름: %s
                상황: 고객이 음성 주문 페이지에 접속했습니다. 정중하게 인사하고 기념일/행사 여부를 물어보며 대화를 시작하세요.
                """.formatted(user.getName());
        return sendToAssistant(session, greetingPrompt, false);
    }

    public VoiceConversationResult handleUtterance(String sessionId, String userText, boolean visibleToClient) {
        VoiceOrderSession session = sessionService.requireSession(sessionId);
        String cleaned = normalizer.cleanupTranscript(userText);
        return sendToAssistant(session, cleaned, visibleToClient);
    }

    private VoiceConversationResult sendToAssistant(VoiceOrderSession session, String userText, boolean visible) {
        VoiceConversationMessage userMessage = VoiceConversationMessage.of("user", userText, visible);
        session.addMessage(userMessage, sessionService.historyLimit());

        VoiceAssistantResponse response = assistantClient.generateResponse(session, menuCatalogService.buildPromptBlock());
        VoiceConversationMessage agentMessage = VoiceConversationMessage.of("assistant", response.assistantMessage(), true);
        session.addMessage(agentMessage, sessionService.historyLimit());

        if (session.getCurrentState() == null) {
            session.setCurrentState(new VoiceOrderState());
        }
        stateMerger.merge(session.getCurrentState(), response.orderState());
        return new VoiceConversationResult(session, userMessage, agentMessage, response);
    }
}


