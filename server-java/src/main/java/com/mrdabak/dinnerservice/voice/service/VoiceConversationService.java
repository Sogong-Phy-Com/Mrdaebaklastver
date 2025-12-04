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
        // JSON 필터링
        cleaned = filterJsonFromUserInput(cleaned);
        return sendToAssistant(session, cleaned, visibleToClient);
    }
    
    /**
     * 고객 입력에서 JSON 블록 제거 (안전하게)
     */
    private String filterJsonFromUserInput(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // 코드 블록 형태의 JSON 제거
        input = input.replaceAll("```json[\\s\\S]*?```", "");
        input = input.replaceAll("```[\\s\\S]*?```", "");
        
        // 명확한 JSON 블록 패턴만 제거 (여러 줄에 걸친 경우)
        if (input.contains("{") && input.contains("}") && input.contains("\"") && 
            (input.contains("order_state_json") || input.contains("assistant_message"))) {
            // JSON 블록 전체 제거
            input = input.replaceAll("(?s)assistant_message\\s*:.*?order_state_json\\s*:.*?```json[\\s\\S]*?```", "");
            input = input.replaceAll("(?s)order_state_json\\s*:.*?```json[\\s\\S]*?```", "");
            input = input.replaceAll("(?i)order_state_json\\s*:?\\s*", "");
            input = input.replaceAll("(?i)assistant_message\\s*:?\\s*", "");
        }
        
        return input.trim();
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
        
        // 배달 주소나 전화번호가 없으면 회원 정보 사용
        VoiceOrderState state = session.getCurrentState();
        if (state.getDeliveryAddress() == null || state.getDeliveryAddress().isBlank()) {
            if (session.getCustomerDefaultAddress() != null && !session.getCustomerDefaultAddress().isBlank()) {
                state.setDeliveryAddress(session.getCustomerDefaultAddress());
            }
        }
        if (state.getContactPhone() == null || state.getContactPhone().isBlank()) {
            if (session.getCustomerPhone() != null && !session.getCustomerPhone().isBlank()) {
                state.setContactPhone(session.getCustomerPhone());
            }
        }
        
        return new VoiceConversationResult(session, userMessage, agentMessage, response);
    }
}


