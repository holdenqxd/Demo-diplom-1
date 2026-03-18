package com.dota2companion.ui;

import com.dota2companion.api.OpenDotaClient;
import com.dota2companion.db.DatabaseManager;
import com.dota2companion.model.Match;
import com.dota2companion.service.MatchService;
import com.dota2companion.state.AppState;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MatchController {

    @FXML
    private TableView<Match> scoreboardTable;
    @FXML
    private LineChart<Number, Number> netWorthChart;
    @FXML
    private LineChart<Number, Number> killTimelineChart;

    @FXML
    private TableColumn<Match, String> playerColumn;
    @FXML
    private TableColumn<Match, String> heroColumn;
    @FXML
    private TableColumn<Match, Integer> killsColumn;
    @FXML
    private TableColumn<Match, Integer> deathsColumn;
    @FXML
    private TableColumn<Match, Integer> assistsColumn;
    @FXML
    private TableColumn<Match, Integer> gpmColumn;
    @FXML
    private TableColumn<Match, Integer> xpmColumn;

    private final OpenDotaClient openDotaClient = new OpenDotaClient();
    private final MatchService matchService = new MatchService(openDotaClient, new DatabaseManager());

    private final ObservableList<Match> matchItems = FXCollections.observableArrayList();
    private String currentPlayerName = "";

    @FXML
    public void initialize() {
        // Колонки таблицы "Детали матча" (показываем сводку по недавним матчам)
        playerColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(currentPlayerName));
        heroColumn.setCellValueFactory(new PropertyValueFactory<>("heroName"));
        killsColumn.setCellValueFactory(new PropertyValueFactory<>("kills"));
        deathsColumn.setCellValueFactory(new PropertyValueFactory<>("deaths"));
        assistsColumn.setCellValueFactory(new PropertyValueFactory<>("assists"));
        gpmColumn.setCellValueFactory(new PropertyValueFactory<>("gpm"));
        xpmColumn.setCellValueFactory(new PropertyValueFactory<>("xpm"));

        scoreboardTable.setItems(matchItems);
        scoreboardTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Обновляем вкладку "Матчи" при изменении выбранного игрока в "Профиль"
        AppState.addAccountChangeListener(() -> loadForCurrentPlayer());

        // На случай, если вкладка "Матчи" открылась после загрузки игрока в "Профиле"
        // (т.е. событие AppState уже произошло).
        loadForCurrentPlayer();
    }

    private void loadForCurrentPlayer() {
        long accountId = AppState.getCurrentAccountId();
        if (accountId <= 0) {
            return;
        }
        currentPlayerName = AppState.getCurrentPersonaName();

        CompletableFuture<List<Match>> matchesFuture = matchService.loadRecentMatches(accountId, 20);
        matchesFuture.thenAccept(matches -> Platform.runLater(() -> updateUi(matches)));
    }

    private void updateUi(List<Match> matches) {
        matchItems.setAll(matches);
        scoreboardTable.refresh();

        // График "Чистое золото по времени": используем gpm как прокси.
        netWorthChart.getData().clear();
        XYChart.Series<Number, Number> netWorthSeries = new XYChart.Series<>();
        netWorthSeries.setName("ЗМ/мин");
        int i = 1;
        for (Match m : matches) {
            netWorthSeries.getData().add(new XYChart.Data<>(i++, m.getGpm()));
        }
        netWorthChart.getData().add(netWorthSeries);

        // График "Хронология убийств": kills по матчам.
        killTimelineChart.getData().clear();
        XYChart.Series<Number, Number> killsSeries = new XYChart.Series<>();
        killsSeries.setName("Убийства");
        i = 1;
        for (Match m : matches) {
            killsSeries.getData().add(new XYChart.Data<>(i++, m.getKills()));
        }
        killTimelineChart.getData().add(killsSeries);
    }
}

