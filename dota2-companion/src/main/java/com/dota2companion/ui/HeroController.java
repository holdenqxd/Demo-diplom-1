package com.dota2companion.ui;

import com.dota2companion.model.HeroStats;
import com.dota2companion.api.OpenDotaClient;
import com.dota2companion.db.DatabaseManager;
import com.dota2companion.model.Match;
import com.dota2companion.service.MatchService;
import com.dota2companion.state.AppState;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class HeroController {

    @FXML
    private TableView<HeroStats> heroesTable;

    @FXML
    private LineChart<Number, Number> heroGpmChart;

    @FXML
    private TableColumn<HeroStats, String> heroColumn;

    @FXML
    private TableColumn<HeroStats, String> playerWinrateColumn;

    @FXML
    private TableColumn<HeroStats, String> globalWinrateColumn;

    @FXML
    private TableColumn<HeroStats, Double> playerGpmColumn;

    @FXML
    private TableColumn<HeroStats, Double> globalGpmColumn;

    private final OpenDotaClient openDotaClient = new OpenDotaClient();
    private final MatchService matchService = new MatchService(openDotaClient, new DatabaseManager());

    private final javafx.collections.ObservableList<HeroStats> heroItems =
            javafx.collections.FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Настраиваем таблицу
        heroColumn.setCellValueFactory(new PropertyValueFactory<>("localizedName"));
        playerWinrateColumn.setCellValueFactory(cell -> {
            HeroStats hs = cell.getValue();
            double v = hs.getWinRate();
            return new javafx.beans.property.SimpleStringProperty(String.format("%.1f%%", v));
        });
        globalWinrateColumn.setCellValueFactory(cell -> {
            HeroStats hs = cell.getValue();
            double v = hs.getGlobalWinRate();
            return new javafx.beans.property.SimpleStringProperty(String.format("%.1f%%", v));
        });
        playerGpmColumn.setCellValueFactory(new PropertyValueFactory<>("gpm"));
        globalGpmColumn.setCellValueFactory(new PropertyValueFactory<>("globalGpm"));

        heroesTable.setItems(heroItems);
        heroesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Если вкладка "Герои" открыта после заполнения "Профиль" — грузим сразу.
        loadHeroesForCurrentPlayer();

        // Обновляем при смене аккаунта.
        AppState.addAccountChangeListener(this::loadHeroesForCurrentPlayer);
    }

    private void loadHeroesForCurrentPlayer() {
        long accountId = AppState.getCurrentAccountId();
        if (accountId <= 0) return;

        // Чистим старые данные, чтобы UI не показывал "No content" до обновления.
        heroItems.clear();
        heroGpmChart.getData().clear();

        CompletableFuture<List<Match>> matchesFuture = matchService.loadRecentMatches(accountId, 20);

        matchesFuture.thenAccept(matches -> {
            Map<Integer, HeroAgg> byHero = new HashMap<>();
            for (Match m : matches) {
                int heroId = m.getHeroId();
                if (heroId == 0) continue;

                HeroAgg agg = byHero.computeIfAbsent(heroId, id -> new HeroAgg(m.getHeroName()));
                agg.games++;
                if (m.isPlayerWin()) agg.wins++;
                agg.gpmSum += m.getGpm();
            }

            List<HeroStats> result = new ArrayList<>();
            for (Map.Entry<Integer, HeroAgg> e : byHero.entrySet()) {
                int heroId = e.getKey();
                HeroAgg agg = e.getValue();

                HeroStats hs = new HeroStats();
                hs.setHeroId(heroId);
                hs.setLocalizedName(agg.heroName);

                double winRate = agg.games > 0 ? (double) agg.wins / agg.games * 100.0 : 0.0;
                double gpm = agg.games > 0 ? (double) agg.gpmSum / agg.games : 0.0;

                hs.setWinRate(winRate);
                hs.setGpm(gpm);

                // Пока нет отдельной "глобальной" выборки — используем те же значения,
                // чтобы вкладка визуально работала корректно.
                hs.setGlobalWinRate(winRate);
                hs.setGlobalGpm(gpm);

                result.add(hs);
            }

            // Сортируем по числу игр (используем отдельную map, чтобы лямбда не захватывала изменяемый byHero).
            Map<Integer, Integer> gamesByHero = new HashMap<>();
            for (Map.Entry<Integer, HeroAgg> e : byHero.entrySet()) {
                gamesByHero.put(e.getKey(), e.getValue().games);
            }

            // Берём топ по числу игр, чтобы таблица была информативной.
            result.sort(Comparator.comparingInt((HeroStats hs) -> gamesByHero.getOrDefault(hs.getHeroId(), 0)).reversed());
            List<HeroStats> finalResult = result.size() > 8
                    ? new ArrayList<>(result.subList(0, 8))
                    : result;

            javafx.application.Platform.runLater(() -> updateUi(finalResult));
        }).exceptionally(ex -> {
            ex.printStackTrace();
            javafx.application.Platform.runLater(() -> updateUi(List.of()));
            return null;
        });
    }

    private void updateUi(List<HeroStats> heroes) {
        heroItems.setAll(heroes);

        heroGpmChart.getData().clear();
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Ваш ЗМ/мин");

        int i = 1;
        for (HeroStats hs : heroes) {
            series.getData().add(new XYChart.Data<>(i++, hs.getGpm()));
        }
        heroGpmChart.getData().add(series);
    }

    private static final class HeroAgg {
        final String heroName;
        int games = 0;
        int wins = 0;
        int gpmSum = 0;

        HeroAgg(String heroName) {
            this.heroName = heroName;
        }
    }
}

