package com.mrdabak.dinnerservice.voice.service;

import com.mrdabak.dinnerservice.voice.dto.VoiceOrderSummaryDto;
import com.mrdabak.dinnerservice.voice.model.VoiceOrderItem;
import com.mrdabak.dinnerservice.voice.model.VoiceOrderSession;
import com.mrdabak.dinnerservice.voice.model.VoiceOrderState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VoiceOrderSummaryMapper {

    private final VoiceMenuCatalogService menuCatalogService;

    public VoiceOrderSummaryMapper(VoiceMenuCatalogService menuCatalogService) {
        this.menuCatalogService = menuCatalogService;
    }

    public VoiceOrderSummaryDto toSummary(VoiceOrderSession session) {
        VoiceOrderState state = session.getCurrentState();
        VoiceOrderSummaryDto summary = new VoiceOrderSummaryDto();
        summary.setDinnerName(menuCatalogService.dinnerLabel(state.getDinnerType()));
        summary.setServingStyle(menuCatalogService.servingStyleLabel(state.getServingStyle()));
        summary.setItems(buildItems(state));
        summary.setDeliverySlot(buildDeliverySlot(state));
        summary.setDeliveryAddress(state.getDeliveryAddress());
        summary.setContactPhone(
                state.getContactPhone() != null ? state.getContactPhone() : session.getCustomerPhone());
        summary.setSpecialRequests(state.getSpecialRequests());
        summary.setReadyForConfirmation(state.isReadyForCheckout());
        summary.setMissingFields(missingFields(state));
        return summary;
    }

    private List<VoiceOrderSummaryDto.SummaryItem> buildItems(VoiceOrderState state) {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();

        if (state.getDinnerType() != null) {
            VoiceMenuCatalogService.DinnerDescriptor dinner = menuCatalogService.requireDinner(state.getDinnerType());
            menuCatalogService.getDefaultItems(dinner.id()).forEach(portion -> {
                quantities.put(portion.key(), portion.quantity());
                labels.put(portion.key(), portion.name());
            });
        }

        if (state.getMenuAdjustments() != null) {
            for (VoiceOrderItem item : state.getMenuAdjustments()) {
                if (item.getKey() == null || item.getQuantity() == null) {
                    continue;
                }
                quantities.put(item.getKey(), item.getQuantity());
                if (item.getName() != null) {
                    labels.put(item.getKey(), item.getName());
                }
            }
        }

        return quantities.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .map(entry -> new VoiceOrderSummaryDto.SummaryItem(
                        labels.getOrDefault(entry.getKey(), entry.getKey()),
                        entry.getValue()))
                .toList();
    }

    private String buildDeliverySlot(VoiceOrderState state) {
        if (state.getDeliveryDate() != null && state.getDeliveryTime() != null) {
            return "%s %s".formatted(state.getDeliveryDate(), state.getDeliveryTime());
        }
        return state.getDeliveryDateTime();
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
        return missing;
    }
}


