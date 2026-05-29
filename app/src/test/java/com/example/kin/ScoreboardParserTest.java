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

    @Test
    public void parse_shouldReadGeometryReconstructedScoreboard() {
        // Rows reconstructed from element bounding boxes for the real Dust II
        // scoreboard: "<ping> <name> [<money>] <k> <d> <a> <hs%> <dmg>".
        String raw = "[ocr-full]\n竞技模式 炙热沙城II The Verkkars - EZ4ENCE\n\n"
                + "[score-a]\n9\n\n[score-b]\n2\n\n"
                + "[team-a-rows]\n"
                + "45 指尖抚轻纱 9 8 8 66 1209\n"
                + "28 城南花已开c 14 1 4 28 1178\n"
                + "28 bj30380773 12 1 1 25 888\n"
                + "37 只见江南雨 4 7 5 25 787\n"
                + "56 XuSilas 8 8 3 12 740\n"
                + "26 L_kin 0 11 10 1 45 1180\n"
                + "30 载物弟弟哉种 0 4 8 4 25 666\n"
                + "36 阿福xxx 5650 4 9 1 50 592\n"
                + "29 Shanghai1tao 150 4 11 4 0 437\n"
                + "41 用户5061991 1200 5 9 0 20 405\n";
        ScoreboardSnapshot snapshot = ScoreboardParser.parse(raw);

        assertEquals("9:2", snapshot.scoreText);
        assertEquals("Dust II", snapshot.mapName);
        assertEquals(10, snapshot.players.size());

        ScoreboardSnapshot.PlayerStat top = findPlayer(snapshot, "指尖抚轻纱");
        assertEquals(9, top.kills);
        assertEquals(8, top.deaths);
        assertEquals(1209, top.damage);

        ScoreboardSnapshot.PlayerStat bj = findPlayer(snapshot, "bj30380773");
        assertEquals(12, bj.kills);
        assertEquals(888, bj.damage);

        ScoreboardSnapshot.PlayerStat lkin = findPlayer(snapshot, "L_kin");
        assertEquals(11, lkin.kills);
        assertEquals(10, lkin.deaths);
        assertEquals(1180, lkin.damage);
        assertEquals(0, lkin.money);

        ScoreboardSnapshot.PlayerStat user = findPlayer(snapshot, "用户5061991");
        assertEquals(5, user.kills);
        assertEquals(1200, user.money);

        ScoreboardSnapshot.PlayerStat afu = findPlayer(snapshot, "阿福");
        assertEquals(5650, afu.money);
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
