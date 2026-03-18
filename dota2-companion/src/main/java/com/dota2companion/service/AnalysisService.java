package com.dota2companion.service;

import com.dota2companion.api.ClaudeClient;
import com.dota2companion.model.Match;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import com.dota2companion.db.DatabaseManager;

public class AnalysisService {

    private final ClaudeClient claudeClient;
    private final DatabaseManager databaseManager;
    private final Gson gson = new Gson();

    public AnalysisService(ClaudeClient claudeClient, DatabaseManager databaseManager) {
        this.claudeClient = claudeClient;
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<String> analyzeMatch(long accountId, Match match) {
        JsonObject summary = new JsonObject();
        summary.addProperty("account_id", accountId);
        summary.addProperty("match_id", match.getMatchId());
        summary.addProperty("hero_name", match.getHeroName());
        summary.addProperty("kills", match.getKills());
        summary.addProperty("deaths", match.getDeaths());
        summary.addProperty("assists", match.getAssists());
        summary.addProperty("gpm", match.getGpm());
        summary.addProperty("xpm", match.getXpm());
        summary.addProperty("duration_seconds", match.getDurationSeconds());
        summary.addProperty("win", match.isPlayerWin());

        String json = gson.toJson(summary);

        return claudeClient.analyzeMatch("claude-3-5-sonnet-20241022", json)
                .thenApply(text -> {
                    persistNotes(accountId, match.getMatchId(), text);
                    return text;
                });
    }

    private void persistNotes(long accountId, long matchId, String notes) {
        String sql = """
                INSERT INTO ai_notes (match_id, account_id, notes, created_at)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, matchId);
            ps.setLong(2, accountId);
            ps.setString(3, notes);
            ps.setLong(4, Instant.now().getEpochSecond());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

