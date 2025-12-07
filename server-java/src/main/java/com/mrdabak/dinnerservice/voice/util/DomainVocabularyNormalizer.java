package com.mrdabak.dinnerservice.voice.util;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Provides light-weight normalization helpers so that Whisper/LLM outputs
 * such as "바케트빵", "valentine dinner", or "디럭스" map to a canonical token.
 */
@Component
public class DomainVocabularyNormalizer {

    private static final Map<String, String> CLEANUP_REPLACEMENTS = Map.ofEntries(
            Map.entry("바케트", "바게트"),
            Map.entry("바게뜨", "바게트"),
            Map.entry("바게트빵", "바게트"),
            Map.entry("champagne feast", "샴페인 축제"),
            Map.entry("champagne dinner", "샴페인 축제"),
            Map.entry("심플 스타일", "심플"),
            Map.entry("그랜드 스타일", "그랜드"),
            Map.entry("디럭스 스타일", "디럭스"));

    private static final List<Term> DINNER_TERMS = List.of(
            term("VALENTINE", "발렌타인", "valentine"),
            term("FRENCH", "프렌치", "french"),
            term("ENGLISH", "잉글리시", "english"),
            term("CHAMPAGNE_FEAST", "샴페인 축제", "champagne feast", "champagne"));

    private static final List<Term> SERVING_TERMS = List.of(
            term("simple", "심플", "simple"),
            term("grand", "그랜드", "grand"),
            term("deluxe", "디럭스", "del럭스", "deluxe"));

    private static final List<Term> MENU_ITEM_TERMS = List.of(
            term("champagne", "샴페인", "champagne", "샴페인주", "샴페인술"),
            term("wine", "와인", "wine", "레드와인", "화이트와인", "포도주"),
            term("coffee", "커피", "coffee", "아메리카노", "에스프레소"),
            term("steak", "스테이크", "ste이크", "steak", "스테익", "스테이", "고기"),
            term("salad", "샐러드", "salad", "야채", "채소"),
            term("eggs", "에그", "스크램블", "eggs", "계란", "달걀", "에그스크램블", "스크램블에그"),
            term("bacon", "베이컨", "bacon", "베이컨", "베이킨", "베이큰"),
            term("bread", "빵", "bread", "식빵", "토스트"),
            term("baguette", "바게트", "baguette", "바게트빵", "바케트", "바게뜨", "바게뜨빵"));

    public String cleanupTranscript(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFKC);
        for (Map.Entry<String, String> entry : CLEANUP_REPLACEMENTS.entrySet()) {
            normalized = normalized.replace(entry.getKey(), entry.getValue());
        }
        return normalized.trim();
    }

    public Optional<String> normalizeDinnerType(String input) {
        return normalize(input, DINNER_TERMS);
    }

    public Optional<String> normalizeServingStyle(String input) {
        return normalize(input, SERVING_TERMS);
    }

    public Optional<String> normalizeMenuItemKey(String input) {
        return normalize(input, MENU_ITEM_TERMS);
    }

    private Optional<String> normalize(String input, List<Term> candidates) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String lowered = sanitize(input);
        for (Term term : candidates) {
            if (term.matches(lowered)) {
                return Optional.of(term.canonical());
            }
        }
        return Optional.empty();
    }

    private static String sanitize(String input) {
        String lowered = Normalizer.normalize(input, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .trim();
        return lowered.replaceAll("\\s+", " ");
    }

    private static Term term(String canonical, String... tokens) {
        List<String> normalizedTokens = new ArrayList<>();
        for (String token : tokens) {
            normalizedTokens.add(sanitize(token));
        }
        return new Term(canonical, normalizedTokens);
    }

    private record Term(String canonical, List<String> tokens) {
        boolean matches(String loweredInput) {
            for (String token : tokens) {
                if (loweredInput.contains(token)) {
                    return true;
                }
            }
            return false;
        }
    }
}


