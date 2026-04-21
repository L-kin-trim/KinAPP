package com.example.kin.util;

import com.example.kin.model.ScoreboardSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ScoreboardParser {
    private static final Pattern SCORE_PATTERN = Pattern.compile("(\\d{1,2})\\s*[:：\\-]\\s*(\\d{1,2})");
    private static final Pattern MONEY_PATTERN = Pattern.compile("\\$\\s*([1-9]\\d{2,5})");
    private static final Pattern KDA_PATTERN = Pattern.compile("(\\d{1,2})\\s*/\\s*(\\d{1,2})(?:\\s*/\\s*(\\d{1,2}))?");

    private ScoreboardParser() {
    }

    public static ScoreboardSnapshot parse(String rawText) {
        ScoreboardSnapshot snapshot = new ScoreboardSnapshot();
        snapshot.rawText = rawText == null ? "" : rawText.trim();
        if (isEmpty(snapshot.rawText)) {
            return snapshot;
        }
        snapshot.scoreText = parseScore(snapshot.rawText);
        snapshot.moneyText = parseMoney(snapshot.rawText);
        snapshot.kdaText = parseKda(snapshot.rawText);
        return snapshot;
    }

    private static String parseScore(String rawText) {
        Matcher matcher = SCORE_PATTERN.matcher(rawText);
        while (matcher.find()) {
            int left = toInt(matcher.group(1));
            int right = toInt(matcher.group(2));
            if (left <= 30 && right <= 30) {
                return left + ":" + right;
            }
        }
        return "";
    }

    private static String parseMoney(String rawText) {
        Matcher matcher = MONEY_PATTERN.matcher(rawText);
        LinkedHashSet<String> values = new LinkedHashSet<>();
        while (matcher.find() && values.size() < 8) {
            values.add("$" + matcher.group(1));
        }
        return join(values, ", ");
    }

    private static String parseKda(String rawText) {
        Matcher matcher = KDA_PATTERN.matcher(rawText);
        LinkedHashSet<String> values = new LinkedHashSet<>();
        while (matcher.find() && values.size() < 6) {
            String kill = matcher.group(1);
            String death = matcher.group(2);
            String assist = matcher.group(3);
            values.add(assist == null ? (kill + "/" + death) : (kill + "/" + death + "/" + assist));
        }
        return join(values, "; ");
    }

    private static int toInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String join(LinkedHashSet<String> values, String delimiter) {
        List<String> list = new ArrayList<>(values);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                builder.append(delimiter);
            }
            builder.append(list.get(i));
        }
        return builder.toString();
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
