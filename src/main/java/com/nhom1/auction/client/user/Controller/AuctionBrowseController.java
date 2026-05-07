package com.nhom1.auction.client.user.controller;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.connection.ServerConnection;
import com.nhom1.auction.client.user.service.BiddingClientService;
import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.protocol.MessageType;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class AuctionBrowseController {

	private final BiddingClientService biddingService = new BiddingClientService();

	@FXML
	private HBox mainContainer; // present in FXML as fx:id

	@FXML
	private GridPane cardsGridPane; // newly added for dynamic card rendering

	@FXML
	public void initialize() {
		// Tải danh sách auction lần đầu khi màn hình được khởi tạo.
		loadAuctions();

		// Đăng ký push handler cho sự kiện PUSH_NEW_AUCTION.
		// Server sẽ broadcast event này mỗi khi có người tạo một auction mới (AuctionHandler.java).
		// ServerConnection lưu handler trong ConcurrentHashMap<MessageType, Consumer<String>>,
		// nên đăng ký lại mỗi lần initialize() chạy chỉ đơn giản là ghi đè — không bị trùng lặp.
		// Khi nhận được push, ta re-fetch toàn bộ danh sách thay vì dùng dữ liệu từ payload,
		// vì payload chỉ có auctionId/itemName/startingPrice — không đủ để render card đầy đủ.
		ServerConnection.getInstance().registerPushHandler(
			MessageType.PUSH_NEW_AUCTION,
			json -> loadAuctions()
		);
	}

	/**
	 * Tải danh sách auction từ server, lọc ra những auction mà user hiện tại đã bid,
	 * sau đó render danh sách đã lọc lên GridPane.
	 *
	 * Method này được gọi:
	 * - Lúc khởi tạo màn hình (initialize)
	 * - Tự động khi nhận push PUSH_NEW_AUCTION (có auction mới được tạo bởi người khác)
	 *
	 * Luồng bất đồng bộ:
	 * 1. Gọi song song listAuctions() và getMyBids() qua socket TCP
	 * 2. thenCombine() hợp nhất kết quả: lọc ra các auction user chưa bid
	 * 3. thenAccept() cập nhật UI trên JavaFX Application Thread qua Platform.runLater()
	 */
	private void loadAuctions() {
		biddingService.listAuctions()
			.thenCombine(
				// getMyBids() có thể thất bại (ví dụ user chưa có bid nào) — dùng exceptionally
				// để trả về danh sách rỗng thay vì làm hỏng toàn bộ luồng.
				biddingService.getMyBids().exceptionally(ex -> {
					String msg = ex != null && ex.getCause() != null ? ex.getCause().getMessage() : "Unknown error";
					System.err.println("Failed to load my bids for explore filter: " + msg);
					return new com.nhom1.auction.common.dto.bidding.MyBidsResponse(Collections.emptyList());
				}),
				(auctionsResp, myBidsResp) -> {
				if (auctionsResp == null || auctionsResp.getAuctions() == null) {
					return java.util.List.<AuctionSummaryDto>of();
				}

				// Thu thập tập hợp auctionId mà user hiện tại đã đặt bid,
				// để loại ra khỏi danh sách Explore (tránh bid lại chính mình).
				Set<String> myBidAuctionIds = myBidsResp == null || myBidsResp.getBids() == null
					? Set.of()
					: myBidsResp.getBids().stream()
						.map(b -> b.getAuctionId())
						.filter(id -> id != null && !id.isBlank())
						.collect(Collectors.toSet());

				return auctionsResp.getAuctions().stream()
					.filter(a -> a.getId() != null && !myBidAuctionIds.contains(a.getId()))
					.toList();
			})
			.thenAccept(filtered -> Platform.runLater(() -> handleFilteredAuctions(filtered)))
			.exceptionally(ex -> {
				// ex là CompletionException bọc lỗi thật; getCause() có thể null nếu exception không có cause.
				String msg = (ex != null && ex.getCause() != null) ? ex.getCause().getMessage()
					: (ex != null ? ex.getMessage() : "Unknown error");
				Platform.runLater(() -> showError("Load auctions failed", msg));
				return null;
			});
	}

	/**
	 * Callback nhận danh sách auction đã được lọc, sau đó render lên GridPane
	 * và lưu auctionId đầu tiên vào AppContext (dùng làm default khi navigate sang detail).
	 *
	 * Được gọi trên JavaFX Application Thread (thông qua Platform.runLater trong loadAuctions).
	 */
	private void handleFilteredAuctions(List<AuctionSummaryDto> auctions) {
		if (auctions == null) return; 
		cardsGridPane.getChildren().clear();
		if (auctions.isEmpty()) return; // Nếu không có auction nào để hiển thị, giữ nguyên GridPane trống và không cần show error.
// Render lại toàn bộ card từ đầu theo danh sách đã lọc.
		renderAuctionCards(auctions);

		// Lưu auction đầu tiên làm "selected" mặc định để AuctionDetailController biết
		// cần hiển thị auction nào nếu user navigate sang detail mà chưa click card nào.
		String firstAuctionId = auctions.get(0).getId();
		com.nhom1.auction.common.utils.AppContext.setSelectedAuctionId(firstAuctionId);
	}

	/**
	 * Xóa toàn bộ card hiện tại trong GridPane rồi render lại từ đầu theo danh sách mới.
	 * Layout 2 cột: col = i % 2, row = i / 2.
	 *
	 * Được gọi cả khi load lần đầu lẫn khi refresh do nhận PUSH_NEW_AUCTION,
	 * nên getChildren().clear() là cần thiết để tránh card bị chồng lên nhau.
	 */
	private void renderAuctionCards(List<AuctionSummaryDto> auctions) {
		cardsGridPane.getChildren().clear();

		for (int i = 0; i < auctions.size(); i++) {
			AuctionSummaryDto auction = auctions.get(i);
			Node card = createAuctionCard(auction);
			int col = i % 2;
			int row = i / 2;
			cardsGridPane.add(card, col, row);
		}
	}

	/**
	 * Tạo một card auction dạng VBox từ dữ liệu AuctionSummaryDto.
	 *
	 * Cấu trúc card (từ trên xuống):
	 *   [topRow]    → tên auction + category | status badge
	 *   [spacer]    → khoảng trống cố định 50px
	 *   [bottomRow] → giá hiện tại | thời gian còn lại + nút "Raise bid"
	 *
	 * Nút "Raise bid" khi click sẽ lưu auctionId vào AppContext rồi navigate
	 * sang AUCTION_DETAIL qua navigateToDetail().
	 */
	private Node createAuctionCard(AuctionSummaryDto dto) {
		VBox card = new VBox(12);
		card.getStyleClass().add("product-card");

		// Top row: Title + Status badge
		HBox topRow = new HBox();
		topRow.setAlignment(Pos.CENTER_LEFT);
		
		VBox titleBox = new VBox(2);
		Label title = new Label(dto.getItemName() != null ? dto.getItemName() : "Untitled");
		title.getStyleClass().add("card-title-text");
		Label category = new Label(dto.getItemCategory() != null ? dto.getItemCategory() : "Uncategorized");
		category.getStyleClass().add("card-sub-text");
		titleBox.getChildren().addAll(title, category);

		Label statusBadge = new Label(formatStatus(dto.getStatus()));
		statusBadge.getStyleClass().add("status-badge");
		
		HBox.setHgrow(titleBox, Priority.ALWAYS);
		topRow.getChildren().addAll(titleBox, statusBadge);

		// Spacer
		VBox spacer = new VBox();
		spacer.setPrefHeight(50.0);

		// Bottom row: Price + Time + Button
		HBox bottomRow = new HBox();
		bottomRow.setAlignment(Pos.BOTTOM_LEFT);
		bottomRow.setSpacing(10);

		VBox priceSection = new VBox(2);
		Label priceLabel = new Label("Current bid");
		priceLabel.getStyleClass().add("card-sub-text");
		Label priceValue = new Label(formatMoney(dto.getCurrentHighestBid()));
		priceValue.getStyleClass().add("card-price-text");
		priceValue.setMaxWidth(Double.MAX_VALUE);
		priceSection.getChildren().addAll(priceLabel, priceValue);

		VBox timeSection = new VBox(2);
		timeSection.setAlignment(Pos.BOTTOM_RIGHT);
		timeSection.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
		Label timeLeftLabel = new Label(formatTimeLeft(dto.getEndTime()));
		timeLeftLabel.getStyleClass().add("card-sub-text");

		Button bidButton = new Button("Raise bid");
		bidButton.getStyleClass().add("btn-action-green");
		bidButton.setMaxWidth(Double.MAX_VALUE);
		timeSection.getChildren().addAll(timeLeftLabel, bidButton);

		HBox.setHgrow(priceSection, Priority.ALWAYS);
		bottomRow.getChildren().addAll(priceSection, timeSection);

		// Wire navigation button
		bidButton.setOnAction(e -> navigateToDetail(dto.getId()));

		// Assemble card
		card.getChildren().addAll(topRow, spacer, bottomRow);
		return card;
	}

	// In lỗi ra console — UI không hiển thị dialog để tránh block JavaFX thread.
	private void showError(String title, String message) {
		System.err.println(title + ": " + message);
	}

	/**
	 * Navigate sang màn hình chi tiết auction với hiệu ứng chuyển cảnh qua LOADING screen.
	 *
	 * Luồng: lưu auctionId vào AppContext → navigate LOADING (0.4s) → navigate AUCTION_DETAIL.
	 * AppContext.setSelectedAuctionId() là cách truyền dữ liệu giữa các controller
	 * vì JavaFX không có built-in navigation stack với parameter passing.
	 *
	 * Guard: nếu đang ở AUCTION_DETAIL rồi thì không navigate lại (tránh vòng lặp).
	 */
	public void navigateToDetail(String auctionId) {
		if (auctionId != null) com.nhom1.auction.common.utils.AppContext.setSelectedAuctionId(auctionId);
		if (AppNavigator.getCurrentView() == AppView.AUCTION_DETAIL) return;
		AppNavigator.navigateTo(AppView.LOADING);
		PauseTransition delay = new PauseTransition(Duration.seconds(0.4));
		delay.setOnFinished(e -> AppNavigator.navigateTo(AppView.AUCTION_DETAIL));
		delay.play();
	}

	// ================= HELPER METHODS =================

	// Chuyển enum AuctionStatus thành chuỗi hiển thị trên badge.
	private String formatStatus(Object status) {
		if (status == null) return "Unknown";
		return status.toString();
	}

	// Format số tiền theo định dạng USD với dấu phân cách hàng nghìn, ví dụ: $1,500,000.
	private String formatMoney(BigDecimal amount) {
		if (amount == null) return "$0";
		return "$" + NumberFormat.getNumberInstance(Locale.US).format(amount);
	}

	/**
	 * Tính thời gian còn lại cho đến khi auction kết thúc và trả về chuỗi thân thiện.
	 * Ưu tiên hiển thị đơn vị lớn nhất còn lại: ngày > giờ > phút > "Ended".
	 * Nếu endTime đã qua hiện tại, tất cả các ChronoUnit đều âm → trả về "Ended".
	 */
	private String formatTimeLeft(LocalDateTime endTime) {
		if (endTime == null) return "N/A";

		long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), endTime);
		long hoursLeft = ChronoUnit.HOURS.between(LocalDateTime.now(), endTime);
		long minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), endTime);

		if (daysLeft > 0) return daysLeft + " day" + (daysLeft > 1 ? "s" : "");
		if (hoursLeft > 0) return hoursLeft + " hour" + (hoursLeft > 1 ? "s" : "");
		if (minutesLeft > 0) return minutesLeft + " min" + (minutesLeft > 1 ? "s" : "");

		return "Ended";
	}
}
