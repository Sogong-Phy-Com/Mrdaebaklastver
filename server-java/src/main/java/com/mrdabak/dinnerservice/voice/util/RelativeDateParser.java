package com.mrdabak.dinnerservice.voice.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * 상대적 날짜 표현을 실제 날짜로 변환하는 유틸리티
 * 예: "내일 오후 8시", "다음주 월요일", "모레 저녁 7시" 등을 실제 날짜/시간으로 변환
 */
public final class RelativeDateParser {

    private RelativeDateParser() {
    }

    /**
     * 상대적 날짜/시간 표현을 실제 날짜/시간으로 변환
     * @param dateExpression 날짜 표현 (예: "내일", "모레", "다음주 월요일")
     * @param timeExpression 시간 표현 (예: "오후 8시", "20:00", "저녁 7시", null 가능)
     * @return 실제 날짜/시간 (YYYY-MM-DD HH:mm 형식)
     */
    public static String parseRelativeDateTime(String dateExpression, String timeExpression) {
        if (dateExpression == null || dateExpression.isBlank()) {
            return null;
        }

        LocalDate today = LocalDate.now();
        LocalDate targetDate = parseRelativeDate(dateExpression, today);
        
        if (targetDate == null) {
            return null;
        }

        LocalTime targetTime = parseTimeExpression(timeExpression);
        if (targetTime == null) {
            targetTime = LocalTime.of(19, 0); // 기본값: 오후 7시
        }

        LocalDateTime dateTime = LocalDateTime.of(targetDate, targetTime);
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    /**
     * 상대적 날짜 표현 파싱
     */
    private static LocalDate parseRelativeDate(String expression, LocalDate today) {
        if (expression == null || expression.isBlank()) {
            return null;
        }

        String lower = expression.toLowerCase().trim();

        // 오늘
        if (lower.contains("오늘") || lower.equals("today")) {
            return today;
        }

        // 내일
        if (lower.contains("내일") || lower.contains("다음날") || lower.equals("tomorrow")) {
            return today.plusDays(1);
        }

        // 모레
        if (lower.contains("모레") || lower.contains("이틀후") || lower.equals("day after tomorrow")) {
            return today.plusDays(2);
        }

        // 다음주 월요일~일요일
        Pattern nextWeekDayPattern = Pattern.compile("다음주\\s*(월|화|수|목|금|토|일)요일");
        java.util.regex.Matcher matcher = nextWeekDayPattern.matcher(lower);
        if (matcher.find()) {
            String dayName = matcher.group(1);
            DayOfWeek targetDay = parseDayOfWeek(dayName);
            if (targetDay != null) {
                return findNextWeekday(today, targetDay);
            }
        }

        // 이번주/다음주 월요일~일요일
        Pattern thisWeekDayPattern = Pattern.compile("(이번주|다음주)?\\s*(월|화|수|목|금|토|일)요일");
        matcher = thisWeekDayPattern.matcher(lower);
        if (matcher.find()) {
            String weekType = matcher.group(1);
            String dayName = matcher.group(2);
            DayOfWeek targetDay = parseDayOfWeek(dayName);
            if (targetDay != null) {
                if (weekType != null && weekType.contains("다음")) {
                    return findNextWeekday(today, targetDay);
                } else {
                    // 이번주 또는 지정 안 함
                    return findThisWeekday(today, targetDay);
                }
            }
        }

        // N일 후
        Pattern daysAfterPattern = Pattern.compile("(\\d+)일\\s*후");
        matcher = daysAfterPattern.matcher(lower);
        if (matcher.find()) {
            int days = Integer.parseInt(matcher.group(1));
            return today.plusDays(days);
        }

        // 이미 YYYY-MM-DD 형식인 경우
        try {
            return LocalDate.parse(expression, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            // 파싱 실패
        }

        return null;
    }

    /**
     * 시간 표현 파싱
     */
    private static LocalTime parseTimeExpression(String expression) {
        if (expression == null || expression.isBlank()) {
            return LocalTime.of(19, 0); // 기본값: 오후 7시
        }

        String lower = expression.toLowerCase().trim();

        // 이미 HH:mm 형식인 경우
        try {
            return LocalTime.parse(lower, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            // 파싱 실패, 계속 진행
        }

        // 오전/오후 시간 - 다양한 패턴 지원
        Pattern amPmPattern1 = Pattern.compile("(오전|오후|am|pm)\\s*(\\d{1,2})\\s*시(?:\\s*(\\d{1,2})\\s*분)?");
        java.util.regex.Matcher matcher = amPmPattern1.matcher(lower);
        if (matcher.find()) {
            String amPm = matcher.group(1);
            int hour = Integer.parseInt(matcher.group(2));
            int minute = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            
            if (amPm != null && (amPm.contains("오전") || amPm.toLowerCase().equals("am"))) {
                if (hour == 12) hour = 0;
                return LocalTime.of(hour, minute);
            } else if (amPm != null && (amPm.contains("오후") || amPm.toLowerCase().equals("pm"))) {
                if (hour != 12) hour += 12;
                return LocalTime.of(hour, minute);
            }
        }
        
        // "오후8시", "오후 8시" (공백 없음)
        Pattern amPmPattern2 = Pattern.compile("(오전|오후|am|pm)\\s*(\\d{1,2})시(?:\\s*(\\d{1,2})분)?");
        matcher = amPmPattern2.matcher(lower);
        if (matcher.find()) {
            String amPm = matcher.group(1);
            int hour = Integer.parseInt(matcher.group(2));
            int minute = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            
            if (amPm != null && (amPm.contains("오전") || amPm.toLowerCase().equals("am"))) {
                if (hour == 12) hour = 0;
                return LocalTime.of(hour, minute);
            } else if (amPm != null && (amPm.contains("오후") || amPm.toLowerCase().equals("pm"))) {
                if (hour != 12) hour += 12;
                return LocalTime.of(hour, minute);
            }
        }
        
        // 숫자만 있는 경우 (예: "8시")
        Pattern hourOnlyPattern = Pattern.compile("(\\d{1,2})\\s*시(?:\\s*(\\d{1,2})\\s*분)?");
        matcher = hourOnlyPattern.matcher(lower);
        if (matcher.find()) {
            int hour = Integer.parseInt(matcher.group(1));
            int minute = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
            
            // 12시간 형식으로 가정 (오후)
            if (hour >= 1 && hour <= 11) {
                hour += 12; // 오후로 가정
            } else if (hour == 12) {
                // 정오는 12시
            }
            return LocalTime.of(hour, minute);
        }

        // 저녁, 점심, 낮 등
        if (lower.contains("저녁")) {
            Pattern eveningPattern = Pattern.compile("(\\d{1,2})시");
            matcher = eveningPattern.matcher(lower);
            if (matcher.find()) {
                int hour = Integer.parseInt(matcher.group(1));
                if (hour < 12) hour += 12;
                return LocalTime.of(hour, 0);
            }
            return LocalTime.of(19, 0); // 기본 저녁 시간
        }
        if (lower.contains("점심") || lower.contains("낮")) {
            return LocalTime.of(12, 0);
        }
        if (lower.contains("아침")) {
            return LocalTime.of(8, 0);
        }

        return null;
    }

    /**
     * 요일 문자열을 DayOfWeek로 변환
     */
    private static DayOfWeek parseDayOfWeek(String dayName) {
        return switch (dayName) {
            case "월" -> DayOfWeek.MONDAY;
            case "화" -> DayOfWeek.TUESDAY;
            case "수" -> DayOfWeek.WEDNESDAY;
            case "목" -> DayOfWeek.THURSDAY;
            case "금" -> DayOfWeek.FRIDAY;
            case "토" -> DayOfWeek.SATURDAY;
            case "일" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    /**
     * 다음주 특정 요일 찾기
     */
    private static LocalDate findNextWeekday(LocalDate today, DayOfWeek targetDay) {
        DayOfWeek todayDay = today.getDayOfWeek();
        int daysUntilTarget = targetDay.getValue() - todayDay.getValue();
        
        if (daysUntilTarget <= 0) {
            daysUntilTarget += 7; // 다음주
        } else {
            daysUntilTarget += 7; // 다음주
        }
        
        return today.plusDays(daysUntilTarget);
    }

    /**
     * 이번주 특정 요일 찾기 (오늘이 그 요일이면 오늘, 아니면 이번주 그 요일)
     */
    private static LocalDate findThisWeekday(LocalDate today, DayOfWeek targetDay) {
        DayOfWeek todayDay = today.getDayOfWeek();
        int daysUntilTarget = targetDay.getValue() - todayDay.getValue();
        
        if (daysUntilTarget < 0) {
            daysUntilTarget += 7; // 다음주
        }
        
        return today.plusDays(daysUntilTarget);
    }
}

