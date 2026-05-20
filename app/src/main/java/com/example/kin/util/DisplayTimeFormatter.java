package com.example.kin.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class DisplayTimeFormatter {
    private static final long HOUR_MS = 60L * 60L * 1000L;
    private static final long DAY_MS = 24L * HOUR_MS;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA);

    private DisplayTimeFormatter() {
    }

    public static String formatDisplayTime(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String normalized = value.trim();
        Long millis = parseEpochMillis(normalized);
        if (millis == null) {
            millis = parseDateTimeMillis(normalized);
        }
        if (millis == null) {
            return normalized;
        }
        return formatDisplayTime(millis);
    }

    public static String formatDisplayTime(long epochMillis) {
        long now = System.currentTimeMillis();
        long diff = now - epochMillis;
        if (diff >= 0L && diff < DAY_MS) {
            long hours = Math.max(1L, diff / HOUR_MS);
            return hours + "小时前";
        }
        if (diff >= DAY_MS && diff < 3L * DAY_MS) {
            long days = Math.max(1L, diff / DAY_MS);
            return days + "天前";
        }
        return Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DATE_FORMATTER);
    }

    private static Long parseEpochMillis(String value) {
        if (!value.matches("\\d{10,17}")) {
            return null;
        }
        try {
            long raw = Long.parseLong(value);
            return value.length() <= 10 ? raw * 1000L : raw;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Long parseDateTimeMillis(String value) {
        String localDateTimeValue = value.contains(" ") ? value.replace(' ', 'T') : value;
        try {
            return Instant.parse(value).toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return ZonedDateTime.parse(value).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(localDateTimeValue)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(value)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
