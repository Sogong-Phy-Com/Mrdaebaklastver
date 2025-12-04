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
                                     @Value("${voice.llm.model:llama-3.1-8b-instant}") String modelName) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    public VoiceAssistantResponse generateResponse(VoiceOrderSession session, String menuPromptBlock) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new VoiceOrderException("LLM API 키가 설정되지 않았습니다. 환경 변수 VOICE_LLM_API_KEY를 설정해주세요.");
        }
        
        try {
            List<Map<String, String>> messages = buildMessages(session, menuPromptBlock);
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", modelName);
            payload.put("messages", messages);
            payload.put("temperature", 0.3); // 일관된 한국어 응답
            payload.put("max_tokens", 1000); // 응답 길이 제한

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl, new HttpEntity<>(payload, headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                LOGGER.error("LLM API 응답 오류: Status={}, Body={}", 
                    response.getStatusCode(), response.getBody());
                throw new VoiceOrderException("대화형 AI 응답이 올바르지 않습니다.");
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            
            // Groq API 응답 형식 파싱 (OpenAI 호환)
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
            } else if (e.getStatusCode().value() == 429) {
                throw new VoiceOrderException("LLM API 사용량 제한에 도달했습니다. 잠시 후 다시 시도해주세요.", e);
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
        
        // 대화 기록 추가
        session.getMessages().forEach(msg -> {
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", msg.getRole());
            userMsg.put("content", msg.getContent());
            messages.add(userMsg);
        });
        
        return messages;
    }

    private String buildSystemPrompt(VoiceOrderSession session, String menuPromptBlock) throws Exception {
        String stateJson = objectMapper.writeValueAsString(session.getCurrentState());
        String defaultAddress = session.getCustomerDefaultAddress() != null ? session.getCustomerDefaultAddress() : "";
        String defaultPhone = session.getCustomerPhone() != null ? session.getCustomerPhone() : "";
        
        // 현재 날짜/시간 정보
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDate today = now.toLocalDate();
        java.time.LocalDate tomorrow = today.plusDays(1);
        String currentDate = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String currentTime = now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        String currentDayOfWeek = now.format(java.time.format.DateTimeFormatter.ofPattern("EEEE", java.util.Locale.KOREAN));
        String tomorrowDate = tomorrow.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        return """
                당신은 미스터 대박 디너 서비스의 한국어 음성 주문 상담원입니다.
                
                [기본 규칙]
                - 항상 존댓말을 사용하고 고객 이름(%s)으로 호칭하세요.
                - 반드시 한국어만 사용하세요.
                - 응답은 자연스럽게 줄바꿈하여 읽기 쉽게 작성하세요.
                
                [현재 날짜/시간 정보]
                - 현재 날짜: %s (%s)
                - 현재 시간: %s
                
                [필수 정보]
                1. 디너 선택 2. 서빙 스타일 3. 배달 날짜/시간 4. 배달 주소 5. 전화번호
                
                [날짜/시간 처리 규칙 - 매우 중요]
                - 고객이 "내일 오후 8시", "내일 20시", "다음주 월요일", "모레 저녁 7시" 같은 상대적 표현을 사용하면, 현재 날짜/시간을 기준으로 실제 날짜/시간을 계산하여 변환하세요.
                - 날짜와 시간을 반드시 분리하여 저장하세요:
                  * deliveryDate: "YYYY-MM-DD" 형식 (예: "%s")
                  * deliveryTime: "HH:mm" 형식 (예: "20:00")
                - [중요] 과거 날짜/시간은 주문할 수 없습니다:
                  * 오늘 이전 날짜는 절대 불가 (예: 어제, 지난주 등)
                  * 오늘 날짜인 경우, 현재 시간 이전 시간은 불가
                  * 반드시 현재 날짜/시간 이후의 날짜/시간만 주문 가능
                  * 과거 날짜를 요청하면 친절하게 "과거 날짜는 주문하실 수 없습니다. 오늘 이후 날짜를 선택해주세요"라고 안내
                - 시간 표현 변환 - 반드시 24시간 형식으로 변환:
                  * "오후 8시", "오후8시", "저녁 8시" → "20:00" (8 + 12 = 20)
                  * "오후 2시" → "14:00" (2 + 12 = 14)
                  * "오전 10시" → "10:00" (변환 없음)
                  * "20시", "20:00" → "20:00" (그대로)
                  * "오후 8시 30분" → "20:30"
                  * "점심", "정오" → "12:00"
                - 중요 예시: "내일 오후 8시" 표현을 받으면:
                  * deliveryDate: "내일" → 현재 날짜가 %s이면 "%s"
                  * deliveryTime: "오후 8시" → 반드시 "20:00" (8 + 12 = 20)
                - "오후 X시"는 항상 (X + 12)로 변환: 오후 8시 = 20:00, 오후 7시 = 19:00, 오후 2시 = 14:00
                - "오전 X시"는 그대로: 오전 8시 = 08:00, 오전 10시 = 10:00
                
                [중요 규칙]
                - 배달 주소나 전화번호를 고객이 별도로 언급하지 않으면 회원 정보를 사용합니다.
                  * 회원 기본 주소: %s
                  * 회원 기본 전화번호: %s
                - menuAdjustments.item: champagne, wine, coffee, steak, salad, eggs, bacon, bread, baguette
                - 샴페인 축제 디너는 그랜드/디럭스만 가능합니다.
                
                [응답 형식]
                assistant_message:
                (고객에게 자연스럽게 들려줄 멘트)

                order_state_json:
                ```json
                {
                  "dinnerType": "VALENTINE|FRENCH|ENGLISH|CHAMPAGNE_FEAST",
                  "servingStyle": "simple|grand|deluxe",
                  "menuAdjustments": [{"item":"baguette","quantity":6}],
                  "deliveryDate": "YYYY-MM-DD",
                  "deliveryTime": "HH:mm",
                  "deliveryAddress": "주소를 언급하지 않으면 회원 주소 사용",
                  "contactPhone": "전화번호를 언급하지 않으면 회원 전화번호 사용",
                  "readyForConfirmation": true|false,
                  "needsMoreInfo": ["deliveryAddress"],
                  "summary": "한 줄 요약"
                }
                ```
                
                [시간 변환 예시 - 반드시 참고하고 따르세요]
                고객이 "오후 8시", "오후8시"라고 말하면:
                  - deliveryTime 필드에 반드시 "20:00"을 넣으세요 (문자열 "오후 8시"가 아니라 숫자 "20:00")
                  - "오후" + 시간 = (시간 + 12)로 계산
                  - 예: "오후 8시" = 8 + 12 = "20:00"
                  - 예: "오후 7시" = 7 + 12 = "19:00"
                  - 예: "오후 2시" = 2 + 12 = "14:00"
                
                실제 변환 예시:
                  - "내일 오후 8시" → deliveryDate: "%s", deliveryTime: "20:00" (NOT "오후 8시")
                  - "오후 7시" → deliveryTime: "19:00" (NOT "오후 7시")
                  - "20시" → deliveryTime: "20:00"
                  - "오전 10시" → deliveryTime: "10:00"
                
                중요: deliveryTime 필드에는 반드시 "HH:mm" 형식(예: "20:00")만 넣고, "오후 8시" 같은 텍스트는 넣지 마세요!

                [메뉴 카탈로그]
                %s

                [현재 주문 상태]
                %s
                """.formatted(
                    session.getCustomerName(),        // 1. 고객 이름
                    currentDate,                       // 2. 현재 날짜
                    currentDayOfWeek,                  // 3. 요일
                    currentTime,                       // 4. 현재 시간
                    tomorrowDate,                      // 5. deliveryDate 예시
                    currentDate,                       // 6. 현재 날짜가
                    tomorrowDate,                      // 7. 내일 날짜
                    defaultAddress,                    // 8. 회원 기본 주소
                    defaultPhone,                      // 9. 회원 기본 전화번호
                    tomorrowDate,                      // 10. "내일 오후 8시" 예시
                    menuPromptBlock,                   // 11. 메뉴 카탈로그
                    stateJson                          // 12. 현재 주문 상태
                );
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
        
        // 한국어만 추출 및 줄바꿈 개선
        assistantMessage = filterKoreanOnly(assistantMessage);
        assistantMessage = formatMessage(assistantMessage);
        
        return new VoiceAssistantResponse(assistantMessage, state, content);
    }
    
    /**
     * 메시지 포맷팅 - 자연스러운 줄바꿈
     */
    private String formatMessage(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 문장 끝에 줄바꿈 추가 (마침표, 물음표, 느낌표 뒤)
        text = text.replaceAll("([.?!])([^\\n])", "$1\n$2");
        
        // 연속된 공백 정리
        text = text.replaceAll(" +", " ");
        
        // 연속된 줄바꿈을 최대 2개로 제한
        text = text.replaceAll("\n{3,}", "\n\n");
        
        return text.trim();
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
