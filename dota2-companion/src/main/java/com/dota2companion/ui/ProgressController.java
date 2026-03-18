package com.dota2companion.ui;

import com.dota2companion.db.DatabaseManager;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;

public class ProgressController {

    @FXML
    private TextField mmrField;
    @FXML
    private BarChart<String, Number> weeklyBarChart;
    @FXML
    private LineChart<Number, Number> gpmTrendChart;
    @FXML
    private LineChart<Number, Number> xpmTrendChart;

    private final DatabaseManager databaseManager = new DatabaseManager();

    @FXML
    public void initialize() {
        XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
        barSeries.setName("Матчи");
        barSeries.getData().add(new XYChart.Data<>("Неделя 1", 10));
        barSeries.getData().add(new XYChart.Data<>("Неделя 2", 8));
        barSeries.getData().add(new XYChart.Data<>("Неделя 3", 12));
        weeklyBarChart.getData().add(barSeries);

        XYChart.Series<Number, Number> gpmSeries = new XYChart.Series<>();
        gpmSeries.setName("ЗМ/мин");
        gpmSeries.getData().add(new XYChart.Data<>(1, 400));
        gpmSeries.getData().add(new XYChart.Data<>(2, 450));
        gpmSeries.getData().add(new XYChart.Data<>(3, 500));
        gpmTrendChart.getData().add(gpmSeries);

        XYChart.Series<Number, Number> xpmSeries = new XYChart.Series<>();
        xpmSeries.setName("ОП/мин");
        xpmSeries.getData().add(new XYChart.Data<>(1, 500));
        xpmSeries.getData().add(new XYChart.Data<>(2, 550));
        xpmSeries.getData().add(new XYChart.Data<>(3, 600));
        xpmTrendChart.getData().add(xpmSeries);
    }

    @FXML
    private void saveMmr() {
        String text = mmrField.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        int mmr;
        try {
            mmr = Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return;
        }

        String sql = """
                INSERT INTO mmr_progress (account_id, mmr, recorded_at)
                VALUES (?, ?, ?)
                """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, 0);
            ps.setInt(2, mmr);
            ps.setLong(3, Instant.now().getEpochSecond());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

