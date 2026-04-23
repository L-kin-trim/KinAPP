package com.example.kin.util;

import com.example.kin.model.ScoreboardSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ScoreboardParser {
    private static final Pattern SCORE_PATTERN = Pattern.compile("(\\d{1,2})\\s*[:：\\-]\\s*(\\d{1,2})");
    private static final Pattern MONEY_PATTERN = Pattern.compile("\\$\\s*([1-9]\\d{2,5})");
    private static final Pattern KDA_PATTERN = Pattern.compile("(\\d{1,2})\\s*/\\s*(\\d{1,2})(?:\\s*/\\s*(\\d{1,2}))?");
    private static final Pattern NAME_PATTERN = Pattern.compile("([\\p{L}][\\p{L}0-9_\\-]{1,15})");

    private static final Set<String> NAME_STOP_WORDS = new LinkedHashSet<>(Arrays.asList(
            "score", "team", "map", "money", "kill", "death", "assist", "damage", "ping",
            "steam", "mode", "round", "alive", "user", "player", "fps", "bomb", "timeout",
            "k", "d", "a", "cs2", "competitive", "name", "total", "ct", "t"
    ));

    private static final Map<String, String> MAP_ALIASES = createMapAliases();

    private ScoreboardParser() {
    }

    public static ScoreboardSnapshot parse(String rawText) {
        ScoreboardSnapshot snapshot = new ScoreboardSnapshot();
        snapshot.rawText = rawText == null ? "" : rawText.trim();
        if (isEmpty(snapshot.rawText)) {
            return snapshot;
        }

        snapshot.scoreText = parseScore(snapshot.rawText);
        List<Integer> moneyValues = parseMoneyValues(snapshot.rawText);
        List<int[]> kdas = parseKdaValues(snapshot.rawText);
        List<String> usernames = parseUsernames(snapshot.rawText);

        snapshot.moneyText = formatMoney(moneyValues);
        snapshot.kdaText = formatKda(kdas);
        snapshot.mapName = parseMap(snapshot.rawText);
        snapshot.players.addAll(buildPlayers(usernames, moneyValues, kdas));
        snapshot.playerStatsText = buildPlayerSummary(snapshot.players);
        snapshot.hotHandSummary = summarizeHotHand(snapshot.players);
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

    private static List<Integer> parseMoneyValues(String rawText) {
        LinkedHashSet<Integer> values = new LinkedHashSet<>();
        Matcher moneyMatcher = MONEY_PATTERN.matcher(rawText);
        while (moneyMatcher.find() && values.size() < 10) {
            int value = toInt(moneyMatcher.group(1));
            if (value >= 150 && value <= 20000) {
                values.add(value);
            }
        }
        return new ArrayList<>(values);
    }

    private static List<int[]> parseKdaValues(String rawText) {
        List<int[]> values = new ArrayList<>();
        Matcher matcher = KDA_PATTERN.matcher(rawText);
        while (matcher.find() && values.size() < 12) {
            int kills = toInt(matcher.group(1));
            int deaths = toInt(matcher.group(2));
            int assists = toInt(matcher.group(3));
            if (kills > 40 || deaths > 40 || assists > 40) {
                continue;
            }
            values.add(new int[]{kills, deaths, assists});
        }
        return values;
    }

    private static List<String> parseUsernames(String rawText) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        Matcher matcher = NAME_PATTERN.matcher(rawText);
        while (matcher.find() && names.size() < 12) {
            String value = matcher.group(1);
            if (value == null) {
                continue;
            }
            String normalized = value.trim();
            if (normalized.length() < 2) {
                continue;
            }
            String lower = normalized.toLowerCase(Locale.ROOT);
            if (NAME_STOP_WORDS.contains(lower) || MAP_ALIASES.containsKey(lower)) {
                continue;
            }
            if (isNumeric(normalized)) {
                continue;
            }
            names.add(normalized);
        }
        return new ArrayList<>(names);
    }

    private static String parseMap(String rawText) {
        String lower = rawText.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : MAP_ALIASES.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "";
    }

    private static List<ScoreboardSnapshot.PlayerStat> buildPlayers(List<String> names,
                                                                    List<Integer> moneyValues,
                                                                    List<int[]> kdas) {
        int count = Math.max(names.size(), kdas.size());
        count = Math.max(count, Math.min(moneyValues.size(), 5));
        if (count <= 0) {
            return new ArrayList<>();
        }

        List<ScoreboardSnapshot.PlayerStat> players = new ArrayList<>();
        for (int i = 0; i < count && i < 10; i++) {
            ScoreboardSnapshot.PlayerStat stat = new ScoreboardSnapshot.PlayerStat();
            stat.username = i < names.size() ? names.get(i) : ("Player" + (i + 1));
            if (i < moneyValues.size()) {
                stat.money = moneyValues.get(i);
            }
            if (i < kdas.size()) {
                int[] kda = kdas.get(i);
                stat.kills = kda[0];
                stat.deaths = kda[1];
                stat.assists = kda[2];
            }
            players.add(stat);
        }
        return players;
    }

    private static String buildPlayerSummary(List<ScoreboardSnapshot.PlayerStat> players) {
        if (players == null || players.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < players.size() && i < 6; i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(players.get(i).pretty());
        }
        return builder.toString();
    }

    private static String summarizeHotHand(List<ScoreboardSnapshot.PlayerStat> players) {
        if (players == null || players.isEmpty()) {
            return "";
        }
        ScoreboardSnapshot.PlayerStat best = null;
        for (ScoreboardSnapshot.PlayerStat player : players) {
            if (best == null) {
                best = player;
                continue;
            }
            if (player.kills > best.kills) {
                best = player;
                continue;
            }
            if (player.kills == best.kills && player.kdRatio() > best.kdRatio()) {
                best = player;
            }
        }
        if (best == null) {
            return "";
        }
        return best.username + " is hot: K/D/A "
                + best.kills + "/" + best.deaths + "/" + best.assists
                + " (KD " + String.format(Locale.US, "%.2f", best.kdRatio()) + ")";
    }

    private static String formatMoney(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size() && i < 8; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append('$').append(values.get(i));
        }
        return builder.toString();
    }

    private static String formatKda(List<int[]> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size() && i < 8; i++) {
            int[] kda = values.get(i);
            if (i > 0) {
                builder.append("; ");
            }
            builder.append(kda[0]).append('/').append(kda[1]).append('/').append(kda[2]);
        }
        return builder.toString();
    }

    private static Map<String, String> createMapAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("dust2", "Dust II");
        aliases.put("dust ii", "Dust II");
        aliases.put("\u7099\u70ed\u6c99\u57ce", "Dust II");
        aliases.put("mirage", "Mirage");
        aliases.put("\u8352\u6f20\u8ff7\u57ce", "Mirage");
        aliases.put("inferno", "Inferno");
        aliases.put("\u70bc\u72f1\u5c0f\u9547", "Inferno");
        aliases.put("ancient", "Ancient");
        aliases.put("\u963f\u52aa\u6bd4\u65af", "Anubis");
        aliases.put("anubis", "Anubis");
        aliases.put("nuke", "Nuke");
        aliases.put("\u6838\u5b50\u5371\u673a", "Nuke");
        aliases.put("overpass", "Overpass");
        aliases.put("\u6b7b\u4ea1\u6e38\u4e50\u56ed", "Overpass");
        aliases.put("vertigo", "Vertigo");
        aliases.put("\u6b92\u547d\u5927\u53a6", "Vertigo");
        aliases.put("train", "Train");
        aliases.put("\u5217\u8f66\u505c\u653e\u7ad9", "Train");
        return aliases;
    }

    private static int toInt(String value) {
        if (isEmpty(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static boolean isNumeric(String value) {
        if (isEmpty(value)) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
