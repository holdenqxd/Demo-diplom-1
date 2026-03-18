package com.dota2companion.service;

import com.dota2companion.api.OpenDotaClient;
import com.dota2companion.db.DatabaseManager;
import com.dota2companion.model.Match;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class MatchService {

    private final OpenDotaClient openDotaClient;
    private final DatabaseManager databaseManager;

    public MatchService(OpenDotaClient openDotaClient, DatabaseManager databaseManager) {
        this.openDotaClient = openDotaClient;
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<List<Match>> loadRecentMatches(long accountId, int limit) {
        return openDotaClient.fetchRecentMatches(accountId, limit)
                .thenApply(matches -> {
                    // Обогащаем матчи именами героев
                    try {
                        Map<Integer, String> heroes = openDotaClient.fetchHeroes()
                                .get(5, TimeUnit.SECONDS);
                        for (Match m : matches) {
                            int heroId = m.getHeroId();
                            if (heroId != 0) {
                                String name = heroes.get(heroId);
                                if (name != null) {
                                    m.setHeroName(name);
                                } else {
                                    m.setHeroName("Hero " + heroId);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    persistMatches(accountId, matches);
                    return matches;
                });
    }

    private void persistMatches(long accountId, List<Match> matches) {
        String sql = """
                INSERT INTO player_history (
                    account_id, match_id, hero_name, win, kda, gpm, xpm, duration_seconds, played_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Match m : matches) {
                ps.setLong(1, accountId);
                ps.setLong(2, m.getMatchId());
                ps.setString(3, m.getHeroName());
                ps.setInt(4, m.isPlayerWin() ? 1 : 0);
                String kda = m.getKills() + "/" + m.getDeaths() + "/" + m.getAssists();
                ps.setString(5, kda);
                ps.setInt(6, m.getGpm());
                ps.setInt(7, m.getXpm());
                ps.setInt(8, m.getDurationSeconds());
                ps.setLong(9, m.getStartTime() != null ? m.getStartTime().getEpochSecond() : 0);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

