package com.mrdabak.dinnerservice.voice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrdabak.dinnerservice.repository.UserRepository;
import com.mrdabak.dinnerservice.repository.order.OrderRepository;
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
    // 여러 JSON 패턴 지원
    private static final Pattern JSON_BLOCK_PATTERN_1 =
            Pattern.compile("order_state_json\\s*:?\\s*```json\\s*(\\{.*?})\\s*```", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern JSON_BLOCK_PATTERN_2 =
            Pattern.compile("```json\\s*(\\{.*?})\\s*```", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern JSON_INLINE_PATTERN =
            Pattern.compile("\\{\\s*\"dinnerType\"[^}]*\\}", Pattern.DOTALL);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String apiKey;
    private final String modelName;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public VoiceOrderAssistantClient(RestTemplate restTemplate,
                                     ObjectMapper objectMapper,
                                     @Value("${voice.llm.api-url:https://api.groq.com/openai/v1/chat/completions}") String apiUrl,
                                     @Value("${voice.llm.api-key:}") String apiKey,
                                     @Value("${voice.llm.model:llama-3.3-70b-versatile}") String modelName,
                                     UserRepository userRepository,
                                     OrderRepository orderRepository) {
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
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
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
        
        // 개인정보 동의 여부 확인 (세션에 저장된 고객 정보가 null이면 비동의)
        boolean hasConsent = session.getCustomerName() != null && 
                            session.getCustomerDefaultAddress() != null && 
                            session.getCustomerPhone() != null;
        
        String customerType = hasConsent ? "개인정보 동의 계정" : "개인정보 비동의 계정";
        String customerName = hasConsent && session.getCustomerName() != null ? session.getCustomerName() : "고객님";
        
        String defaultAddress = session.getCustomerDefaultAddress();
        String defaultPhone = session.getCustomerPhone();
        
        String consentInstructions = hasConsent ? 
            """
            [개인정보 동의 계정]
            - 고객의 배달 주소와 전화번호 정보가 저장되어 있습니다.
            - 주문 정보 수집 시:
              1. 배달 주소: 먼저 저장된 주소("%s")를 제시하고 고객에게 "이 주소로 배달하시겠어요?"라고 물어보세요.
                 - 고객이 "네/좋아요/맞아요" 등으로 동의하면 deliveryAddress에 저장된 주소를 그대로 사용.
                 - 고객이 "아니요/다른 곳/변경" 등으로 거부하면 새로운 주소를 입력받으세요.
              2. 연락처: 먼저 저장된 전화번호("%s")를 제시하고 고객에게 "이 번호로 연락드려도 될까요?"라고 물어보세요.
                 - 고객이 동의하면 contactPhone에 저장된 번호를 그대로 사용.
                 - 고객이 거부하면 새로운 번호를 입력받으세요.
            """ : 
            """
            [개인정보 비동의 계정]
            - 고객의 개인정보는 저장되지 않았습니다.
            - 주문 완료를 위해 배달 주소(deliveryAddress)와 배달 시간(deliveryDateTime)은 반드시 수집해야 합니다.
            - 연락처(contactPhone)는 선택 사항입니다.
            """;
        
        consentInstructions = hasConsent ? 
            String.format(consentInstructions, 
                defaultAddress != null ? defaultAddress : "(저장된 주소 없음)",
                defaultPhone != null ? defaultPhone : "(저장된 번호 없음)") : 
            consentInstructions;
        
        // 할인 혜택 확인
        boolean loyaltyEligible = false;
        try {
            if (hasConsent && session.getUserId() != null) {
                var userOpt = userRepository.findById(session.getUserId());
                if (userOpt.isPresent()) {
                    var user = userOpt.get();
                    boolean consentGiven = Boolean.TRUE.equals(user.getConsent());
                    boolean loyaltyConsent = Boolean.TRUE.equals(user.getLoyaltyConsent());
                    long deliveredOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(session.getUserId()).stream()
                            .filter(o -> "delivered".equalsIgnoreCase(o.getStatus()))
                            .count();
                    loyaltyEligible = consentGiven && loyaltyConsent && deliveredOrders >= 4;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("할인 혜택 확인 중 오류 (무시): {}", e.getMessage());
        }
        
        String discountInfo = loyaltyEligible ? 
            "\n- 단골 고객 할인: 이 고객은 10%% 단골 고객 할인 대상입니다. 주문 완료 시 할인 안내를 포함하세요." : 
            "";
        
        return """
                당신은 미스터 대박 디너 서비스의 한국어 음성 주문 상담원입니다. 
                고객 이름: %s
                고객 유형: %s
                
                %s
                
                [기본 규칙]
                - 한국어만 사용. 자연스럽고 공손하게 대화.
                - 이미 말한 정보 반복 금지. 문맥으로 발음 보정 (예: "백언"→"베이컨").
                - 도메인 외 질문은 "그 질문은 제가 도와드리기 어렵지만, 주문을 이어서 도와드릴게요."로 거절.
                
                [필수 정보]
                dinnerType, servingStyle, deliveryDateTime, deliveryAddress
                %s인 경우 contactPhone은 필수가 아닙니다.
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
                
                [주문 요약표 실시간 반영 - 매우 중요]
                - 고객이 주문 정보를 제공할 때마다 매 응답에서 order_state_json을 업데이트하세요.
                - 주문 정보가 하나라도 변경되면 즉시 JSON의 해당 필드를 업데이트하고 readyForConfirmation 상태를 업데이트하세요.
                - 주문이 완료되면(readyForCheckout=true) 반드시 주문 요약표를 완전한 JSON 형식으로 제공하세요.
                - 주문 완료 시 "주문 요약표: [메뉴 정보, 배달 정보 등]" 형식으로 요약을 명확히 제시하세요.%s
                
                [메뉴 요약]
                - 기본 메뉴 구성 + 변경 사항 모두 언급.
                - 예: "메뉴 구성: 에그 스크램블 x1, 베이컨 x1, 빵 x1, 스테이크 x1"
                - 주문 완료 시 주문 요약표를 반드시 포함하세요.
                
                [출력 형식 - 필수]
                반드시 다음 형식으로 응답하세요. JSON은 매 응답마다 필수입니다.
                
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
                
                중요: order_state_json 블록은 반드시 포함해야 합니다. JSON이 없으면 시스템이 작동하지 않습니다.
                
                [메뉴 항목 정확한 매핑 - 반드시 다음 키만 사용]
                - menuAdjustments.item 필드에는 반드시 다음 중 하나만 사용:
                  * "champagne" (샴페인, 샴페인주 등)
                  * "wine" (와인, 포도주 등)
                  * "coffee" (커피)
                  * "steak" (스테이크, 고기 등)
                  * "salad" (샐러드, 야채 등)
                  * "eggs" (에그, 계란, 스크램블 등)
                  * "bacon" (베이컨)
                  * "bread" (빵, 식빵 등)
                  * "baguette" (바게트, 바게트빵 등)
                - 고객이 "계란 2개 추가"라고 하면 {"item":"eggs","quantity":2,"action":"add"} 형식으로 작성
                - 고객이 "베이컨 빼줘"라고 하면 {"item":"bacon","quantity":0} 또는 해당 항목 제거
                - 고객이 "모든 구성품 2개씩", "전체 2개", "각각 2개" 등으로 명시적으로 수량을 지정하면 {"item":"...","quantity":2,"action":"set"} 형식으로 작성 (절대값)
                - 고객이 "베이컨만 6개"처럼 특정 항목의 수량을 명시하면 {"item":"bacon","quantity":6,"action":"set"} 형식으로 작성 (절대값)
                - quantity는 반드시 숫자 (0 이상)
                - action 규칙:
                  * "add", "increase", "추가", "증가" → 기존 수량에 추가
                  * "set", "change", "설정", "변경" 또는 action 없음 → 절대값으로 설정 (기본 수량 무시)
                  * "remove", "delete", "제거", "삭제", "빼" → 항목 제거
                
                - 샴페인 축제 디너는 grand/deluxe만 가능.
                - 이미 설정된 정보는 반복 요청하지 말고 요약만 확인.

                [메뉴 카탈로그]
                %s
                
                [현재 주문 상태]
                %s
                """.formatted(customerName, customerType, consentInstructions, 
                             hasConsent ? "" : "개인정보 비동의", discountInfo, menuPromptBlock, stateJson);
    }

    private VoiceAssistantResponse parseContent(String content) {
        VoiceOrderState state = new VoiceOrderState();
        String assistantMessage = content;
        String extractedJson = null;
        
        // 원본 응답 로깅 (디버깅용)
        LOGGER.debug("LLM 원본 응답 (처음 500자): {}", 
                content.length() > 500 ? content.substring(0, 500) + "..." : content);
        
        // 여러 패턴으로 JSON 추출 시도
        Matcher matcher1 = JSON_BLOCK_PATTERN_1.matcher(content);
        Matcher matcher2 = JSON_BLOCK_PATTERN_2.matcher(content);
        Matcher matcherInline = JSON_INLINE_PATTERN.matcher(content);
        
        if (matcher1.find()) {
            extractedJson = matcher1.group(1);
            assistantMessage = content.substring(0, matcher1.start())
                    .replace("assistant_message:", "")
                    .trim();
            LOGGER.debug("JSON 패턴 1로 추출 성공");
        } else if (matcher2.find()) {
            extractedJson = matcher2.group(1);
            // JSON 블록 이전 부분을 메시지로 추출
            int jsonStart = matcher2.start();
            assistantMessage = content.substring(0, jsonStart)
                    .replace("assistant_message:", "")
                    .trim();
            LOGGER.debug("JSON 패턴 2로 추출 성공");
        } else if (matcherInline.find()) {
            // 인라인 JSON 추출 시도
            int jsonStart = matcherInline.start();
            int jsonEnd = findMatchingBrace(content, jsonStart);
            if (jsonEnd > jsonStart) {
                extractedJson = content.substring(jsonStart, jsonEnd + 1);
                assistantMessage = content.substring(0, jsonStart)
                        .replace("assistant_message:", "")
                        .trim();
                LOGGER.debug("JSON 패턴 3 (인라인)으로 추출 성공");
            }
        }
        
        // JSON 파싱 시도
        if (extractedJson != null && !extractedJson.isBlank()) {
            try {
                // JSON 정리 (주석 제거, 불필요한 공백 제거)
                String cleanedJson = cleanJsonString(extractedJson);
                state = objectMapper.readValue(cleanedJson, VoiceOrderState.class);
                LOGGER.debug("JSON 파싱 성공: dinnerType={}, servingStyle={}, menuAdjustments={}", 
                        state.getDinnerType(), state.getServingStyle(), 
                        state.getMenuAdjustments() != null ? state.getMenuAdjustments().size() : 0);
            } catch (Exception e) {
                LOGGER.warn("주문 상태 JSON 파싱 실패: {} - 원본 JSON: {}", e.getMessage(), 
                        extractedJson.length() > 200 ? extractedJson.substring(0, 200) + "..." : extractedJson);
                // 부분 파싱 시도 (일부 필드만 추출)
                try {
                    JsonNode jsonNode = objectMapper.readTree(extractedJson);
                    if (jsonNode.has("dinnerType")) {
                        state.setDinnerType(jsonNode.get("dinnerType").asText());
                    }
                    if (jsonNode.has("servingStyle")) {
                        state.setServingStyle(jsonNode.get("servingStyle").asText());
                    }
                    if (jsonNode.has("menuAdjustments") && jsonNode.get("menuAdjustments").isArray()) {
                        List<com.mrdabak.dinnerservice.voice.model.VoiceOrderItem> items = new ArrayList<>();
                        for (JsonNode itemNode : jsonNode.get("menuAdjustments")) {
                            try {
                                com.mrdabak.dinnerservice.voice.model.VoiceOrderItem item = 
                                        objectMapper.treeToValue(itemNode, com.mrdabak.dinnerservice.voice.model.VoiceOrderItem.class);
                                if (item != null) {
                                    items.add(item);
                                }
                            } catch (Exception itemEx) {
                                LOGGER.debug("메뉴 항목 파싱 실패 (무시): {}", itemEx.getMessage());
                            }
                        }
                        state.setMenuAdjustments(items);
                    }
                    if (jsonNode.has("deliveryDateTime")) {
                        state.setDeliveryDateTime(jsonNode.get("deliveryDateTime").asText());
                    }
                    if (jsonNode.has("deliveryAddress")) {
                        state.setDeliveryAddress(jsonNode.get("deliveryAddress").asText());
                    }
                    if (jsonNode.has("contactPhone")) {
                        state.setContactPhone(jsonNode.get("contactPhone").asText());
                    }
                    LOGGER.info("부분 JSON 파싱 성공: 일부 필드만 추출됨");
                } catch (Exception partialEx) {
                    LOGGER.warn("부분 JSON 파싱도 실패: {}", partialEx.getMessage());
                }
            }
        } else {
            LOGGER.warn("JSON 블록을 찾을 수 없습니다. LLM이 JSON을 생성하지 않았을 수 있습니다.");
            LOGGER.debug("응답 내용 (처음 1000자): {}", 
                    content.length() > 1000 ? content.substring(0, 1000) + "..." : content);
            assistantMessage = assistantMessage.replace("assistant_message:", "").trim();
        }
        
        // 한국어만 추출 (다른 언어 제거)
        assistantMessage = filterKoreanOnly(assistantMessage);
        
        return new VoiceAssistantResponse(assistantMessage, state, content);
    }
    
    /**
     * JSON 문자열 정리 (주석 제거, 불필요한 공백 제거)
     */
    private String cleanJsonString(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }
        // 한 줄 주석 제거 (// ...)
        json = json.replaceAll("//.*", "");
        // 여러 줄 주석 제거 (/* ... */)
        json = json.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        // 줄바꿈과 불필요한 공백 정리
        json = json.replaceAll("\\s+", " ").trim();
        return json;
    }
    
    /**
     * 중괄호 매칭하여 JSON 블록 끝 찾기
     */
    private int findMatchingBrace(String content, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escapeNext = false;
        
        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (escapeNext) {
                escapeNext = false;
                continue;
            }
            
            if (c == '\\') {
                escapeNext = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (inString) {
                continue;
            }
            
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        
        return -1; // 매칭 실패
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
