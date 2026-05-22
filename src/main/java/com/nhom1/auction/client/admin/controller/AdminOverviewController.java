package com.nhom1.auction.client.admin.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.nhom1.auction.client.admin.service.AdminClientService;
import com.nhom1.auction.common.dto.admin.AdminAuctionListResponse;
import com.nhom1.auction.common.dto.admin.AdminUserListResponse;
import com.nhom1.auction.common.dto.admin.UserSummaryDto;
import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.enums.UserRole;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class AdminOverviewController {
    private final AdminClientService adminClientService = new AdminClientService();

    @FXML private Label lblOverviewDate;
    @FXML private Label lblTotalUsersValue;
    @FXML private Label lblTotalUsersBreakdown;
    @FXML private Label lblActiveAuctionsValue;
    @FXML private Label lblActiveAuctionsBreakdown;
    @FXML private Label lblRecentActivityBody;
    @FXML private Label lblRecentActivityTime;
    @FXML private Label lblSessionStatus;
    @FXML private Circle circleSessionStatus;

    @FXML
    public void initialize() {
        lblOverviewDate.setText("System overview - "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        lblRecentActivityBody.setText("Loading admin dashboard data...");
        lblRecentActivityTime.setText("now");
        lblSessionStatus.setText("Loading...");

        adminClientService.listUsers()
                .thenCombine(adminClientService.listAllAuctions(), DashboardSnapshot::new)
                .thenAccept(snapshot -> Platform.runLater(() ->
                        renderDashboard(snapshot.usersResponse(), snapshot.auctionsResponse())))
                .exceptionally(ex -> {
                    Platform.runLater(() -> renderFailure(AdminClientService.extractFailure(ex)));
                    return null;
                });
    }

    private void renderDashboard(AdminUserListResponse usersResponse, AdminAuctionListResponse auctionsResponse) {
        List<UserSummaryDto> users = usersResponse.getUsers() != null ? usersResponse.getUsers() : List.of();
        List<AuctionSummaryDto> auctions = auctionsResponse.getAuctions() != null ? auctionsResponse.getAuctions() : List.of();

        long adminCount = users.stream().filter(user -> user.getRole() == UserRole.ADMIN).count();
        long memberCount = Math.max(0, users.size() - adminCount);
        long openAuctions = auctions.stream().filter(auction -> auction.getStatus() == AuctionStatus.OPEN).count();
        long runningAuctions = auctions.stream().filter(auction -> auction.getStatus() == AuctionStatus.RUNNING).count();
        long finishedAuctions = auctions.stream().filter(auction -> auction.getStatus() == AuctionStatus.FINISHED).count();
        long paidAuctions = auctions.stream().filter(auction -> auction.getStatus() == AuctionStatus.PAID).count();
        long canceledAuctions = auctions.stream().filter(auction -> auction.getStatus() == AuctionStatus.CANCELED).count();

        lblTotalUsersValue.setText(String.valueOf(users.size()));
        lblTotalUsersBreakdown.setText(memberCount + " members | " + adminCount + " admins");
        lblActiveAuctionsValue.setText(String.valueOf(runningAuctions));
        lblActiveAuctionsBreakdown.setText(openAuctions + " open | " + finishedAuctions + " finished | " + paidAuctions + " paid");
        lblRecentActivityBody.setText("Loaded " + users.size() + " users and " + auctions.size()
                + " auctions. Current mix: " + runningAuctions + " running, " + canceledAuctions + " canceled.");
        lblRecentActivityTime.setText("live");
        lblSessionStatus.setText(runningAuctions + " running / " + finishedAuctions + " finished / " + paidAuctions + " paid");
        updateSessionStatusCircle(runningAuctions, finishedAuctions, paidAuctions);
    }

    private void renderFailure(Throwable cause) {
        lblTotalUsersValue.setText("--");
        lblTotalUsersBreakdown.setText("Could not load users");
        lblActiveAuctionsValue.setText("--");
        lblActiveAuctionsBreakdown.setText("Could not load auctions");
        lblRecentActivityBody.setText("Admin dashboard failed to load: " + cause.getMessage());
        lblRecentActivityTime.setText("error");
        lblSessionStatus.setText("Unavailable");
        setCircleColor(Color.web("#1a221e"));
    }

    private void updateSessionStatusCircle(long runningAuctions, long finishedAuctions, long paidAuctions) {
        if (circleSessionStatus == null) return;
        if (runningAuctions > finishedAuctions) {
            setCircleColor(Color.web("#4d8055"));
        } else if (finishedAuctions > runningAuctions) {
            setCircleColor(Color.web("#d5a44c"));
        } else if (runningAuctions == 0 && finishedAuctions == 0 && paidAuctions == 0) {
            setCircleColor(Color.web("#1a221e"));
        } else {
            setCircleColor(Color.web("#4d8055"));
        }
    }

    private void setCircleColor(Color color) {
        if (circleSessionStatus != null) circleSessionStatus.setStroke(color);
    }

    private record DashboardSnapshot(
            AdminUserListResponse usersResponse,
            AdminAuctionListResponse auctionsResponse) {}
}
