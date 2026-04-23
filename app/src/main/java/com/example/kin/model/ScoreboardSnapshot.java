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

        public double kdRatio() {
            return deaths <= 0 ? kills : (kills * 1.0d / deaths);
        }

        public String pretty() {
            String moneyText = money > 0 ? ("$" + money) : "N/A";
            return username + " K/D/A " + kills + "/" + deaths + "/" + assists + " | " + moneyText;
        }
    }
}
