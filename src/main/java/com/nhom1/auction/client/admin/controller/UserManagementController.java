package com.nhom1.auction.client.admin.controller;

import com.nhom1.auction.client.admin.service.AdminClientService;
import com.nhom1.auction.common.dto.admin.UserSummaryDto;
import com.nhom1.auction.common.enums.UserRole;
import com.nhom1.auction.client.user.connection.ServerConnection;
import com.nhom1.auction.common.protocol.MessageType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class UserManagementController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final AdminClientService adminClientService = new AdminClientService();
    private boolean loading;
    private String deletingUserId;

    @FXML private Label lblUserSummary;
    @FXML private GridPane userGrid;

    @FXML
    public void initialize() {
        reloadUsers();

        // Register push handlers so admin view updates in realtime
        ServerConnection.getInstance().registerPushHandler(
                MessageType.PUSH_USER_DELETED,
                json -> Platform.runLater(this::reloadUsers)
        );

        ServerConnection.getInstance().registerPushHandler(
                MessageType.PUSH_USER_CREATED,
                json -> Platform.runLater(this::reloadUsers)
        );
    }

    private void reloadUsers() {
        loading = true;
        lblUserSummary.setText("Loading users...");
        adminClientService.listUsers()
                .thenAccept(res -> Platform.runLater(() -> {
                    loading = false;
                    renderUsers(res.getUsers());
                }))
                .exceptionally(ex -> {
                    Throwable cause = AdminClientService.extractFailure(ex);
                    Platform.runLater(() -> {
                        loading = false;
                        lblUserSummary.setText("Load users failed: " + cause.getMessage());
                    });
                    return null;
                });
    }

    private void renderUsers(List<UserSummaryDto> users) {
        clearRowsFrom(1);
        List<UserSummaryDto> safeUsers = users != null ? users : List.of();
        for (int i = 0; i < safeUsers.size(); i++) {
            addRow(i + 1, safeUsers.get(i));
        }
        long adminCount = safeUsers.stream().filter(u -> u.getRole() == UserRole.ADMIN).count();
        long memberCount = Math.max(0, safeUsers.size() - adminCount);
        lblUserSummary.setText(safeUsers.size() + " total users - " + memberCount + " members, " + adminCount + " admins");
    }

    private void addRow(int row, UserSummaryDto user) {
        HBox bg = new HBox();
        bg.getStyleClass().add("table-row-bg");
        userGrid.add(bg, 0, row, 5, 1);

        Label username = styled(nvl(user.getUsername()), "table-text-main");
        Label email = styled(nvl(user.getEmail()), "table-text-sub");
        Label role = styled(user.getRole() != null ? user.getRole().name() : "USER",
                user.getRole() == UserRole.ADMIN ? "status-pill-banned" : "status-pill-active");
        Label createdAt = styled(formatDate(user.getCreatedAt()), "table-text-sub");

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-cancel");
        deleteBtn.setDisable(loading || user.getRole() == UserRole.ADMIN || user.getId().equals(deletingUserId));
        deleteBtn.setOnAction(e -> deleteUser(user.getId(), deleteBtn));

        HBox actions = new HBox(deleteBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        userGrid.add(username, 0, row);
        userGrid.add(email, 1, row);
        userGrid.add(role, 2, row);
        userGrid.add(createdAt, 3, row);
        userGrid.add(actions, 4, row);
    }

    private void deleteUser(String userId, Button deleteBtn) {
        deletingUserId = userId;
        deleteBtn.setDisable(true);
        lblUserSummary.setText("Deleting user...");
        adminClientService.deleteUser(userId)
                .thenAccept(ok -> Platform.runLater(() -> {
                    deletingUserId = null;
                    reloadUsers();
                }))
                .exceptionally(ex -> {
                    Throwable cause = AdminClientService.extractFailure(ex);
                    Platform.runLater(() -> {
                        deletingUserId = null;
                        deleteBtn.setDisable(false);
                        lblUserSummary.setText("Delete failed: " + cause.getMessage());
                    });
                    return null;
                });
    }

    private Label styled(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private void clearRowsFrom(int startRow) {
        userGrid.getChildren().removeIf(node -> {
            Integer row = GridPane.getRowIndex(node);
            return row != null && row >= startRow;
        });
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_FORMATTER);
    }

    private String nvl(String value) { return value == null || value.isBlank() ? "-" : value; }
}
