package com.nhom1.auction.client.user.controller;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.connection.ServerConnection;
import com.nhom1.auction.client.user.service.BiddingClientService;
import com.nhom1.auction.common.dto.bidding.AuctionDetailDto;
import com.nhom1.auction.common.dto.bidding.BidSummaryDto;
import com.nhom1.auction.common.enums.BidType;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.utils.AppContext;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 * Controller cho màn hình biểu đồ lịch sử đấu giá.
 *
 * Render LineChart với X = thời gian bid (epoch second), Y = giá bid.
 * Mỗi điểm gắn Tooltip (bidder name + amount + type + time).
 * Realtime: subscribe PUSH_BID_UPDATE → re-fetch full AuctionDetail → redraw.
 *
 * Data: reuse {@link BiddingClientService#getAuctionDetail(String)} — không
 * đổi schema DB, không thêm DTO mới.
 */
public class BidHistoryChartController {

    private static final DateTimeFormatter TICK_FMT = DateTimeFormatter.ofPattern("HH:mm\ndd/MM");
    private static final DateTimeFormatter TOOLTIP_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final String BASELINE_SERIES_NAME = "__baseline__";

    private final BiddingClientService biddingService = new BiddingClientService();
    private final ObjectMapper mapper = new ObjectMapper();

    @FXML private Button btnBack;
    @FXML private Label lblSubtitle;
    @FXML private Label lblChartSub;
    @FXML private Label lblStatTotal;
    @FXML private Label lblStatHigh;
    @FXML private Label lblStatBidders;
    @FXML private Label lblEmpty;
    @FXML private StackPane chartContainer;
    @FXML private LineChart<Number, Number> bidChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;

    @FXML
    public void initialize() {
        String sel = AppContext.getSelectedAuctionId();
        if (sel == null || sel.isBlank()) {
            AppNavigator.navigateTo(AppView.AUCTION_BROWSE);
            return;
        }

        if (btnBack != null) {
            btnBack.setOnAction(e -> AppNavigator.navigateTo(AppView.AUCTION_DETAIL));
        }

        bidChart.setCreateSymbols(true);
        bidChart.setAnimated(false);
        bidChart.setLegendVisible(false);

        configureAxes();
        loadAndRender();

        ServerConnection.getInstance().registerPushHandler(
            MessageType.PUSH_BID_UPDATE,
            json -> handleBidUpdatePush(json)
        );
    }

    private void configureAxes() {
        xAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number value) {
                if (value == null) return "";
                LocalDateTime t = LocalDateTime.ofEpochSecond(
                    value.longValue(), 0, ZoneId.systemDefault().getRules().getOffset(LocalDateTime.now()));
                return TICK_FMT.format(t);
            }

            @Override
            public Number fromString(String s) {
                return 0;
            }
        });

        NumberFormat priceFmt = NumberFormat.getNumberInstance(Locale.US);
        yAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number value) {
                if (value == null) return "$0";
                return "$" + priceFmt.format(value.doubleValue());
            }

            @Override
            public Number fromString(String s) {
                return 0;
            }
        });
    }

    private void loadAndRender() {
        String auctionId = AppContext.getSelectedAuctionId();
        if (auctionId == null || auctionId.isBlank()) return;

        biddingService.getAuctionDetail(auctionId)
            .thenAccept(dto -> Platform.runLater(() -> applyDetail(dto)))
            .exceptionally(ex -> {
                Platform.runLater(() -> showError(extractMessage(ex)));
                return null;
            });
    }

    private void applyDetail(AuctionDetailDto dto) {
        if (dto == null) {
            showError("Auction not found");
            return;
        }

        List<BidSummaryDto> history = dto.getBidHistory() != null ? dto.getBidHistory() : List.of();

        // Defensive sort ASC by createdAt — chart cần time-series tăng dần
        List<BidSummaryDto> sorted = new ArrayList<>(history);
        sorted.sort(Comparator.comparing(BidSummaryDto::getCreatedAt,
            Comparator.nullsFirst(Comparator.naturalOrder())));

        // Header subtitle
        String itemName = dto.getItemName() != null ? dto.getItemName() : "auction";
        lblSubtitle.setText(String.format("Showing %d bid%s for %s",
            sorted.size(), sorted.size() == 1 ? "" : "s", itemName));

        // Stats
        long biddersCount = sorted.stream()
            .map(BidSummaryDto::getBidderId)
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .count();
        BigDecimal highest = dto.getCurrentHighestBid();
        if (highest == null || highest.compareTo(BigDecimal.ZERO) <= 0) {
            highest = dto.getStartingPrice();
        }
        lblStatTotal.setText(String.valueOf(sorted.size()));
        lblStatHigh.setText(formatMoney(highest));
        lblStatBidders.setText(String.valueOf(biddersCount));

        lblChartSub.setText("Last update " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        rebuildChart(dto, sorted);
    }

    private void rebuildChart(AuctionDetailDto dto, List<BidSummaryDto> bids) {
        bidChart.getData().clear();

        boolean empty = bids.isEmpty();
        lblEmpty.setVisible(empty);
        lblEmpty.setManaged(empty);
        bidChart.setVisible(true);

        // X-axis bounds: dùng startTime → endTime của auction
        LocalDateTime start = dto.getStartTime();
        LocalDateTime end = dto.getEndTime();
        long xStart = start != null ? epochSecondsOf(start) : minBidEpoch(bids);
        long xEnd = end != null ? epochSecondsOf(end) : maxBidEpoch(bids);
        if (xEnd <= xStart) {
            xEnd = xStart + 60; // fallback 1 phút
        }
        long range = xEnd - xStart;
        long pad = Math.max(1, range / 50); // padding 2%
        xAxis.setLowerBound(xStart - pad);
        xAxis.setUpperBound(xEnd + pad);
        xAxis.setTickUnit(Math.max(1, range / 8.0));

        // Build bid series first (default-color0 → main green line + symbols)
        XYChart.Series<Number, Number> bidSeries = null;
        if (!empty) {
            bidSeries = new XYChart.Series<>();
            bidSeries.setName("Bids");
            for (BidSummaryDto bid : bids) {
                if (bid.getCreatedAt() == null || bid.getAmount() == null) continue;
                XYChart.Data<Number, Number> data = new XYChart.Data<>(
                    epochSecondsOf(bid.getCreatedAt()),
                    bid.getAmount().doubleValue()
                );
                data.setExtraValue(bid);
                bidSeries.getData().add(data);
            }
            bidChart.getData().add(bidSeries);
        }

        // Baseline series — startingPrice from startTime → endTime
        // (default-color1 → dashed gray via CSS, symbols hidden)
        BigDecimal startingPrice = dto.getStartingPrice();
        if (startingPrice != null && start != null && end != null) {
            XYChart.Series<Number, Number> baseline = new XYChart.Series<>();
            baseline.setName(BASELINE_SERIES_NAME);
            baseline.getData().add(new XYChart.Data<>(xStart, startingPrice.doubleValue()));
            baseline.getData().add(new XYChart.Data<>(xEnd, startingPrice.doubleValue()));
            bidChart.getData().add(baseline);
        }

        // Style + tooltip cho mỗi marker sau khi node được tạo
        if (bidSeries != null) {
            final XYChart.Series<Number, Number> seriesRef = bidSeries;
            Platform.runLater(() -> decorateBidNodes(seriesRef));
        }
    }

    private void decorateBidNodes(XYChart.Series<Number, Number> series) {
        for (XYChart.Data<Number, Number> data : series.getData()) {
            Node node = data.getNode();
            if (node == null) continue;
            Object extra = data.getExtraValue();
            if (!(extra instanceof BidSummaryDto bid)) continue;

            String klass = bid.getBidType() == BidType.AUTO ? "bid-auto" : "bid-manual";
            if (!node.getStyleClass().contains(klass)) {
                node.getStyleClass().add(klass);
            }

            String tooltipText = String.format(
                "%s%n%s%n%s · %s",
                bid.getBidderName() != null ? bid.getBidderName() : "Unknown",
                formatMoney(bid.getAmount()),
                bid.getBidType() != null ? bid.getBidType().name() : "?",
                bid.getCreatedAt() != null ? bid.getCreatedAt().format(TOOLTIP_FMT) : "-"
            );
            Tooltip tip = new Tooltip(tooltipText);
            tip.setShowDelay(Duration.millis(120));
            tip.setHideDelay(Duration.millis(80));
            Tooltip.install(node, tip);

            node.setOnMouseEntered(e -> {
                node.setScaleX(1.5);
                node.setScaleY(1.5);
            });
            node.setOnMouseExited(e -> {
                node.setScaleX(1.0);
                node.setScaleY(1.0);
            });
        }
    }

    private void handleBidUpdatePush(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode node = root.has("payload") && !root.get("payload").isNull()
                ? root.get("payload") : root;
            String pushedAuctionId = node.has("auctionId") ? node.get("auctionId").asText() : null;
            String currentId = AppContext.getSelectedAuctionId();
            if (pushedAuctionId == null || !pushedAuctionId.equals(currentId)) {
                return;
            }
            // Match — refetch + redraw trên UI thread
            Platform.runLater(this::loadAndRender);
        } catch (Exception e) {
            System.err.println("Error parsing bid update push: " + e.getMessage());
        }
    }

    private void showError(String message) {
        bidChart.getData().clear();
        lblEmpty.setText(message != null ? message : "Failed to load");
        lblEmpty.setVisible(true);
        lblEmpty.setManaged(true);
        lblSubtitle.setText("Unable to load bid history");
    }

    private static long epochSecondsOf(LocalDateTime t) {
        return t.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private static long minBidEpoch(List<BidSummaryDto> bids) {
        return bids.stream()
            .map(BidSummaryDto::getCreatedAt)
            .filter(t -> t != null)
            .mapToLong(BidHistoryChartController::epochSecondsOf)
            .min()
            .orElse(LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond());
    }

    private static long maxBidEpoch(List<BidSummaryDto> bids) {
        return bids.stream()
            .map(BidSummaryDto::getCreatedAt)
            .filter(t -> t != null)
            .mapToLong(BidHistoryChartController::epochSecondsOf)
            .max()
            .orElse(LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond());
    }

    private static String formatMoney(BigDecimal amount) {
        if (amount == null) return "$0";
        return "$" + NumberFormat.getNumberInstance(Locale.US).format(amount);
    }

    private static String extractMessage(Throwable t) {
        if (t == null) return "Unknown error";
        if (t.getCause() != null) return t.getCause().getMessage();
        return t.getMessage();
    }
}
