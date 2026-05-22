package com.nhom1.auction.client.user.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.dto.bidding.AuctionDetailDto;
import com.nhom1.auction.common.dto.bidding.GetAuctionDetailRequest;
import com.nhom1.auction.common.dto.bidding.ListAuctionsResponse;
import com.nhom1.auction.common.dto.bidding.MyBidsResponse;
import com.nhom1.auction.common.dto.bidding.PlaceBidRequest;
import com.nhom1.auction.common.dto.bidding.PlaceBidResponse;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.utils.AppContext;

public class BiddingClientService extends BaseClientService {

	/**
	 * Fetch all active auctions for browsing.
	 * @return CompletableFuture that resolves to ListAuctionsResponse
	 */
	public CompletableFuture<ListAuctionsResponse> listAuctions() {
		RequestMessage<Void> request = new RequestMessage<>(MessageType.LIST_AUCTIONS, null);
		return send(request, ListAuctionsResponse.class);
	}

	public CompletableFuture<List<AuctionSummaryDto>> listBrowseAuctions() {
		return listAuctions()
			.thenCombine(
				getMyBids().exceptionally(ex -> new MyBidsResponse(Collections.emptyList())),
				this::filterBrowseAuctions
			);
	}

	private List<AuctionSummaryDto> filterBrowseAuctions(
		ListAuctionsResponse auctionsResponse,
		MyBidsResponse myBidsResponse
	) {
		if (auctionsResponse == null || auctionsResponse.getAuctions() == null) {
			return List.of();
		}

		String currentUserId = AppContext.getCurrentUser() != null
			? AppContext.getCurrentUser().getUserID()
			: null;
		Set<String> myBidAuctionIds = myBidsResponse == null || myBidsResponse.getBids() == null
			? Set.of()
			: myBidsResponse.getBids().stream()
				.map(bid -> bid.getAuctionId())
				.filter(id -> id != null && !id.isBlank())
				.collect(Collectors.toSet());

		return auctionsResponse.getAuctions().stream()
			.filter(auction -> auction.getId() != null && !myBidAuctionIds.contains(auction.getId()))
			.filter(auction -> currentUserId == null
				|| auction.getSellerId() == null
				|| !currentUserId.equals(auction.getSellerId()))
			.filter(auction -> auction.getStatus() == AuctionStatus.OPEN
				|| auction.getStatus() == AuctionStatus.RUNNING)
			.toList();
	}

	/**
	 * Fetch detailed information about a specific auction.
	 * @param auctionId UUID of the auction
	 * @return CompletableFuture that resolves to AuctionDetailDto
	 */
	public CompletableFuture<AuctionDetailDto> getAuctionDetail(String auctionId) {
		if (auctionId == null || auctionId.isBlank()) {
			return validationError("Auction ID is required.");
		}

		GetAuctionDetailRequest requestPayload = new GetAuctionDetailRequest(auctionId);
		RequestMessage<GetAuctionDetailRequest> request = new RequestMessage<>(
			MessageType.GET_AUCTION_DETAIL, requestPayload
		);
		return send(request, AuctionDetailDto.class);
	}

	/**
	 * Place a bid on an auction.
	 * Automatically fills bidderId from the current logged-in user.
	 * @param auctionId UUID of the auction to bid on
	 * @param amount The bid amount (must be > 0)
	 * @return CompletableFuture that resolves to PlaceBidResponse
	 */
	public CompletableFuture<PlaceBidResponse> placeBid(String auctionId, BigDecimal amount) {
		AuthResponse user = AppContext.getCurrentUser();
		if (user == null) {
			return validationError("User not logged in.");
		}

		if (auctionId == null || auctionId.isBlank()) {
			return validationError("Auction ID is required.");
		}

		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			return validationError("Bid amount must be greater than 0.");
		}

		PlaceBidRequest requestPayload = new PlaceBidRequest(auctionId, user.getUserID(), amount);
		RequestMessage<PlaceBidRequest> request = new RequestMessage<>(
			MessageType.PLACE_BID, requestPayload
		);
		return send(request, PlaceBidResponse.class);
	}

	/**
	 * Retrieve all bids placed by the current logged-in user.
	 * @return CompletableFuture that resolves to MyBidsResponse
	 */
	public CompletableFuture<MyBidsResponse> getMyBids() {
		AuthResponse user = AppContext.getCurrentUser();
		if (user == null) {
			return validationError("User not logged in.");
		}

		BidderRequest requestPayload = new BidderRequest(user.getUserID());
		RequestMessage<BidderRequest> request = new RequestMessage<>(
			MessageType.LIST_MY_BIDS, requestPayload
		);
		return send(request, MyBidsResponse.class);
	}

	/**
	 * Simple DTO for sending bidderId in LIST_MY_BIDS request.
	 */
	public static class BidderRequest {
		private String bidderId;

		public BidderRequest() {
		}

		public BidderRequest(String bidderId) {
			this.bidderId = bidderId;
		}

		public String getBidderId() {
			return bidderId;
		}

		public void setBidderId(String bidderId) {
			this.bidderId = bidderId;
		}
	}
}
