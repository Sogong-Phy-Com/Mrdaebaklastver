package com.mrdabak.dinnerservice.voice.service;

import com.mrdabak.dinnerservice.voice.model.VoiceOrderItem;
import com.mrdabak.dinnerservice.voice.model.VoiceOrderState;
import com.mrdabak.dinnerservice.voice.util.DomainVocabularyNormalizer;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
            List<VoiceOrderItem> normalizedItems = new ArrayList<>();
            for (VoiceOrderItem item : incoming.getMenuAdjustments()) {
                if (item == null) continue;
                VoiceOrderItem clone = new VoiceOrderItem();
                clone.setQuantity(item.getQuantity());
                clone.setAction(item.getAction());
                clone.setName(item.getName());
                normalizer.normalizeMenuItemKey(Optional.ofNullable(item.getKey()).orElse(item.getName()))
                        .ifPresent(clone::setKey);
                if (clone.getKey() == null && item.getKey() != null) {
                    clone.setKey(item.getKey());
                }
                normalizedItems.add(clone);
            }
            current.setMenuAdjustments(normalizedItems);
        }

        if (incoming.getDeliveryDateTime() != null && !incoming.getDeliveryDateTime().isBlank()) {
            parseDateTime(incoming.getDeliveryDateTime()).ifPresent(dt -> {
                current.setDeliveryDate(dt.toLocalDate().toString());
                current.setDeliveryTime(dt.toLocalTime().withSecond(0).withNano(0).toString());
                current.setDeliveryDateTime(dt.toString());
            });
        } else {
            if (incoming.getDeliveryDate() != null) {
                current.setDeliveryDate(incoming.getDeliveryDate());
            }
            if (incoming.getDeliveryTime() != null) {
                current.setDeliveryTime(incoming.getDeliveryTime());
            }
        }

        if (incoming.getDeliveryAddress() != null && !incoming.getDeliveryAddress().isBlank()) {
            current.setDeliveryAddress(incoming.getDeliveryAddress().trim());
        }

        if (incoming.getContactPhone() != null) {
            current.setContactPhone(incoming.getContactPhone());
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
}


