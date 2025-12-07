package com.mrdabak.dinnerservice.voice.service;

import com.mrdabak.dinnerservice.voice.model.VoiceOrderItem;
import com.mrdabak.dinnerservice.voice.model.VoiceOrderState;
import com.mrdabak.dinnerservice.voice.util.DomainVocabularyNormalizer;
import com.mrdabak.dinnerservice.voice.util.RelativeDateParser;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class VoiceOrderStateMerger {

    private static final List<DateTimeFormatter> SUPPORTED_DATETIME_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));

    private final DomainVocabularyNormalizer normalizer;

    public VoiceOrderStateMerger(DomainVocabularyNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public void merge(VoiceOrderState current, VoiceOrderState incoming) {
        if (incoming == null) {
            return;
        }

        Optional.ofNullable(incoming.getDinnerType())
                .map(normalizer::normalizeDinnerType)
                .orElseGet(() -> Optional.ofNullable(incoming.getDinnerType()))
                .ifPresent(token -> current.setDinnerType(token));

        Optional.ofNullable(incoming.getServingStyle())
                .map(normalizer::normalizeServingStyle)
                .orElseGet(() -> Optional.ofNullable(incoming.getServingStyle()))
                .ifPresent(style -> current.setServingStyle(style.toLowerCase(Locale.ROOT)));

        if (incoming.getMenuAdjustments() != null && !incoming.getMenuAdjustments().isEmpty()) {
            // 기존 메뉴 조정사항을 가져오거나 새로 생성
            List<VoiceOrderItem> existingAdjustments = current.getMenuAdjustments() != null 
                    ? new ArrayList<>(current.getMenuAdjustments()) 
                    : new ArrayList<>();
            
            // 기존 항목들을 맵으로 변환 (key 또는 name으로 인덱싱)
            Map<String, VoiceOrderItem> adjustmentMap = new LinkedHashMap<>();
            for (VoiceOrderItem existing : existingAdjustments) {
                String key = existing.getKey() != null ? existing.getKey() : existing.getName();
                if (key != null) {
                    adjustmentMap.put(key, existing);
                }
            }
            
            // 새로운 조정사항을 병합
            for (VoiceOrderItem item : incoming.getMenuAdjustments()) {
                if (item == null) continue;
                
                // key와 name 모두 정규화 시도
                String rawKey = Optional.ofNullable(item.getKey()).orElse("");
                String rawName = Optional.ofNullable(item.getName()).orElse("");
                
                // 정규화 시도 (key 우선, 없으면 name 사용)
                String itemKey = null;
                if (!rawKey.isBlank()) {
                    itemKey = normalizer.normalizeMenuItemKey(rawKey)
                            .orElse(rawKey.toLowerCase().trim());
                }
                if (itemKey == null && !rawName.isBlank()) {
                    itemKey = normalizer.normalizeMenuItemKey(rawName)
                            .orElse(rawName.toLowerCase().trim());
                }
                
                // 여전히 null이면 건너뛰기
                if (itemKey == null || itemKey.isBlank()) {
                    continue;
                }
                
                // 기존 항목 찾기 (정규화된 key로)
                VoiceOrderItem existing = adjustmentMap.get(itemKey);
                
                // action이 "remove" 또는 "delete"인 경우 항목 제거
                if (item.getAction() != null && 
                    (item.getAction().toLowerCase().contains("remove") || 
                     item.getAction().toLowerCase().contains("delete") ||
                     item.getAction().toLowerCase().contains("제거") ||
                     item.getAction().toLowerCase().contains("삭제") ||
                     item.getAction().toLowerCase().contains("빼"))) {
                    adjustmentMap.remove(itemKey);
                    continue;
                }
                
                // quantity가 0 이하인 경우 제거
                if (item.getQuantity() != null && item.getQuantity() <= 0) {
                    adjustmentMap.remove(itemKey);
                    continue;
                }
                
                // action이 "add" 또는 "increase"인 경우 기존 수량에 추가
                if (item.getAction() != null && 
                    (item.getAction().toLowerCase().contains("add") || 
                     item.getAction().toLowerCase().contains("increase") ||
                     item.getAction().toLowerCase().contains("추가") ||
                     item.getAction().toLowerCase().contains("증가"))) {
                    int currentQuantity = existing != null && existing.getQuantity() != null 
                            ? existing.getQuantity() 
                            : 0;
                    int addQuantity = item.getQuantity() != null ? item.getQuantity() : 1;
                    VoiceOrderItem newItem = new VoiceOrderItem();
                    newItem.setKey(itemKey);
                    newItem.setName(item.getName() != null && !item.getName().isBlank() ? item.getName() : itemKey);
                    newItem.setQuantity(currentQuantity + addQuantity);
                    newItem.setAction(item.getAction());
                    adjustmentMap.put(itemKey, newItem);
                } else {
                    // action이 없거나 "set", "change"인 경우 수량을 직접 설정
                    VoiceOrderItem clone = new VoiceOrderItem();
                    clone.setQuantity(item.getQuantity() != null ? item.getQuantity() : 1);
                    clone.setAction(item.getAction());
                    clone.setName(item.getName() != null && !item.getName().isBlank() ? item.getName() : itemKey);
                    clone.setKey(itemKey);
                    adjustmentMap.put(itemKey, clone);
                }
            }
            
            current.setMenuAdjustments(new ArrayList<>(adjustmentMap.values()));
        }

        // 배달 날짜/시간 처리
        if (incoming.getDeliveryDateTime() != null && !incoming.getDeliveryDateTime().isBlank()) {
            parseDateTime(incoming.getDeliveryDateTime()).ifPresent(dt -> {
                // 과거 날짜/시간 검증
                validateFutureDateTime(dt);
                current.setDeliveryDate(dt.toLocalDate().toString());
                current.setDeliveryTime(dt.toLocalTime().withSecond(0).withNano(0).toString());
                current.setDeliveryDateTime(dt.toString());
            });
        } else {
            // 상대적 날짜/시간 표현 처리
            String deliveryDate = incoming.getDeliveryDate();
            String deliveryTime = incoming.getDeliveryTime();
            
            // deliveryDate에 시간 정보가 포함된 경우 분리 (예: "내일 오후 8시")
            if (deliveryDate != null && deliveryTime == null) {
                String[] dateTimeParts = extractDateAndTime(deliveryDate);
                if (dateTimeParts != null) {
                    deliveryDate = dateTimeParts[0];
                    deliveryTime = dateTimeParts[1];
                }
            }
            
            // 상대적 날짜/시간을 실제 날짜/시간으로 변환
            String convertedDateTime = convertRelativeDateTime(deliveryDate, deliveryTime);
            
            if (convertedDateTime != null) {
                // YYYY-MM-DD HH:mm 형식으로 파싱
                try {
                    LocalDateTime dt = LocalDateTime.parse(convertedDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    // 과거 날짜/시간 검증
                    validateFutureDateTime(dt);
                    current.setDeliveryDate(dt.toLocalDate().toString());
                    current.setDeliveryTime(dt.toLocalTime().withSecond(0).withNano(0).toString());
                } catch (IllegalArgumentException e) {
                    // 과거 날짜/시간인 경우 예외를 다시 던짐 (호출자가 처리하도록)
                    throw e;
                } catch (Exception e) {
                    // 파싱 실패 시 기존 로직 사용
                    if (deliveryDate != null) {
                        String convertedDate = convertRelativeDate(deliveryDate);
                        if (convertedDate != null) {
                            validateFutureDate(convertedDate);
                            current.setDeliveryDate(convertedDate);
                        }
                    }
                    if (deliveryTime != null) {
                        current.setDeliveryTime(convertRelativeTime(deliveryTime));
                    }
                }
            } else {
                // 상대적 표현이 아닌 경우 기존 로직
                if (deliveryDate != null) {
                    String convertedDate = convertRelativeDate(deliveryDate);
                    if (convertedDate != null) {
                        validateFutureDate(convertedDate);
                        current.setDeliveryDate(convertedDate);
                    }
                }
                if (deliveryTime != null) {
                    current.setDeliveryTime(convertRelativeTime(deliveryTime));
                }
            }
        }

        // 배달 주소: 명시적으로 언급한 경우만 업데이트, 없으면 회원 정보 사용 (호출자에서 처리)
        if (incoming.getDeliveryAddress() != null && !incoming.getDeliveryAddress().isBlank()) {
            String address = incoming.getDeliveryAddress().trim();
            // "회원 주소 사용" 같은 키워드가 아니면 실제 주소로 인식
            if (!address.matches("(?i).*(회원.*주소|기본.*주소|동일|같음).*")) {
                current.setDeliveryAddress(address);
            }
        }

        // 전화번호: 명시적으로 언급한 경우만 업데이트, 없으면 회원 정보 사용 (호출자에서 처리)
        if (incoming.getContactPhone() != null && !incoming.getContactPhone().isBlank()) {
            String phone = incoming.getContactPhone().trim();
            // "회원 전화번호 사용" 같은 키워드가 아니면 실제 전화번호로 인식
            if (!phone.matches("(?i).*(회원.*전화|기본.*전화|동일|같음).*")) {
                current.setContactPhone(phone);
            }
        }
        if (incoming.getContactName() != null) {
            current.setContactName(incoming.getContactName());
        }
        if (incoming.getSpecialRequests() != null) {
            current.setSpecialRequests(incoming.getSpecialRequests());
        }
        if (incoming.getReadyForConfirmation() != null) {
            current.setReadyForConfirmation(incoming.getReadyForConfirmation());
        }
        if (incoming.getFinalConfirmation() != null) {
            current.setFinalConfirmation(incoming.getFinalConfirmation());
        }
        if (incoming.getNeedsMoreInfo() != null && !incoming.getNeedsMoreInfo().isEmpty()) {
            current.setNeedsMoreInfo(incoming.getNeedsMoreInfo());
        }
    }

    private Optional<LocalDateTime> parseDateTime(String raw) {
        for (DateTimeFormatter formatter : SUPPORTED_DATETIME_FORMATS) {
            try {
                return Optional.of(LocalDateTime.parse(raw.trim(), formatter));
            } catch (Exception ignored) { }
        }
        return Optional.empty();
    }
    
    /**
     * 상대적 날짜/시간 표현을 실제 날짜/시간으로 변환
     */
    private String convertRelativeDateTime(String dateExpression, String timeExpression) {
        if (dateExpression == null || dateExpression.isBlank()) {
            return null;
        }
        
        // 이미 YYYY-MM-DD 형식이면 변환 불필요
        try {
            LocalDate.parse(dateExpression, DateTimeFormatter.ISO_LOCAL_DATE);
            return null; // 이미 올바른 형식
        } catch (Exception e) {
            // 상대적 표현이거나 잘못된 형식
        }
        
        return RelativeDateParser.parseRelativeDateTime(dateExpression, timeExpression);
    }
    
    /**
     * 상대적 날짜를 실제 날짜로 변환
     */
    private String convertRelativeDate(String dateExpression) {
        if (dateExpression == null || dateExpression.isBlank()) {
            return null;
        }
        
        // 이미 YYYY-MM-DD 형식이면 그대로 반환
        try {
            LocalDate.parse(dateExpression, DateTimeFormatter.ISO_LOCAL_DATE);
            return dateExpression;
        } catch (Exception e) {
            // 상대적 표현 변환 시도
        }
        
        String converted = RelativeDateParser.parseRelativeDateTime(dateExpression, null);
        if (converted != null) {
            return converted.substring(0, 10); // YYYY-MM-DD 부분만 추출
        }
        
        return dateExpression; // 변환 실패 시 원본 반환
    }
    
    /**
     * 상대적 시간을 실제 시간으로 변환
     */
    private String convertRelativeTime(String timeExpression) {
        if (timeExpression == null || timeExpression.isBlank()) {
            return null;
        }
        
        // 이미 HH:mm 형식이면 그대로 반환
        try {
            LocalTime.parse(timeExpression, DateTimeFormatter.ofPattern("HH:mm"));
            return timeExpression;
        } catch (Exception e) {
            // 상대적 표현 변환 시도
        }
        
        String converted = RelativeDateParser.parseRelativeDateTime("오늘", timeExpression);
        if (converted != null) {
            return converted.substring(11); // HH:mm 부분만 추출
        }
        
        return timeExpression; // 변환 실패 시 원본 반환
    }
    
    /**
     * 날짜 표현에서 날짜와 시간을 분리 (예: "내일 오후 8시" -> ["내일", "오후 8시"])
     */
    private String[] extractDateAndTime(String dateTimeExpression) {
        if (dateTimeExpression == null || dateTimeExpression.isBlank()) {
            return null;
        }
        
        String lower = dateTimeExpression.toLowerCase().trim();
        
        // 시간 패턴 찾기: "오후 8시", "오전 10시", "20시", "저녁 7시", "오후8시" 등
        java.util.regex.Pattern timePattern = java.util.regex.Pattern.compile(
            "\\s+((오전|오후|am|pm)\\s*(\\d{1,2})\\s*시|(\\d{1,2})\\s*시|(저녁|점심|낮|아침)|(\\d{2}):(\\d{2}))"
        );
        java.util.regex.Matcher timeMatcher = timePattern.matcher(lower);
        
        if (timeMatcher.find()) {
            int timeStart = timeMatcher.start();
            String datePart = dateTimeExpression.substring(0, timeStart).trim();
            String timePart = dateTimeExpression.substring(timeStart).trim();
            
            // 날짜 부분에 "내일", "모레" 등이 있는지 확인
            if (!datePart.isEmpty() && !timePart.isEmpty() && 
                (datePart.contains("내일") || datePart.contains("모레") || datePart.contains("오늘") ||
                 datePart.matches(".*\\d{4}-\\d{2}-\\d{2}.*") || datePart.contains("요일"))) {
                return new String[]{datePart, timePart};
            }
        }
        
        return null; // 분리 실패
    }
    
    /**
     * 과거 날짜/시간 검증 - 현재 시간 이후만 허용
     */
    private void validateFutureDateTime(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        if (dateTime.isBefore(now) || dateTime.isEqual(now)) {
            throw new IllegalArgumentException("과거 날짜/시간은 주문하실 수 없습니다. 현재 시간 이후의 날짜/시간을 선택해주세요.");
        }
    }
    
    /**
     * 과거 날짜 검증 - 날짜만 비교
     */
    private void validateFutureDate(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate today = LocalDate.now();
            if (date.isBefore(today)) {
                throw new IllegalArgumentException("과거 날짜는 주문하실 수 없습니다. 오늘 이후 날짜를 선택해주세요.");
            }
        } catch (Exception e) {
            // 파싱 실패는 다른 곳에서 처리
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
        }
    }
}


