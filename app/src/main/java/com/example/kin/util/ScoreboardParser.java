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

        // 1. Score: prefer the two dedicated big-number crops on the left of each
        //    team block; fall back to scanning the whole text.
        String teamScore = parseTeamScore(snapshot.rawText);
        if (!isEmpty(teamScore)) {
            snapshot.scoreText = teamScore;
        } else {
            String scoreSource = firstNonEmpty(
                    sectionText(snapshot.rawText, "scoreboard-binary"),
                    sectionText(snapshot.rawText, "scoreboard"),
                    snapshot.rawText
            );
            snapshot.scoreText = parseScore(scoreSource);
        }

        // 2. Player rows: parse each team's fixed five rows. The last five numbers
        //    on a row are always 杀敌/死亡/助攻/爆%/伤害; an extra leading number is
        //    money (only shown for the viewer's own team).
        List<ScoreboardSnapshot.PlayerStat> binaryRows = parseCs2Rows(
                joinSections(snapshot.rawText, "team-a-rows-binary", "team-b-rows-binary"));
        List<ScoreboardSnapshot.PlayerStat> plainRows = parseCs2Rows(
                joinSections(snapshot.rawText, "team-a-rows", "team-b-rows"));
        List<ScoreboardSnapshot.PlayerStat> tablePlayers = chooseBetterRows(binaryRows, plainRows);

        snapshot.mapName = parseMap(snapshot.rawText);

        if (!tablePlayers.isEmpty()) {
            snapshot.players.addAll(tablePlayers);
            snapshot.moneyText = formatMoney(moneyFromPlayers(tablePlayers));
            snapshot.kdaText = formatKda(kdasFromPlayers(tablePlayers));
        } else {
            // Fallback for free-form text without fixed-layout sections.
            String statsSource = firstNonEmpty(
                    sectionText(snapshot.rawText, "scoreboard-binary"),
                    sectionText(snapshot.rawText, "scoreboard"),
                    snapshot.rawText);
            List<ScoreboardSnapshot.PlayerStat> rowPlayers = parsePlayerRows(statsSource);
            List<Integer> moneyValues = parseMoneyValues(statsSource);
            List<int[]> kdas = rowPlayers.isEmpty() ? parseKdaValues(statsSource) : kdasFromPlayers(rowPlayers);
            List<String> usernames = parseUsernames(statsSource);
            snapshot.moneyText = formatMoney(moneyValues);
            snapshot.kdaText = formatKda(kdas);
            if (rowPlayers.isEmpty()) {
                snapshot.players.addAll(buildPlayers(usernames, moneyValues, kdas));
            } else {
                snapshot.players.addAll(rowPlayers);
            }
        }

        snapshot.playerStatsText = buildPlayerSummary(snapshot.players);
        snapshot.hotHandSummary = summarizeHotHand(snapshot.players);
        return snapshot;
    }

    /**
     * Reads the two big team-score numbers from their dedicated crops and joins
     * them as "A:B". Returns empty when either crop produced no usable digit.
     */
    private static String parseTeamScore(String rawText) {
        String left = firstStandaloneScore(joinSections(rawText, "score-a-binary", "score-a"));
        String right = firstStandaloneScore(joinSections(rawText, "score-b-binary", "score-b"));
        if (left == null || right == null) {
            return "";
        }
        return left + ":" + right;
    }

    private static String firstStandaloneScore(String text) {
        if (isEmpty(text)) {
            return null;
        }
        Matcher matcher = SMALL_NUMBER_PATTERN.matcher(cleanOcrText(text));
        while (matcher.find()) {
            int value = toInt(matcher.group(1));
            if (value >= 0 && value <= 30) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    /**
     * Parses CS2 scoreboard rows. For each line the trailing run of integer
     * tokens is the stat tail; the last five are 杀敌/死亡/助攻/爆%/伤害 and a
     * sixth (leading) token, when present, is money. This is robust to player
     * names that contain digits because only the trailing numbers are used.
     */
    private static List<ScoreboardSnapshot.PlayerStat> parseCs2Rows(String rawText) {
        List<ScoreboardSnapshot.PlayerStat> players = new ArrayList<>();
        if (isEmpty(rawText)) {
            return players;
        }
        for (String line : cleanOcrText(rawText).split("\\R+")) {
            if (players.size() >= 10) {
                break;
            }
            String normalized = line.replaceAll("\\s+", " ").trim();
            if (normalized.isEmpty()) {
                continue;
            }
            String[] tokens = normalized.split(" ");
            List<Integer> tail = new ArrayList<>();
            int index = tokens.length - 1;
            while (index >= 0 && isStatToken(tokens[index])) {
                tail.add(0, statTokenValue(tokens[index]));
                index--;
            }
            if (tail.size() < 5) {
                continue;
            }
            int n = tail.size();
            int kills = tail.get(n - 5);
            int deaths = tail.get(n - 4);
            int assists = tail.get(n - 3);
            int headshot = tail.get(n - 2);
            int damage = tail.get(n - 1);
            if (kills > 60 || deaths > 60 || assists > 60 || headshot > 100 || damage > 9999) {
                continue;
            }
            ScoreboardSnapshot.PlayerStat stat = new ScoreboardSnapshot.PlayerStat();
            // Drop a leading standalone ping/level number (e.g. "45 指尖抚轻纱")
            // while keeping names that themselves contain digits (e.g. bj30380773).
            int nameStart = (index >= 1 && tokens[0].matches("\\d{1,3}")) ? 1 : 0;
            StringBuilder name = new StringBuilder();
            for (int i = nameStart; i <= index; i++) {
                if (name.length() > 0) {
                    name.append(' ');
                }
                name.append(tokens[i]);
            }
            stat.username = name.length() == 0 ? ("玩家" + (players.size() + 1)) : name.toString().trim();
            stat.kills = kills;
            stat.deaths = deaths;
            stat.assists = assists;
            stat.headshotPercent = headshot;
            stat.damage = damage;
            int money = n >= 6 ? tail.get(n - 6) : 0;
            stat.money = money > 16000 ? 0 : money;
            players.add(stat);
        }
        return players;
    }

    /**
     * Picks the OCR pass (binarized vs. enhanced) that produced the more
     * complete set of rows: more players first, then more recognised names.
     */
    private static List<ScoreboardSnapshot.PlayerStat> chooseBetterRows(
            List<ScoreboardSnapshot.PlayerStat> first,
            List<ScoreboardSnapshot.PlayerStat> second) {
        if (first.size() != second.size()) {
            return first.size() > second.size() ? first : second;
        }
        return namedCount(first) >= namedCount(second) ? first : second;
    }

    private static int namedCount(List<ScoreboardSnapshot.PlayerStat> players) {
        int count = 0;
        for (ScoreboardSnapshot.PlayerStat player : players) {
            if (!isEmpty(player.username) && !player.username.startsWith("玩家")) {
                count++;
            }
        }
        return count;
    }

    private static boolean isStatToken(String token) {
        if (token == null) {
            return false;
        }
        return token.matches("\\$?\\d{1,5}");
    }

    private static int statTokenValue(String token) {
        return toInt(token.replaceAll("[^0-9]", ""));
    }

    private static List<Integer> moneyFromPlayers(List<ScoreboardSnapshot.PlayerStat> players) {
        List<Integer> values = new ArrayList<>();
        for (ScoreboardSnapshot.PlayerStat player : players) {
            if (player.money > 0) {
                values.add(player.money);
            }
        }
        return values;
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
