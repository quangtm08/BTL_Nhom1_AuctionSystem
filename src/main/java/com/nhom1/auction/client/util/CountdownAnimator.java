package com.nhom1.auction.client.util;

import java.time.Duration;
import java.time.LocalDateTime;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Label;

/**
 * Drives a per-second countdown label for an auction.
 * Adds CSS state classes ("warn" / "critical") so the view can highlight
 * the anti-sniping window.
 */
public class CountdownAnimator {

    private static final String CLASS_WARN = "warn";
    private static final String CLASS_CRITICAL = "critical";

    private static final long WARN_THRESHOLD_SECONDS = 60;
    private static final long CRITICAL_THRESHOLD_SECONDS = 15;

    private final Label target;
    private volatile LocalDateTime endTime;
    private Timeline timeline;
    private Runnable onExpired;
    private boolean expiredFired;

    public CountdownAnimator(Label target, LocalDateTime endTime) {
        this.target = target;
        this.endTime = endTime;
    }

    public void setOnExpired(Runnable onExpired) {
        this.onExpired = onExpired;
    }

    public void start() {
        stop();
        expiredFired = false;
        tick();
        timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> tick()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        target.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                stop();
            }
        });
    }

    public void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    public void resetEndTime(LocalDateTime newEndTime) {
        this.endTime = newEndTime;
        this.expiredFired = false;
        if (timeline == null) {
            start();
        } else {
            Platform.runLater(this::tick);
        }
    }

    private void tick() {
        LocalDateTime now = LocalDateTime.now();
        Duration remaining = Duration.between(now, endTime);
        long totalSeconds = remaining.getSeconds();

        if (totalSeconds <= 0) {
            target.setText("00:00");
            updateStateClasses(0);
            if (!expiredFired) {
                expiredFired = true;
                if (onExpired != null) {
                    onExpired.run();
                }
            }
            return;
        }

        target.setText(format(totalSeconds));
        updateStateClasses(totalSeconds);
    }

    private static String format(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (days > 0) {
            return String.format("%dd %02d:%02d:%02d", days, hours, minutes, seconds);
        }
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void updateStateClasses(long totalSeconds) {
        target.getStyleClass().removeAll(CLASS_WARN, CLASS_CRITICAL);
        if (totalSeconds <= CRITICAL_THRESHOLD_SECONDS) {
            target.getStyleClass().add(CLASS_CRITICAL);
        } else if (totalSeconds <= WARN_THRESHOLD_SECONDS) {
            target.getStyleClass().add(CLASS_WARN);
        }
    }
}
