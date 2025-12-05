package com.mrdabak.dinnerservice.voice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrdabak.dinnerservice.voice.VoiceOrderException;
import com.mrdabak.dinnerservice.voice.client.VoiceAssistantResponse;
import com.mrdabak.dinnerservice.voice.model.VoiceOrderSession;
import com.mrdabak.dinnerservice.voice.model.VoiceOrderState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VoiceOrderAssistantClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceOrderAssistantClient.class);
    private static final Pattern JSON_BLOCK_PATTERN =
            Pattern.compile("order_state_json\\s*:?\\s*```json\\s*(\\{.*?})\\s*```", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String apiKey;
    private final String modelName;

    public VoiceOrderAssistantClient(RestTemplate restTemplate,
                                     ObjectMapper objectMapper,
                                     @Value("${voice.llm.api-url:https://api.groq.com/openai/v1/chat/completions}") String apiUrl,
                                     @Value("${voice.llm.api-key:}") String apiKey,
                                     @Value("${voice.llm.model:llama-3.3-70b-versatile}") String modelName) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiUrl = apiUrl;
        // API 키가 빈 문자열이거나 null인 경우 기본값 사용
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Groq API 키가 설정되지 않았습니다. 환경 변수 VOICE_LLM_API_KEY를 설정하거나 application.properties에 voice.llm.api-key를 설정해주세요.");
        }
        this.apiKey = apiKey.trim();
        LOGGER.info("Groq API 키가 설정되었습니다. (길이: {})", this.apiKey.length());
        this.modelName = modelName;
    }

    public VoiceAssistantResponse generateResponse(VoiceOrderSession session, String menuPromptBlock) {
        // API 키는 생성자에서 항상 유효한 값으로 설정됨
        LOGGER.debug("Groq API 호출 시작 - API 키 길이: {}", apiKey != null ? apiKey.length() : 0);
        
        try {
            List<Map<String, String>> messages = buildMessages(session, menuPromptBlock);
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", modelName);
            payload.put("messages", messages);
            payload.put("temperature", 0.3); // 일관된 한국어 응답
            payload.put("max_tokens", 1000); // 응답 길이 제한

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // Groq API 키는 "gsk-"로 시작
            String apiKeyToUse = apiKey.trim();
            if (!apiKeyToUse.startsWith("gsk-")) {
                LOGGER.warn("Groq API 키가 'gsk-'로 시작하지 않습니다. 형식을 확인해주세요.");
            }
            headers.setBearerAuth(apiKeyToUse);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl, new HttpEntity<>(payload, headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                LOGGER.error("LLM API 응답 오류: Status={}, Body={}", 
                    response.getStatusCode(), response.getBody());
                throw new VoiceOrderException("대화형 AI 응답이 올바르지 않습니다.");
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            
            // Groq API 응답 형식 파싱 (OpenAI 호환 형식)
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new VoiceOrderException("대화형 AI 응답에 선택지가 없습니다.");
            }
            
            JsonNode messageNode = choices.get(0).path("message").path("content");
            if (messageNode.isMissingNode() || !messageNode.isTextual()) {
                throw new VoiceOrderException("대화형 AI 응답을 읽을 수 없습니다.");
            }

            String content = messageNode.asText();
            return parseContent(content);
        } catch (VoiceOrderException e) {
            throw e;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            LOGGER.error("LLM API HTTP 오류: Status={}, Body={}", 
                e.getStatusCode(), e.getResponseBodyAsString(), e);
            
            if (e.getStatusCode().value() == 401) {
                throw new VoiceOrderException("LLM API 키가 유효하지 않습니다. API 키를 확인해주세요.", e);
            } else if (e.getStatusCode().value() == 413) {
                // Payload Too Large - 토큰 제한 초과
                String errorBody = e.getResponseBodyAsString();
                String errorMessage = "요청이 너무 큽니다. 대화가 길어져서 처리할 수 없습니다. 잠시 후 다시 시도하거나 새로운 세션을 시작해주세요.";
                
                if (errorBody != null && errorBody.contains("tokens per minute")) {
                    errorMessage = "Groq API 토큰 제한에 도달했습니다. 대화가 너무 길어져서 처리할 수 없습니다. 잠시 후 다시 시도하거나 새로운 세션을 시작해주세요.";
                } else if (errorBody != null && errorBody.contains("Request too large")) {
                    errorMessage = "요청이 너무 큽니다. 대화가 길어져서 처리할 수 없습니다. 잠시 후 다시 시도하거나 새로운 세션을 시작해주세요.";
                }
                
                throw new VoiceOrderException(errorMessage, e);
            } else if (e.getStatusCode().value() == 429) {
                String errorBody = e.getResponseBodyAsString();
                String errorMessage = "LLM API 사용량 제한에 도달했습니다.";
                
                // Groq API의 할당량 초과 오류인지 확인
                if (errorBody != null && errorBody.contains("insufficient_quota")) {
                    errorMessage = "Groq API 할당량이 부족합니다. Groq 대시보드에서 결제 정보를 확인하고 크레딧을 충전해주세요.";
                } else if (errorBody != null && errorBody.contains("rate_limit_exceeded")) {
                    errorMessage = "Groq API 요청 속도 제한에 도달했습니다. 잠시 후 다시 시도해주세요.";
                }
                
                throw new VoiceOrderException(errorMessage, e);
            }
            throw new VoiceOrderException("LLM API 호출 중 오류가 발생했습니다.", e);
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            LOGGER.error("LLM API 서버 오류: Status={}, Body={}", 
                e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new VoiceOrderException("LLM 서비스에 일시적인 문제가 있습니다. 잠시 후 다시 시도해주세요.", e);
        } catch (Exception e) {
            LOGGER.error("LLM 호출 실패: URL={}", apiUrl, e);
            throw new VoiceOrderException("상담원 응답을 생성하지 못했습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }

    private List<Map<String, String>> buildMessages(VoiceOrderSession session, String menuPromptBlock) throws Exception {
        List<Map<String, String>> messages = new ArrayList<>();
        String systemPrompt = buildSystemPrompt(session, menuPromptBlock);
        
        // System prompt 추가
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);
        
        // 대화 기록 추가 (최근 10개만 포함하여 토큰 제한 방지)
        // 최신 메시지부터 역순으로 정렬하여 최근 대화만 선택
        List<com.mrdabak.dinnerservice.voice.model.VoiceConversationMessage> allMessages = 
                new ArrayList<>(session.getMessages());
        
        // 타임스탬프 기준으로 정렬 (최신이 마지막)
        allMessages.sort(java.util.Comparator.comparing(
                com.mrdabak.dinnerservice.voice.model.VoiceConversationMessage::getTimestamp));
        
        // 최근 10개 메시지만 선택 (토큰 제한 고려: Groq on-demand tier는 6000 TPM 제한)
        // 시스템 프롬프트가 크므로 대화 기록을 더 줄임
        int maxMessages = 10;
        int startIndex = Math.max(0, allMessages.size() - maxMessages);
        List<com.mrdabak.dinnerservice.voice.model.VoiceConversationMessage> recentMessages = 
                allMessages.subList(startIndex, allMessages.size());
        
        recentMessages.forEach(msg -> {
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", msg.getRole());
            // 메시지 내용이 너무 길면 자르기 (각 메시지당 최대 500자)
            String content = msg.getContent();
            if (content != null && content.length() > 500) {
                content = content.substring(0, 500) + "...";
            }
            userMsg.put("content", content);
            messages.add(userMsg);
        });
        
        LOGGER.debug("대화 기록: 전체 {}개 중 최근 {}개만 API에 전송 (토큰 제한 방지)", 
                allMessages.size(), recentMessages.size());
        
        return messages;
    }

    private String buildSystemPrompt(VoiceOrderSession session, String menuPromptBlock) throws Exception {
        String stateJson = objectMapper.writeValueAsString(session.getCurrentState());
        return """
                당신은 미스터 대박 디너 서비스의 한국어 음성 주문 상담원입니다. 고객 이름: %s
                
                [기본 규칙]
                - 한국어만 사용. 자연스럽고 공손하게 대화.
                - 이미 말한 정보 반복 금지. 문맥으로 발음 보정 (예: "백언"→"베이컨").
                - 도메인 외 질문은 "그 질문은 제가 도와드리기 어렵지만, 주문을 이어서 도와드릴게요."로 거절.
                
                [필수 정보]
                dinnerType, servingStyle, deliveryDateTime, deliveryAddress, contactPhone
                needsMoreInfo에는 누락된 정보만 포함.
                
                [확정 규칙]
                - finalConfirmation=true면 확정 재요청 금지.
                - readyForConfirmation=true && finalConfirmation=false일 때만 한 번 확인 요청.
                - 고객이 "확정/주문해줘/좋아요/네" 등으로 말하면 finalConfirmation=true 설정.
                
                [배달 시간]
                - deliveryDateTime: "2025-12-05T20:00:00" 형식만 사용. 플레이스홀더 절대 금지.
                - "내일 모레" 등 모호한 표현은 명확한 날짜 요청.
                - "내일 오후 8시" → "2025-12-06T20:00:00" 변환.
                - 고객에게 말할 때는 원문 그대로 사용 ("배달 시간: 내일 오후 8시").
                
                [메뉴 요약]
                - 기본 메뉴 구성 + 변경 사항 모두 언급.
                - 예: "메뉴 구성: 에그 스크램블 x1, 베이컨 x1, 빵 x1, 스테이크 x1"
                
                [출력 형식]
                assistant_message: (한국어 텍스트만, JSON 포함 금지)
                
                order_state_json:
                ```json
                {
                  "dinnerType": "VALENTINE|FRENCH|ENGLISH|CHAMPAGNE_FEAST",
                  "servingStyle": "simple|grand|deluxe",
                  "menuAdjustments": [{"item":"baguette","quantity":6}],
                  "deliveryDateTime": "2025-12-05T20:00:00",
                  "deliveryAddress": "서울시 강남구 테헤란로 123",
                  "contactPhone": "010-1234-5678",
                  "specialRequests": "문 앞에 놓아주세요",
                  "readyForConfirmation": true,
                  "finalConfirmation": false,
                  "needsMoreInfo": [],
                  "summary": "발렌타인 디너, 그랜드 스타일, 모레 오후 8시 배달"
                }
                ```
                
                - menuAdjustments.item: champagne, wine, coffee, steak, salad, eggs, bacon, bread, baguette
                - 샴페인 축제 디너는 grand/deluxe만 가능.
                - 이미 설정된 정보는 반복 요청하지 말고 요약만 확인.

                [메뉴 카탈로그]
                %s
                
                [현재 주문 상태]
                %s
                """.formatted(session.getCustomerName(), menuPromptBlock, stateJson);
    }

    private VoiceAssistantResponse parseContent(String content) {
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(content);
        VoiceOrderState state = new VoiceOrderState();
        String assistantMessage = content;
        if (matcher.find()) {
            String json = matcher.group(1);
            assistantMessage = content.substring(0, matcher.start())
                    .replace("assistant_message:", "")
                    .trim();
            try {
                state = objectMapper.readValue(json, VoiceOrderState.class);
            } catch (Exception e) {
                LOGGER.warn("주문 상태 JSON 파싱 실패: {}", e.getMessage());
            }
        } else {
            assistantMessage = assistantMessage.replace("assistant_message:", "").trim();
        }
        
        // 한국어만 추출 (다른 언어 제거)
        assistantMessage = filterKoreanOnly(assistantMessage);
        
        return new VoiceAssistantResponse(assistantMessage, state, content);
    }
    
    /**
     * 외국어(중국어, 일본어 등) 섞임 제거 - 한국어 문장 유지
     */
    private String filterKoreanOnly(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // JSON 블록이나 코드 블록은 그대로 유지
        StringBuilder result = new StringBuilder();
        boolean inJsonBlock = false;
        boolean inCodeBlock = false;
        int jsonDepth = 0;
        int codeBlockCount = 0;
        
        String[] lines = text.split("\n");
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // JSON 블록이나 코드 블록 체크
            if (trimmedLine.contains("```json") || trimmedLine.contains("```")) {
                codeBlockCount++;
                result.append(line).append("\n");
                continue;
            }
            
            if (codeBlockCount > 0 && codeBlockCount % 2 == 1) {
                // 코드 블록 내부
                result.append(line).append("\n");
                continue;
            }
            
            // JSON 블록 내부 체크
            for (char c : trimmedLine.toCharArray()) {
                if (c == '{' || c == '[') jsonDepth++;
                if (c == '}' || c == ']') jsonDepth--;
            }
            
            if (jsonDepth > 0) {
                // JSON 내부
                result.append(line).append("\n");
                continue;
            }
            
            // 한국어 비율 체크 (한국어가 30% 이상이면 문장 유지)
            int koreanCharCount = 0;
            int totalCharCount = 0;
            
            for (char c : trimmedLine.toCharArray()) {
                if (!Character.isWhitespace(c)) {
                    totalCharCount++;
                    // 한글 범위: 가(0xAC00) ~ 힣(0xD7AF)
                    if (c >= '\uAC00' && c <= '\uD7AF') {
                        koreanCharCount++;
                    }
                }
            }
            
            // 한국어 비율이 30% 이상이거나, 전체가 짧은 경우(20자 이하) 유지
            if (totalCharCount == 0 || 
                (koreanCharCount * 100.0 / totalCharCount) >= 30 ||
                trimmedLine.length() <= 20) {
                result.append(line).append("\n");
            } else {
                // 외국어가 많은 문장은 제거
                LOGGER.debug("외국어 문장 필터링: {}", trimmedLine);
            }
        }
        
        String filtered = result.toString().replaceAll("\n+", "\n").trim();
        
        // 필터링 후 너무 짧아지면 원본 반환 (최소 50%는 유지되어야 함)
        if (filtered.length() < text.length() * 0.5 && text.length() > 50) {
            LOGGER.warn("필터링 결과가 너무 짧음. 원본 반환. 원본: {}", text.substring(0, Math.min(100, text.length())));
            return text;
        }
        
        return filtered.isEmpty() ? text : filtered;
    }
}
