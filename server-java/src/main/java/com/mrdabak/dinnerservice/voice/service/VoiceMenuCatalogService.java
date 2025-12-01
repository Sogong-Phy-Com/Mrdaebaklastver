package com.mrdabak.dinnerservice.voice.service;

import com.mrdabak.dinnerservice.model.DinnerType;
import com.mrdabak.dinnerservice.model.MenuItem;
import com.mrdabak.dinnerservice.repository.DinnerMenuItemRepository;
import com.mrdabak.dinnerservice.repository.DinnerTypeRepository;
import com.mrdabak.dinnerservice.repository.MenuItemRepository;
import com.mrdabak.dinnerservice.voice.VoiceOrderException;
import com.mrdabak.dinnerservice.voice.util.DomainVocabularyNormalizer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VoiceMenuCatalogService {

    private static final List<String> ALL_STYLES = List.of("simple", "grand", "deluxe");

    private final DinnerTypeRepository dinnerTypeRepository;
    private final DinnerMenuItemRepository dinnerMenuItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final DomainVocabularyNormalizer normalizer;

    public VoiceMenuCatalogService(DinnerTypeRepository dinnerTypeRepository,
                                   DinnerMenuItemRepository dinnerMenuItemRepository,
                                   MenuItemRepository menuItemRepository,
                                   DomainVocabularyNormalizer normalizer) {
        this.dinnerTypeRepository = dinnerTypeRepository;
        this.dinnerMenuItemRepository = dinnerMenuItemRepository;
        this.menuItemRepository = menuItemRepository;
        this.normalizer = normalizer;
    }

    public String buildPromptBlock() {
        List<DinnerDescriptor> dinners = listDinners();
        return dinners.stream()
                .map(dinner -> {
                    List<MenuItemPortion> defaults = getDefaultItems(dinner.id());
                    String items = defaults.stream()
                            .map(portion -> "%s x%d".formatted(portion.name(), portion.quantity()))
                            .collect(Collectors.joining(", "));
                    String allowedStyles = String.join(", ", dinner.allowedServingStyles());
                    return "- %s (code=%s, styles=%s) 기본 구성: %s"
                            .formatted(dinner.name(), dinner.canonicalCode(), allowedStyles, items);
                })
                .collect(Collectors.joining("\n"));
    }

    public List<DinnerDescriptor> listDinners() {
        return dinnerTypeRepository.findAll().stream()
                .sorted(Comparator.comparing(DinnerType::getId))
                .map(this::describeDinner)
                .collect(Collectors.toList());
    }

    public DinnerDescriptor requireDinner(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new VoiceOrderException("디너 정보를 먼저 확인해 주세요.");
        }

        Optional<DinnerDescriptor> byId = tryParseLong(identifier)
                .flatMap(dinnerTypeRepository::findById)
                .map(this::describeDinner);
        if (byId.isPresent()) {
            return byId.get();
        }

        String normalized = normalizer.normalizeDinnerType(identifier)
                .orElseGet(() -> sanitize(identifier));

        return listDinners().stream()
                .filter(d -> d.canonicalCode().equalsIgnoreCase(normalized)
                        || sanitize(d.name()).equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new VoiceOrderException("디너를 찾을 수 없습니다: " + identifier));
    }

    public List<String> allowedServingStyles(String dinnerCode) {
        return listDinners().stream()
                .filter(d -> d.canonicalCode().equalsIgnoreCase(dinnerCode))
                .map(DinnerDescriptor::allowedServingStyles)
                .findFirst()
                .orElseGet(() -> new ArrayList<>(ALL_STYLES));
    }

    public String servingStyleLabel(String style) {
        if (style == null) {
            return null;
        }
        return switch (style.toLowerCase(Locale.ROOT)) {
            case "simple" -> "심플";
            case "grand" -> "그랜드";
            case "deluxe" -> "디럭스";
            default -> style;
        };
    }

    public String dinnerLabel(String canonicalCode) {
        if (canonicalCode == null) {
            return null;
        }
        return listDinners().stream()
                .filter(d -> d.canonicalCode().equalsIgnoreCase(canonicalCode))
                .map(DinnerDescriptor::name)
                .findFirst()
                .orElse(canonicalCode);
    }

    public List<MenuItemPortion> getDefaultItems(Long dinnerTypeId) {
        return dinnerMenuItemRepository.findByDinnerTypeId(dinnerTypeId).stream()
                .map(portion -> {
                    MenuItem item = menuItemRepository.findById(portion.getMenuItemId())
                            .orElseThrow(() -> new VoiceOrderException("메뉴 항목을 찾을 수 없습니다: " + portion.getMenuItemId()));
                    String key = normalizer.normalizeMenuItemKey(item.getName()).orElse(item.getName());
                    return new MenuItemPortion(item.getId(), key, item.getName(), portion.getQuantity());
                })
                .collect(Collectors.toList());
    }

    public MenuItemPortion describeMenuItem(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new VoiceOrderException("메뉴 항목명이 필요합니다.");
        }

        String normalized = normalizer.normalizeMenuItemKey(keyword).orElseGet(() -> sanitize(keyword));
        return menuItemRepository.findAll().stream()
                .map(item -> {
                    String key = normalizer.normalizeMenuItemKey(item.getName()).orElse(item.getName());
                    return new MenuItemPortion(item.getId(), key, item.getName(), 1);
                })
                .filter(item -> item.key().equalsIgnoreCase(normalized)
                        || sanitize(item.name()).equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new VoiceOrderException("메뉴 항목을 찾을 수 없습니다: " + keyword));
    }

    private DinnerDescriptor describeDinner(DinnerType dinner) {
        String code = normalizer.normalizeDinnerType(dinner.getName())
                .orElseGet(() -> dinner.getNameEn()
                        .toUpperCase(Locale.ROOT)
                        .replace(" ", "_"));
        List<String> allowed = "CHAMPAGNE_FEAST".equalsIgnoreCase(code)
                ? List.of("grand", "deluxe")
                : ALL_STYLES;
        return new DinnerDescriptor(dinner.getId(), code, dinner.getName(), dinner.getDescription(), allowed);
    }

    private static Optional<Long> tryParseLong(String input) {
        try {
            return Optional.of(Long.parseLong(input.trim()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String sanitize(String text) {
        return text == null ? null : text.trim().toLowerCase(Locale.ROOT);
    }

    public record DinnerDescriptor(Long id,
                                   String canonicalCode,
                                   String name,
                                   String description,
                                   List<String> allowedServingStyles) { }

    public record MenuItemPortion(Long menuItemId,
                                  String key,
                                  String name,
                                  Integer quantity) { }
}


