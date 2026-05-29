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
    public void parse_shouldIgnoreCountdownTimerAsScore() {
        String raw = "[left-score]\n13\n7\n[top-hud]\n0:03";
        ScoreboardSnapshot snapshot = ScoreboardParser.parse(raw);
        assertEquals("13:7", snapshot.scoreText);
    }

    @Test
    public void parse_shouldNotReadLeadingZeroSecondsAsScore() {
        String raw = "Round timer 1:09 then 9:12 final";
        ScoreboardSnapshot snapshot = ScoreboardParser.parse(raw);
        assertEquals("9:12", snapshot.scoreText);
    }

    @Test
    public void parse_shouldReadFixedCs2Layout() {
        String raw = "[map-title]\n竞技模式 炙热沙城II\n\n[score-a]\n9\n\n[score-b]\n2\n\n"
                + "[team-a-rows]\n45 指尖抚轻纱 9 8 8 66 1209\n"
                + "28 bj30380773 12 1 1 25 888\n\n"
                + "[team-b-rows]\n26 L_kin 0 11 10 1 45 1180\n"
                + "36 阿福xxx 5650 4 9 1 50 592";
        ScoreboardSnapshot snapshot = ScoreboardParser.parse(raw);

        assertEquals("9:2", snapshot.scoreText);
        assertEquals("Dust II", snapshot.mapName);

        ScoreboardSnapshot.PlayerStat lkin = findPlayer(snapshot, "L_kin");
        assertEquals(11, lkin.kills);
        assertEquals(10, lkin.deaths);
        assertEquals(1, lkin.assists);
        assertEquals(1180, lkin.damage);

        // Player name with embedded digits must not corrupt the trailing stats.
        ScoreboardSnapshot.PlayerStat bj = findPlayer(snapshot, "bj30380773");
        assertEquals(12, bj.kills);
        assertEquals(1, bj.deaths);
        assertEquals(888, bj.damage);

        ScoreboardSnapshot.PlayerStat afu = findPlayer(snapshot, "阿福");
        assertEquals(5650, afu.money);
        assertEquals(4, afu.kills);
    }

    private static ScoreboardSnapshot.PlayerStat findPlayer(ScoreboardSnapshot snapshot, String nameFragment) {
        for (ScoreboardSnapshot.PlayerStat player : snapshot.players) {
            if (player.username != null && player.username.contains(nameFragment)) {
                return player;
            }
        }
        throw new AssertionError("player not found: " + nameFragment);
    }

    @Test
    public void parse_shouldHandleEmptyInput() {
        ScoreboardSnapshot snapshot = ScoreboardParser.parse("");
        assertEquals("", snapshot.scoreText);
        assertEquals("", snapshot.moneyText);
        assertEquals("", snapshot.kdaText);
    }
}
