package com.mrdabak.dinnerservice.voice.service;

import com.mrdabak.dinnerservice.dto.OrderItemDto;
import com.mrdabak.dinnerservice.dto.OrderRequest;
import com.mrdabak.dinnerservice.voice.VoiceOrderException;
import com.mrdabak.dinnerservice.voice.model.VoiceOrderItem;
import com.mrdabak.dinnerservice.voice.model.VoiceOrderSession;
import com.mrdabak.dinnerservice.voice.model.VoiceOrderState;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VoiceOrderMapper {

    private final VoiceMenuCatalogService menuCatalogService;

    public VoiceOrderMapper(VoiceMenuCatalogService menuCatalogService) {
        this.menuCatalogService = menuCatalogService;
    }

    public OrderRequest toOrderRequest(VoiceOrderSession session) {
        VoiceOrderState state = session.getCurrentState();
        if (!state.isReadyForCheckout()) {
            throw new VoiceOrderException("주문 확정을 위해 필요한 정보가 부족합니다.");
        }

        VoiceMenuCatalogService.DinnerDescriptor dinner = menuCatalogService.requireDinner(state.getDinnerType());
        validateServingStyle(state, dinner);

        String deliveryTimestamp = resolveDeliveryTimestamp(state);
        String address = state.getDeliveryAddress() != null
                ? state.getDeliveryAddress()
                : session.getCustomerDefaultAddress();
        if (address == null || address.isBlank()) {
            throw new VoiceOrderException("배달 주소를 확인할 수 없습니다.");
        }

        List<OrderItemDto> items = buildItems(state, dinner);
        if (items.isEmpty()) {
            throw new VoiceOrderException("주문 항목을 확인하지 못했습니다.");
        }

        OrderRequest request = new OrderRequest();
        request.setDinnerTypeId(dinner.id());
        request.setServingStyle(state.getServingStyle());
        request.setDeliveryTime(deliveryTimestamp);
        request.setDeliveryAddress(address);
        request.setItems(items);
        request.setPaymentMethod("voice-bot-card");
        return request;
    }

    private void validateServingStyle(VoiceOrderState state, VoiceMenuCatalogService.DinnerDescriptor dinner) {
        if (state.getServingStyle() == null) {
            throw new VoiceOrderException("서빙 스타일을 먼저 정해 주세요.");
        }
        if (!dinner.allowedServingStyles().contains(state.getServingStyle())) {
            throw new VoiceOrderException("선택한 디너와 서빙 스타일 조합이 허용되지 않습니다.");
        }
    }

    private List<OrderItemDto> buildItems(VoiceOrderState state, VoiceMenuCatalogService.DinnerDescriptor dinner) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();

        // 기본 디너 메뉴 수량 설정
        menuCatalogService.getDefaultItems(dinner.id()).forEach(portion ->
                quantities.put(portion.menuItemId(), portion.quantity()));

        if (state.getMenuAdjustments() != null) {
            for (VoiceOrderItem adjustment : state.getMenuAdjustments()) {
                if (adjustment.getQuantity() == null) {
                    continue;
                }
                VoiceMenuCatalogService.MenuItemPortion target = menuCatalogService.describeMenuItem(
                        adjustment.getKey() != null ? adjustment.getKey() : adjustment.getName());
                
                Long menuItemId = target.menuItemId();
                Integer currentQuantity = quantities.getOrDefault(menuItemId, 0);
                
                // action이 "add", "increase", "추가", "증가"인 경우 기존 수량에 추가
                if (adjustment.getAction() != null && 
                    (adjustment.getAction().toLowerCase().contains("add") || 
                     adjustment.getAction().toLowerCase().contains("increase") ||
                     adjustment.getAction().toLowerCase().contains("추가") ||
                     adjustment.getAction().toLowerCase().contains("증가"))) {
                    int addQuantity = adjustment.getQuantity() != null ? adjustment.getQuantity() : 1;
                    quantities.put(menuItemId, currentQuantity + addQuantity);
                } else if (adjustment.getQuantity() <= 0) {
                    // 수량이 0 이하이면 제거
                    quantities.remove(menuItemId);
                } else {
                    // action이 없거나 "set", "change"인 경우 수량을 직접 설정
                    quantities.put(menuItemId, adjustment.getQuantity());
                }
            }
        }

        return quantities.entrySet().stream()
                .map(entry -> new OrderItemDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    private String resolveDeliveryTimestamp(VoiceOrderState state) {
        if (state.getDeliveryDateTime() != null && !state.getDeliveryDateTime().isBlank()) {
            return state.getDeliveryDateTime();
        }
        try {
            LocalDate date = LocalDate.parse(state.getDeliveryDate());
            LocalTime time = LocalTime.parse(state.getDeliveryTime());
            return LocalDateTime.of(date, time.withSecond(0).withNano(0))
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            throw new VoiceOrderException("배달 시간을 해석할 수 없습니다. 다시 알려주세요.");
        }
    }
}


