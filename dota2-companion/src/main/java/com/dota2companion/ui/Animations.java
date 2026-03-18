package com.dota2companion.ui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

public class Animations {

    /**
     * Fade in animation for a node
     * @param node The node to animate
     * @param durationMs Duration in milliseconds
     */
    public static void fadeIn(Node node, int durationMs) {
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    /**
     * Slide in from left animation with fade in
     * @param node The node to animate
     * @param durationMs Duration in milliseconds
     */
    public static void slideInFromLeft(Node node, int durationMs) {
        // Set initial position
        node.setTranslateX(-20);
        node.setOpacity(0);
        
        // Create transitions
        TranslateTransition tt = new TranslateTransition(Duration.millis(durationMs), node);
        tt.setFromX(-20);
        tt.setToX(0);
        
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        
        // Play both transitions together
        ParallelTransition pt = new ParallelTransition(tt, ft);
        pt.play();
    }

    /**
     * Button press animation - scales down slightly then back to normal
     * @param btn The button to animate
     */
    public static void buttonPress(javafx.scene.control.Button btn) {
        ScaleTransition st = new ScaleTransition(Duration.millis(80), btn);
        st.setToX(0.96);
        st.setToY(0.96);
        st.setCycleCount(2);
        st.setAutoReverse(true);
        st.play();
    }
}
