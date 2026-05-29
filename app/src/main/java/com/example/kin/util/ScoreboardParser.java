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
    private static final Pattern MONEY_PATTERN = Pattern.compile("(?i)(?:[$￥¥＄]|\\bS)\\s*([0-9]{1,5})");
    private static final Pattern ROW_STATS_PATTERN = Pattern.compile("(?i)(.{0,50}?)(?:[$￥¥＄]|\\bS)\\s*([0-9]{1,5})\\s+(\\d{1,2})\\s+(\\d{1,2})\\s+(\\d{1,2})(?:\\s+\\d{1,3})?(?:\\s+\\d{2,5})?");
    private static final Pattern KDA_PATTERN = Pattern.compile("(\\d{1,2})\\s*/\\s*(\\d{1,2})(?:\\s*/\\s*(\\d{1,2}))?");
    private static final Pattern SMALL_NUMBER_PATTERN = Pattern.compile("\\b(\\d{1,2})\\b");
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

        String scoreSource = firstNonEmpty(
                sectionText(snapshot.rawText, "left-score-binary"),
                sectionText(snapshot.rawText, "left-score"),
                sectionText(snapshot.rawText, "scoreboard-binary"),
                sectionText(snapshot.rawText, "scoreboard"),
                withoutSections(snapshot.rawText, "top-hud", "top-hud-binary", "full", "mini-map", "mini-map-binary")
        );
        String statsSource = firstNonEmpty(
                joinSections(snapshot.rawText,
                        "team-a-rows-binary", "team-b-rows-binary", "stat-columns-binary",
                        "team-a-rows", "team-b-rows", "stat-columns",
                        "scoreboard-binary", "scoreboard"),
                snapshot.rawText
        );
        String nameSource = firstNonEmpty(
                joinSections(snapshot.rawText,
                        "player-names-binary", "player-names",
                        "team-a-rows-binary", "team-b-rows-binary", "team-a-rows", "team-b-rows"),
                statsSource
        );

        snapshot.scoreText = parseScore(scoreSource);
        List<ScoreboardSnapshot.PlayerStat> tablePlayers = parsePlayerRows(statsSource);
        List<Integer> moneyValues = parseMoneyValues(statsSource);
        List<int[]> kdas = tablePlayers.isEmpty() ? parseKdaValues(statsSource) : kdasFromPlayers(tablePlayers);
        List<String> usernames = parseUsernames(nameSource);

        snapshot.moneyText = formatMoney(moneyValues);
        snapshot.kdaText = formatKda(kdas);
        snapshot.mapName = parseMap(snapshot.rawText);
        if (tablePlayers.isEmpty()) {
            snapshot.players.addAll(buildPlayers(usernames, moneyValues, kdas));
        } else {
            snapshot.players.addAll(tablePlayers);
        }
        snapshot.playerStatsText = buildPlayerSummary(snapshot.players);
        snapshot.hotHandSummary = summarizeHotHand(snapshot.players);
        return snapshot;
    }

    private static String parseScore(String rawText) {
        String source = cleanOcrText(rawText);
        String noAlive = source.replaceAll("\\d{1,2}\\s*/\\s*\\d{1,2}(?:\\s*/\\s*\\d{1,2})?", " ");
        List<Integer> standalone = new ArrayList<>();
        for (String line : noAlive.split("\\R+")) {
            String trimmed = line.trim();
            if (trimmed.matches("\\d{1,2}")) {
                int value = toInt(trimmed);
                if (value <= 30) {
                    standalone.add(value);
                }
            }
        }
        if (standalone.size() >= 2) {
            return standalone.get(0) + ":" + standalone.get(1);
        }

        Matcher matcher = SCORE_PATTERN.matcher(noAlive);
        while (matcher.find()) {
            String rightRaw = matcher.group(2);
            int left = toInt(matcher.group(1));
            int right = toInt(rightRaw);
            if (left > 30 || right > 30) {
                continue;
            }
            // Reject clock/timer like 0:03 or 1:09 where the seconds use a leading zero.
            if (rightRaw != null && rightRaw.length() == 2 && rightRaw.charAt(0) == '0') {
                continue;
            }
            return left + ":" + right;
        }
        List<Integer> numbers = smallNumbers(noAlive);
        if (numbers.size() >= 2) {
            return numbers.get(0) + ":" + numbers.get(1);
        }
        return "";
    }

    private static List<Integer> parseMoneyValues(String rawText) {
        LinkedHashSet<Integer> values = new LinkedHashSet<>();
        Matcher moneyMatcher = MONEY_PATTERN.matcher(cleanOcrText(rawText));
        while (moneyMatcher.find() && values.size() < 10) {
            int value = toInt(moneyMatcher.group(1));
            if (value >= 0 && value <= 20000) {
                values.add(value);
            }
        }
        return new ArrayList<>(values);
    }

    private static List<ScoreboardSnapshot.PlayerStat> parsePlayerRows(String rawText) {
        List<ScoreboardSnapshot.PlayerStat> players = new ArrayList<>();
        String[] lines = cleanOcrText(rawText).split("\\R+");
        for (String line : lines) {
            if (players.size() >= 10) {
                break;
            }
            String normalized = line.replaceAll("\\s+", " ").trim();
            if (normalized.length() < 8) {
                continue;
            }
            Matcher matcher = ROW_STATS_PATTERN.matcher(normalized);
            if (!matcher.find()) {
                continue;
            }
            int money = toInt(matcher.group(2));
            int kills = toInt(matcher.group(3));
            int deaths = toInt(matcher.group(4));
            int assists = toInt(matcher.group(5));
            if (money > 20000 || kills > 40 || deaths > 40 || assists > 40) {
                continue;
            }
            ScoreboardSnapshot.PlayerStat stat = new ScoreboardSnapshot.PlayerStat();
            stat.username = cleanPlayerName(matcher.group(1), players.size() + 1);
            stat.money = money;
            stat.kills = kills;
            stat.deaths = deaths;
            stat.assists = assists;
            players.add(stat);
        }
        return players;
    }

    private static List<int[]> kdasFromPlayers(List<ScoreboardSnapshot.PlayerStat> players) {
        List<int[]> values = new ArrayList<>();
        for (ScoreboardSnapshot.PlayerStat player : players) {
            values.add(new int[]{player.kills, player.deaths, player.assists});
        }
        return values;
    }

    private static List<int[]> parseKdaValues(String rawText) {
        List<int[]> values = new ArrayList<>();
        Matcher matcher = KDA_PATTERN.matcher(cleanOcrText(rawText));
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
        Matcher matcher = NAME_PATTERN.matcher(cleanOcrText(rawText));
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
            stat.username = i < names.size() ? names.get(i) : ("玩家" + (i + 1));
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
        return best.username + " 手感较热: K/D/A "
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

    private static String sectionText(String rawText, String label) {
        if (isEmpty(rawText) || isEmpty(label)) {
            return "";
        }
        String startMarker = "[" + label + "]";
        int start = rawText.indexOf(startMarker);
        if (start < 0) {
            return "";
        }
        int contentStart = start + startMarker.length();
        int next = rawText.indexOf("\n[", contentStart);
        int separator = rawText.indexOf("\n\n-----", contentStart);
        int end = next < 0 ? rawText.length() : next;
        if (separator >= 0 && separator < end) {
            end = separator;
        }
        return rawText.substring(contentStart, end).trim();
    }

    private static String joinSections(String rawText, String... labels) {
        StringBuilder builder = new StringBuilder();
        for (String label : labels) {
            String section = sectionText(rawText, label);
            if (isEmpty(section)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(section);
        }
        return builder.toString().trim();
    }

    private static String withoutSections(String rawText, String... labels) {
        if (isEmpty(rawText)) {
            return "";
        }
        String result = rawText;
        for (String label : labels) {
            result = result.replace(sectionText(rawText, label), "");
            result = result.replace("[" + label + "]", "");
        }
        return result.trim();
    }

    private static String cleanOcrText(String rawText) {
        if (rawText == null) {
            return "";
        }
        return rawText
                .replace('＄', '$')
                .replace('￥', '$')
                .replace('¥', '$')
                .replace('，', ',')
                .replace('：', ':')
                .replace('／', '/');
    }

    private static List<Integer> smallNumbers(String rawText) {
        List<Integer> values = new ArrayList<>();
        Matcher matcher = SMALL_NUMBER_PATTERN.matcher(rawText);
        while (matcher.find() && values.size() < 8) {
            int value = toInt(matcher.group(1));
            if (value <= 30) {
                values.add(value);
            }
        }
        return values;
    }

    private static String cleanPlayerName(String rawName, int index) {
        String value = rawName == null ? "" : rawName.replaceAll("[^\\p{L}0-9_\\-\\s]", " ").trim();
        String[] tokens = value.split("\\s+");
        for (int i = tokens.length - 1; i >= 0; i--) {
            String token = tokens[i].trim();
            if (token.length() < 2 || isNumeric(token)) {
                continue;
            }
            String lower = token.toLowerCase(Locale.ROOT);
            if (NAME_STOP_WORDS.contains(lower) || MAP_ALIASES.containsKey(lower)) {
                continue;
            }
            return token;
        }
        return "玩家" + index;
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (!isEmpty(value)) {
                return value;
            }
        }
        return "";
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
