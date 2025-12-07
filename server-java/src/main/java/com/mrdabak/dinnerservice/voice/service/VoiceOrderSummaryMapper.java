package com.mrdabak.dinnerservice.voice.service;

import com.mrdabak.dinnerservice.voice.dto.VoiceOrderSummaryDto;
import com.mrdabak.dinnerservice.voice.model.VoiceOrderItem;
import com.mrdabak.dinnerservice.voice.model.VoiceOrderSession;
import com.mrdabak.dinnerservice.voice.model.VoiceOrderState;
import com.mrdabak.dinnerservice.voice.util.DomainVocabularyNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class VoiceOrderSummaryMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceOrderSummaryMapper.class);
    private final VoiceMenuCatalogService menuCatalogService;
    private final DomainVocabularyNormalizer normalizer;

    public VoiceOrderSummaryMapper(VoiceMenuCatalogService menuCatalogService,
                                   DomainVocabularyNormalizer normalizer) {
        this.menuCatalogService = menuCatalogService;
        this.normalizer = normalizer;
    }

    public VoiceOrderSummaryDto toSummary(VoiceOrderSession session) {
        VoiceOrderState state = session.getCurrentState();
        if (state == null) {
            LOGGER.warn("VoiceOrderState가 null입니다. 새로 생성합니다.");
            state = new VoiceOrderState();
        }
        
        // 디버깅: state 내용 로깅
        LOGGER.debug("=== 주문 요약 생성 시작 ===");
        LOGGER.debug("State - dinnerType: {}, servingStyle: {}, deliveryDateTime: {}, deliveryAddress: {}, contactPhone: {}", 
                state.getDinnerType(), state.getServingStyle(), state.getDeliveryDateTime(), 
                state.getDeliveryAddress(), state.getContactPhone());
        LOGGER.debug("State - menuAdjustments: {}", state.getMenuAdjustments() != null ? state.getMenuAdjustments().size() : 0);
        
        VoiceOrderSummaryDto summary = new VoiceOrderSummaryDto();
        
        // 디너 이름 설정 (null 체크 및 빈 문자열 체크)
        String dinnerName = null;
        if (state.getDinnerType() != null && !state.getDinnerType().isBlank()) {
            try {
                dinnerName = menuCatalogService.dinnerLabel(state.getDinnerType());
                LOGGER.debug("디너 라벨: {} -> {}", state.getDinnerType(), dinnerName);
            } catch (Exception e) {
                LOGGER.warn("디너 라벨 조회 실패: {} - {}", state.getDinnerType(), e.getMessage());
                dinnerName = state.getDinnerType(); // 원본 값 사용
            }
        }
        summary.setDinnerName(dinnerName);
        
        // 서빙 스타일 설정 (null 체크 및 빈 문자열 체크)
        String servingStyle = null;
        if (state.getServingStyle() != null && !state.getServingStyle().isBlank()) {
            try {
                servingStyle = menuCatalogService.servingStyleLabel(state.getServingStyle());
                LOGGER.debug("서빙 스타일 라벨: {} -> {}", state.getServingStyle(), servingStyle);
            } catch (Exception e) {
                LOGGER.warn("서빙 스타일 라벨 조회 실패: {} - {}", state.getServingStyle(), e.getMessage());
                servingStyle = state.getServingStyle(); // 원본 값 사용
            }
        }
        summary.setServingStyle(servingStyle);
        
        // 주문 항목 설정 (항상 실행하여 빈 리스트라도 반환)
        List<VoiceOrderSummaryDto.SummaryItem> items = buildItems(state);
        summary.setItems(items != null ? items : new ArrayList<>());
        LOGGER.debug("주문 항목 수: {}", summary.getItems().size());
        
        // 배달 시간 설정
        String deliverySlot = buildDeliverySlot(state);
        summary.setDeliverySlot(deliverySlot);
        LOGGER.debug("배달 시간: {}", deliverySlot);
        
        // 배달 주소 설정 (state에 없으면 세션의 기본 주소 사용)
        String deliveryAddress = state.getDeliveryAddress();
        if (deliveryAddress == null || deliveryAddress.isBlank()) {
            deliveryAddress = session.getCustomerDefaultAddress();
            LOGGER.debug("배달 주소를 세션 기본 주소로 설정: {}", deliveryAddress);
        }
        summary.setDeliveryAddress(deliveryAddress);
        
        // 연락처 설정 (state에 없으면 세션의 고객 전화번호 사용)
        String contactPhone = state.getContactPhone();
        if (contactPhone == null || contactPhone.isBlank()) {
            contactPhone = session.getCustomerPhone();
            LOGGER.debug("연락처를 세션 전화번호로 설정: {}", contactPhone);
        }
        summary.setContactPhone(contactPhone);
        
        // 특별 요청사항 설정
        summary.setSpecialRequests(state.getSpecialRequests());
        
        // 주문 확정 준비 여부
        summary.setReadyForConfirmation(state.isReadyForCheckout());
        
        // 주문 확정 의사 반영
        summary.setFinalConfirmation(state.getFinalConfirmation());
        
        // 누락된 필드 목록
        summary.setMissingFields(missingFields(state));
        
        // 주문 완료 후 주문 정보 반영
        if (session.isOrderPlaced() && session.getCreatedOrderId() != null) {
            summary.setOrderId(session.getCreatedOrderId());
        }
        
        LOGGER.debug("=== 주문 요약 생성 완료 ===");
        LOGGER.debug("Summary - dinnerName: {}, servingStyle: {}, items: {}, deliverySlot: {}, deliveryAddress: {}, contactPhone: {}", 
                summary.getDinnerName(), summary.getServingStyle(), summary.getItems().size(), 
                summary.getDeliverySlot(), summary.getDeliveryAddress(), summary.getContactPhone());
        
        return summary;
    }
    
    /**
     * 주문 완료 후 주문 정보를 포함한 요약 생성
     */
    public VoiceOrderSummaryDto toSummaryWithOrder(VoiceOrderSession session, Long orderId, Integer totalPrice) {
        VoiceOrderSummaryDto summary = toSummary(session);
        summary.setOrderId(orderId);
        summary.setTotalPrice(totalPrice);
        summary.setReadyForConfirmation(false); // 주문 완료 후에는 확정 불가
        return summary;
    }

    private List<VoiceOrderSummaryDto.SummaryItem> buildItems(VoiceOrderState state) {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();

        // menuAdjustments에 절대값 설정이 필요한지 확인
        // action이 없거나 "set", "change"이면 절대값, "add"만 추가
        boolean hasSetAction = false;
        boolean hasOnlyAddAction = true;
        if (state.getMenuAdjustments() != null && !state.getMenuAdjustments().isEmpty()) {
            for (VoiceOrderItem item : state.getMenuAdjustments()) {
                if (item != null) {
                    String action = item.getAction() != null ? item.getAction().toLowerCase() : "";
                    boolean isAdd = action.contains("add") || action.contains("increase") ||
                                   action.contains("추가") || action.contains("증가");
                    boolean isSet = action.contains("set") || action.contains("change") ||
                                   action.contains("설정") || action.contains("변경") || action.isEmpty();
                    
                    if (isSet) {
                        hasSetAction = true;
                        hasOnlyAddAction = false;
                    } else if (!isAdd) {
                        // action이 없으면 절대값으로 간주
                        hasSetAction = true;
                        hasOnlyAddAction = false;
                    }
                }
            }
        }
        
        // 기본 디너 메뉴 항목 추가 (디너가 선택된 경우에만)
        // 단, menuAdjustments에 "set" 액션이 있으면 기본 수량을 설정하지 않음 (절대값 사용)
        if (!hasSetAction && state != null && state.getDinnerType() != null && !state.getDinnerType().isBlank()) {
            try {
                VoiceMenuCatalogService.DinnerDescriptor dinner = menuCatalogService.requireDinner(state.getDinnerType());
                List<VoiceMenuCatalogService.MenuItemPortion> defaultItems = menuCatalogService.getDefaultItems(dinner.id());
                
                LOGGER.debug("기본 디너 메뉴 항목 수: {}", defaultItems != null ? defaultItems.size() : 0);
                
                if (defaultItems != null && !defaultItems.isEmpty()) {
                    defaultItems.forEach(portion -> {
                        if (portion != null && portion.key() != null && portion.quantity() != null) {
                            quantities.put(portion.key(), portion.quantity());
                            if (portion.name() != null) {
                                labels.put(portion.key(), portion.name());
                            }
                            LOGGER.debug("기본 메뉴 항목 추가: {} x{}", portion.name(), portion.quantity());
                        }
                    });
                }
            } catch (Exception e) {
                // 디너를 찾을 수 없는 경우 로그만 남기고 계속 진행
                LOGGER.error("디너를 찾을 수 없습니다: {} - {}", state.getDinnerType(), e.getMessage(), e);
            }
        } else {
            if (hasSetAction) {
                LOGGER.debug("menuAdjustments에 'set' 액션이 있어 기본 수량을 설정하지 않습니다.");
            } else {
                LOGGER.debug("디너가 선택되지 않았습니다. state.getDinnerType(): {}", state != null ? state.getDinnerType() : "null");
            }
        }

        // 인분(portion) 배수 추출 및 적용
        // 단, menuAdjustments에 "set" 액션이 있으면 인분 배수 적용 안 함 (절대값 사용)
        if (!hasSetAction) {
            int portionMultiplier = extractPortionMultiplier(state);
            if (portionMultiplier > 1) {
                // 모든 기본 수량에 인분 배수 적용
                quantities.replaceAll((k, v) -> v * portionMultiplier);
            }
        }

        // 메뉴 조정 사항 적용
        if (state.getMenuAdjustments() != null && !state.getMenuAdjustments().isEmpty()) {
            for (VoiceOrderItem item : state.getMenuAdjustments()) {
                if (item == null) continue;
                
                // 인분 정보가 포함된 경우 건너뛰기
                if (item.getName() != null && (item.getName().contains("인분") || item.getName().contains("명분"))) {
                    continue;
                }
                
                String itemKey = null;
                String rawKey = item.getKey();
                String rawName = item.getName();
                
                // key 정규화 시도
                if (rawKey != null && !rawKey.isBlank()) {
                    Optional<String> normalized = normalizer.normalizeMenuItemKey(rawKey);
                    itemKey = normalized.orElse(rawKey.toLowerCase().trim());
                }
                
                // key가 없거나 정규화 실패 시 name으로 시도
                if (itemKey == null && rawName != null && !rawName.isBlank()) {
                    Optional<String> normalized = normalizer.normalizeMenuItemKey(rawName);
                    if (normalized.isPresent()) {
                        itemKey = normalized.get();
                    } else {
                        // 정규화 실패 시 데이터베이스에서 조회 시도
                        try {
                            VoiceMenuCatalogService.MenuItemPortion portion = 
                                    menuCatalogService.describeMenuItem(rawName);
                            itemKey = portion.key();
                        } catch (Exception e) {
                            LOGGER.debug("메뉴 항목을 찾을 수 없습니다: {} - {}", rawName, e.getMessage());
                            continue; // 찾을 수 없으면 건너뛰기
                        }
                    }
                }
                
                if (itemKey == null || itemKey.isBlank()) {
                    LOGGER.warn("메뉴 항목 key를 결정할 수 없습니다. key={}, name={}", rawKey, rawName);
                    continue;
                }
                
                Integer itemQuantity = item.getQuantity();
                
                // quantity가 0 이하인 경우 제거
                if (itemQuantity == null || itemQuantity <= 0) {
                    quantities.remove(itemKey);
                    continue;
                }
                
                // action 확인
                String action = item.getAction() != null ? item.getAction().toLowerCase() : "";
                boolean isAddAction = action.contains("add") || action.contains("increase") ||
                                     action.contains("추가") || action.contains("증가");
                
                if (isAddAction) {
                    // action이 "add", "increase", "추가", "증가"인 경우 기존 수량에 추가
                    Integer currentQuantity = quantities.getOrDefault(itemKey, 0);
                    quantities.put(itemKey, currentQuantity + itemQuantity);
                    LOGGER.debug("메뉴 항목 추가: {} (기존: {} + 추가: {} = {})", itemKey, currentQuantity, itemQuantity, currentQuantity + itemQuantity);
                } else {
                    // action이 없거나 "set", "change"인 경우 수량을 절대값으로 설정 (기본 수량 무시)
                    quantities.put(itemKey, itemQuantity);
                    LOGGER.debug("메뉴 항목 설정 (절대값): {} = {}", itemKey, itemQuantity);
                }
                
                // 라벨 업데이트 (name이 있으면 사용, 없으면 데이터베이스에서 조회)
                if (rawName != null && !rawName.isBlank()) {
                    labels.put(itemKey, rawName);
                } else if (!labels.containsKey(itemKey)) {
                    // 라벨이 없으면 데이터베이스에서 메뉴 항목 이름 조회
                    try {
                        VoiceMenuCatalogService.MenuItemPortion portion = 
                                menuCatalogService.describeMenuItem(itemKey);
                        labels.put(itemKey, portion.name());
                    } catch (Exception e) {
                        // 조회 실패 시 key를 그대로 사용하지 않고, 한글 이름으로 변환 시도
                        String koreanName = convertKeyToKoreanName(itemKey);
                        labels.put(itemKey, koreanName);
                    }
                }
            }
        }

        return quantities.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .map(entry -> {
                    String itemName = labels.get(entry.getKey());
                    if (itemName == null || itemName.isBlank()) {
                        // 라벨이 없으면 데이터베이스에서 조회
                        try {
                            VoiceMenuCatalogService.MenuItemPortion portion = 
                                    menuCatalogService.describeMenuItem(entry.getKey());
                            itemName = portion.name();
                        } catch (Exception e) {
                            // 조회 실패 시 한글 이름으로 변환
                            itemName = convertKeyToKoreanName(entry.getKey());
                        }
                    }
                    return new VoiceOrderSummaryDto.SummaryItem(itemName, entry.getValue());
                })
                .toList();
    }
    
    /**
     * 인분(portion) 배수 추출 (예: "2인분", "2명분" → 2)
     */
    private int extractPortionMultiplier(VoiceOrderState state) {
        int multiplier = 1;
        
        // specialRequests에서 인분 정보 추출
        if (state.getSpecialRequests() != null && !state.getSpecialRequests().isBlank()) {
            String requests = state.getSpecialRequests().toLowerCase();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*(?:인분|명분)");
            java.util.regex.Matcher matcher = pattern.matcher(requests);
            if (matcher.find()) {
                try {
                    multiplier = Integer.parseInt(matcher.group(1));
                } catch (Exception e) {
                    // 파싱 실패 시 1 유지
                }
            }
        }
        
        // menuAdjustments에서 인분 정보 추출
        if (state.getMenuAdjustments() != null) {
            for (VoiceOrderItem item : state.getMenuAdjustments()) {
                if (item.getName() != null) {
                    String name = item.getName().toLowerCase();
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*(?:인분|명분)");
                    java.util.regex.Matcher matcher = pattern.matcher(name);
                    if (matcher.find()) {
                        try {
                            int found = Integer.parseInt(matcher.group(1));
                            if (found > multiplier) {
                                multiplier = found;
                            }
                        } catch (Exception e) {
                            // 파싱 실패 시 무시
                        }
                    }
                }
            }
        }
        
        return multiplier > 0 ? multiplier : 1;
    }

    private String buildDeliverySlot(VoiceOrderState state) {
        // deliveryDate와 deliveryTime이 모두 있으면 조합
        if (state.getDeliveryDate() != null && !state.getDeliveryDate().isBlank() 
                && state.getDeliveryTime() != null && !state.getDeliveryTime().isBlank()) {
            return "%s %s".formatted(state.getDeliveryDate(), state.getDeliveryTime());
        }
        
        // deliveryDateTime이 있으면 포맷팅하여 반환
        if (state.getDeliveryDateTime() != null && !state.getDeliveryDateTime().isBlank()) {
            try {
                // ISO 형식 파싱 (예: "2025-12-05T20:00:00")
                java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(
                        state.getDeliveryDateTime(), 
                        java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                
                // 한국어 형식으로 포맷팅 (예: "2025년 12월 5일 오후 8시")
                java.time.format.DateTimeFormatter formatter = 
                        java.time.format.DateTimeFormatter.ofPattern("yyyy년 M월 d일 a h시", java.util.Locale.KOREAN);
                return dateTime.format(formatter);
            } catch (Exception e) {
                // 파싱 실패 시 원본 반환
                return state.getDeliveryDateTime();
            }
        }
        
        return null;
    }

    private List<String> missingFields(VoiceOrderState state) {
        List<String> missing = new ArrayList<>();
        if (!state.hasDinnerSelection()) {
            missing.add("디너 선택");
        }
        if (!state.hasServingStyle()) {
            missing.add("서빙 스타일");
        }
        if (!state.hasDeliverySlot()) {
            missing.add("배달 날짜/시간");
        }
        if (!state.hasAddress()) {
            missing.add("배달 주소");
        }
        if (!state.hasContactPhone()) {
            missing.add("연락처(전화번호)");
        }
        return missing;
    }
    
    /**
     * 메뉴 항목 키를 한글 이름으로 변환 (fallback)
     */
    private String convertKeyToKoreanName(String key) {
        if (key == null || key.isBlank()) {
            return key;
        }
        return switch (key.toLowerCase()) {
            case "bread" -> "빵";
            case "baguette" -> "바게트빵";
            case "champagne" -> "샴페인";
            case "wine" -> "와인";
            case "coffee" -> "커피";
            case "steak" -> "스테이크";
            case "salad" -> "샐러드";
            case "eggs" -> "에그 스크램블";
            case "bacon" -> "베이컨";
            default -> key;
        };
    }
}


