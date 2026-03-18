package com.dota2companion.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:app.db";

    public DatabaseManager() {
        initialize();
    }

    private void initialize() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        account_id INTEGER NOT NULL,
                        match_id INTEGER NOT NULL,
                        hero_name TEXT,
                        win INTEGER,
                        kda TEXT,
                        gpm INTEGER,
                        xpm INTEGER,
                        duration_seconds INTEGER,
                        played_at INTEGER
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS ai_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        match_id INTEGER NOT NULL,
                        account_id INTEGER NOT NULL,
                        notes TEXT,
                        created_at INTEGER
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS mmr_progress (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        account_id INTEGER NOT NULL,
                        mmr INTEGER NOT NULL,
                        recorded_at INTEGER NOT NULL
                    )
                    """);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}

