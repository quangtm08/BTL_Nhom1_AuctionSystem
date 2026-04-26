package com.nhom1.auction.client.admin.controller;

import com.nhom1.auction.client.admin.service.AdminClientService;
import com.nhom1.auction.common.dto.admin.AdminAuctionListResponse;
import com.nhom1.auction.common.dto.admin.AdminUserListResponse;
import com.nhom1.auction.common.dto.admin.UserSummaryDto;
import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.enums.UserRole;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

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

    @FXML
    public void initialize() {
        lblOverviewDate.setText("System overview - "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        lblRecentActivityBody.setText("Loading admin dashboard data...");
        lblRecentActivityTime.setText("now");
        lblSessionStatus.setText("Loading...");

        CompletableFuture<AdminUserListResponse> usersFuture = adminClientService.listUsers();
        CompletableFuture<AdminAuctionListResponse> auctionsFuture = adminClientService.listAllAuctions();

        CompletableFuture.allOf(usersFuture, auctionsFuture)
                .thenRun(() -> Platform.runLater(() -> {
                    AdminUserListResponse users = usersFuture.join();
                    AdminAuctionListResponse auctions = auctionsFuture.join();
                    renderDashboard(users, auctions);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> renderFailure(ex));
                    return null;
                });
    }

    private void renderDashboard(AdminUserListResponse usersResponse, AdminAuctionListResponse auctionsResponse) {
        List<UserSummaryDto> users = usersResponse.getUsers() != null ? usersResponse.getUsers() : List.of();
        List<AuctionSummaryDto> auctions = auctionsResponse.getAuctions() != null ? auctionsResponse.getAuctions() : List.of();

        int totalUsers = users.size();
        long adminCount = users.stream().filter(user -> user.getRole() == UserRole.ADMIN).count();
        long memberCount = Math.max(0, totalUsers - adminCount);

        int totalAuctions = auctions.size();
        long runningAuctions = auctions.stream()
                .filter(auction -> AuctionStatus.RUNNING.name().equals(auction.getStatus()))
                .count();
        long finishedAuctions = auctions.stream()
                .filter(auction -> AuctionStatus.FINISHED.name().equals(auction.getStatus()))
                .count();

        lblTotalUsersValue.setText(String.valueOf(totalUsers));
        lblTotalUsersBreakdown.setText(memberCount + " Members | " + adminCount + " Admins");
        lblActiveAuctionsValue.setText(String.valueOf(runningAuctions));
        lblActiveAuctionsBreakdown.setText(totalAuctions + " total | " + finishedAuctions + " finished");

        // Team overlap note:
        // Recent activity feed is not defined in the current DTO/contracts, so we
        // use a safe summary placeholder until a real activity source exists.
        lblRecentActivityBody.setText("Loaded " + totalUsers + " users and " + totalAuctions
                + " auctions from the admin endpoints.");
        lblRecentActivityTime.setText("live");

        // Team overlap note:
        // Current shared DTOs are enough for counts but not for a richer chart
        // model, so the dashboard keeps this area textual for now.
        lblSessionStatus.setText(runningAuctions + " running / " + finishedAuctions + " finished");
    }

    private void renderFailure(Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        lblTotalUsersValue.setText("--");
        lblTotalUsersBreakdown.setText("Could not load users");
        lblActiveAuctionsValue.setText("--");
        lblActiveAuctionsBreakdown.setText("Could not load auctions");
        lblRecentActivityBody.setText("Admin dashboard failed to load: " + cause.getMessage());
        lblRecentActivityTime.setText("error");
        lblSessionStatus.setText("Unavailable");
    }
}
