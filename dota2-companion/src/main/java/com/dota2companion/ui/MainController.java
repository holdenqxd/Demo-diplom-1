package com.dota2companion.ui;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class MainController {

    @FXML
    private StackPane contentRoot;
    
    @FXML
    private HBox titleBar;
    
    @FXML
    private VBox sidebar;

    private Node profileView;
    private Node matchesView;
    private Node heroesView;
    private Node progressView;
    
    private double offsetX, offsetY;

    @FXML
    public void initialize() {
        try {
            profileView = loadView("/fxml/profile.fxml");
            contentRoot.getChildren().setAll(profileView);
            
            // Setup window dragging
            setupWindowDrag();
            
            // Setup button animations
            setupButtonAnimations();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void setupWindowDrag() {
        titleBar.setOnMousePressed(e -> {
            offsetX = e.getSceneX();
            offsetY = e.getSceneY();
        });
        
        titleBar.setOnMouseDragged(e -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();
            stage.setX(e.getScreenX() - offsetX);
            stage.setY(e.getScreenY() - offsetY);
        });
    }
    
    private void setupButtonAnimations() {
        // Add button press animations to nav buttons
        if (profileView != null) {
            addAnimationToButtons(sidebar);
        }
    }
    
    private void addAnimationToButtons(Node parent) {
        if (parent instanceof javafx.scene.Parent) {
            javafx.scene.Parent p = (javafx.scene.Parent) parent;
            for (javafx.scene.Node child : p.getChildrenUnmodifiable()) {
                if (child instanceof javafx.scene.control.Button) {
                    javafx.scene.control.Button btn = (javafx.scene.control.Button) child;
                    btn.setOnMousePressed(e -> {
                        btn.setScaleX(0.96);
                        btn.setScaleY(0.96);
                    });
                    btn.setOnMouseReleased(e -> {
                        btn.setScaleX(1.0);
                        btn.setScaleY(1.0);
                    });
                } else if (child instanceof javafx.scene.Parent) {
                    addAnimationToButtons(child);
                }
            }
        }
    }

    @FXML
    private void showProfile() {
        if (profileView == null) {
            try {
                profileView = loadView("/fxml/profile.fxml");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        switchTo(profileView);
    }

    @FXML
    private void showMatches() {
        if (matchesView == null) {
            try {
                matchesView = loadView("/fxml/match.fxml");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        switchTo(matchesView);
    }

    @FXML
    private void showHeroes() {
        if (heroesView == null) {
            try {
                heroesView = loadView("/fxml/heroes.fxml");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        switchTo(heroesView);
    }

    @FXML
    private void showProgress() {
        if (progressView == null) {
            try {
                progressView = loadView("/fxml/progress.fxml");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        switchTo(progressView);
    }

    @FXML
    private void minimizeWindow() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void maximizeWindow() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.close();
    }

    private void switchTo(Node view) {
        contentRoot.getChildren().setAll(view);
        FadeTransition ft = new FadeTransition(Duration.millis(250), view);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private Node loadView(String path) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
        return loader.load();
    }
}

