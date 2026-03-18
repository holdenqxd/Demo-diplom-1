package com.dota2companion.ui;

import com.dota2companion.api.OpenDotaClient;
import com.dota2companion.db.DatabaseManager;
import com.dota2companion.model.Match;
import com.dota2companion.model.Player;
import com.dota2companion.service.MatchService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import com.dota2companion.state.AppState;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ProfileController {

    @FXML
    private TextField steamIdField;
    @FXML
    private Label playerNameLabel;
    @FXML
    private Label mmrLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    private TableView<Match> matchesTable;
    @FXML
    private TableColumn<Match, String> heroColumn;
    @FXML
    private TableColumn<Match, String> winColumn;
    @FXML
    private TableColumn<Match, String> kdaColumn;
    @FXML
    private TableColumn<Match, Integer> gpmColumn;
    @FXML
    private TableColumn<Match, Integer> xpmColumn;
    @FXML
    private TableColumn<Match, String> durationColumn;
    @FXML
    private TableColumn<Match, String> dateColumn;

    @FXML
    private LineChart<Number, Number> gpmChart;

    private final OpenDotaClient openDotaClient = new OpenDotaClient();
    private final MatchService matchService = new MatchService(openDotaClient, new DatabaseManager());

    private final ObservableList<Match> matchItems = FXCollections.observableArrayList();

    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    @FXML
    public void initialize() {
        heroColumn.setCellValueFactory(new PropertyValueFactory<>("heroName"));
        // Показываем победу как текст "Да"/"Нет", чтобы избежать проблем с Boolean-колонкой
        winColumn.setCellValueFactory(cellData -> {
            boolean win = cellData.getValue().isPlayerWin();
            String text = win ? "Да" : "Нет";
            return new javafx.beans.property.SimpleStringProperty(text);
        });
        gpmColumn.setCellValueFactory(new PropertyValueFactory<>("gpm"));
        xpmColumn.setCellValueFactory(new PropertyValueFactory<>("xpm"));

        kdaColumn.setCellValueFactory(cellData -> {
            Match m = cellData.getValue();
            String kda = m.getKills() + "/" + m.getDeaths() + "/" + m.getAssists();
            return new javafx.beans.property.SimpleStringProperty(kda);
        });

        durationColumn.setCellValueFactory(cellData -> {
            Match m = cellData.getValue();
            int minutes = m.getDurationSeconds() / 60;
            int seconds = m.getDurationSeconds() % 60;
            String text = String.format("%d:%02d", minutes, seconds);
            return new javafx.beans.property.SimpleStringProperty(text);
        });

        dateColumn.setCellValueFactory(cellData -> {
            Match m = cellData.getValue();
            if (m.getStartTime() != null) {
                String text = dateFormatter.format(m.getStartTime());
                return new javafx.beans.property.SimpleStringProperty(text);
            }
            return new javafx.beans.property.SimpleStringProperty("-");
        });

        // Растягиваем колонки, чтобы не было пустой области справа таблицы.
        matchesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        matchesTable.setItems(matchItems);

        steamIdField.setOnAction(e -> loadPlayer());
    }

    private void setLoading(boolean loading) {
        Platform.runLater(() -> {
            loadingIndicator.setVisible(loading);
            steamIdField.setDisable(loading);
        });
    }

    private void setStatus(String text) {
        Platform.runLater(() -> statusLabel.setText(text));
    }

    private void loadPlayer() {
        String query = steamIdField.getText();
        if (query == null || query.isBlank()) {
            setStatus("Пожалуйста, введите Steam ID или ник.");
            return;
        }

        setLoading(true);
        // Сбрасываем прошлые данные, пока идёт загрузка.
        playerNameLabel.setText("Игрок: -");
        mmrLabel.setText("MMR: нет данных");
        matchItems.clear();
        gpmChart.getData().clear();

        setStatus("Ищем игрока и загружаем статистику...");

        CompletableFuture<Long> accountIdFuture = openDotaClient.resolveAccountId(query);

        CompletableFuture<Void> loadFuture = accountIdFuture.thenCompose(accountId -> {
            CompletableFuture<Player> playerFuture = openDotaClient.fetchPlayer(accountId);
            CompletableFuture<List<Match>> matchesFuture = matchService.loadRecentMatches(accountId, 20);

            return playerFuture.thenCombine(matchesFuture, (player, matches) -> {
                Platform.runLater(() -> {
                    String personaName = player.getPersonaName() != null ? player.getPersonaName() : "-";

                    playerNameLabel.setText("Игрок: " + personaName);
                    mmrLabel.setText("MMR: " + (player.getMmrEstimate() >= 0 ? player.getMmrEstimate() : "нет данных"));
                    matchItems.setAll(matches);
                    updateGpmChart(matches);
                    setStatus("Загружено " + matches.size() + " матчей.");

                    // Сообщаем вкладке "Матчи", кого надо обновить.
                    AppState.setCurrentPlayer(accountId, personaName);
                });
                return null;
            });
        });

        loadFuture.exceptionally(ex -> {
            ex.printStackTrace();
            String msg = ex != null ? ex.getMessage() : null;
            if (msg == null && ex != null && ex.getCause() != null) {
                msg = ex.getCause().getMessage();
            }
            if (msg != null && msg.contains("Игрок не найден")) {
                setStatus("OpenDota не нашёл игрока по этому нику. Попробуйте точный personaname или введите SteamID64/account_id.");
            } else {
                setStatus("Ошибка при загрузке данных: " + msg);
            }
            return null;
        }).whenComplete((r, ex) -> setLoading(false));
    }

    private void updateGpmChart(List<Match> matches) {
        gpmChart.getData().clear();
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        int index = 1;
        for (Match match : matches) {
            series.getData().add(new XYChart.Data<>(index++, match.getGpm()));
        }
        series.setName("ЗМ/мин");
        gpmChart.getData().add(series);
    }
}

