package com.example.kin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.kin.model.ScoreboardSnapshot;
import com.example.kin.util.ScoreboardParser;

import org.junit.Test;

public class ScoreboardParserTest {
    @Test
    public void parse_shouldExtractScoreMoneyAndKda() {
        String raw = "Score 8:7\nTeam money $4200 $3100 $1800\nPlayerA 16/10/3 PlayerB 12/13/4";
        ScoreboardSnapshot snapshot = ScoreboardParser.parse(raw);
        assertEquals("8:7", snapshot.scoreText);
        assertTrue(snapshot.moneyText.contains("$4200"));
        assertTrue(snapshot.kdaText.contains("16/10/3"));
        assertTrue(snapshot.playerStatsText.contains("PlayerA"));
    }

    @Test
    public void parse_shouldExtractMapAndHotHandSummary() {
        String raw = "\u7ade\u6280\u6a21\u5f0f \u7099\u70ed\u6c99\u57ceII\nPlayerA $5000 18/10/2\nPlayerB $1200 9/12/4";
        ScoreboardSnapshot snapshot = ScoreboardParser.parse(raw);
        assertEquals("Dust II", snapshot.mapName);
        assertTrue(!snapshot.hotHandSummary.isEmpty());
    }

    @Test
    public void parse_shouldHandleEmptyInput() {
        ScoreboardSnapshot snapshot = ScoreboardParser.parse("");
        assertEquals("", snapshot.scoreText);
        assertEquals("", snapshot.moneyText);
        assertEquals("", snapshot.kdaText);
    }
}
