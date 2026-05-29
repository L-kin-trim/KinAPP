package com.example.kin.model;

import java.util.ArrayList;
import java.util.List;

public class ScoreboardSnapshot {
    public String rawText = "";
    public String latinRawText = "";
    public String chineseRawText = "";

    public String scoreText = "";
    public String moneyText = "";
    public String kdaText = "";
    public String mapName = "";
    public String playerStatsText = "";
    public String hotHandSummary = "";

    public final List<PlayerStat> players = new ArrayList<>();

    public boolean hasCoreStats() {
        return !isEmpty(scoreText)
                || !isEmpty(moneyText)
                || !isEmpty(kdaText)
                || !isEmpty(mapName)
                || !isEmpty(rawText);
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class PlayerStat {
        public String username = "";
        public int money;
        public int kills;
        public int deaths;
        public int assists;
        public int headshotPercent = -1;
        public int damage = -1;

        public double kdRatio() {
            return deaths <= 0 ? kills : (kills * 1.0d / deaths);
        }

        public String pretty() {
            StringBuilder builder = new StringBuilder();
            builder.append(username)
                    .append(" K/D/A ")
                    .append(kills).append('/').append(deaths).append('/').append(assists);
            if (damage >= 0) {
                builder.append(" | 伤害 ").append(damage);
            }
            if (headshotPercent >= 0) {
                builder.append(" | 爆头 ").append(headshotPercent).append('%');
            }
            builder.append(" | ").append(money > 0 ? ("$" + money) : "未知");
            return builder.toString();
        }
    }
}
