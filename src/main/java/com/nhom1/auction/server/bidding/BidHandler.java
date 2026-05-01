package com.nhom1.auction.server.bidding;

import com.nhom1.auction.common.dto.bidding.AuctionDetailDto;
import com.nhom1.auction.common.dto.bidding.GetAuctionDetailRequest;
import com.nhom1.auction.common.dto.bidding.ListAuctionsResponse;
import com.nhom1.auction.common.dto.bidding.MyBidsResponse;
import com.nhom1.auction.common.dto.bidding.PlaceBidRequest;
import com.nhom1.auction.common.dto.bidding.PlaceBidResponse;
import com.nhom1.auction.common.entity.BidTransaction;
import com.nhom1.auction.common.exception.AuctionClosedException;
import com.nhom1.auction.common.exception.InvalidBidException;
import com.nhom1.auction.common.exception.UnauthorizedActionException;
import com.nhom1.auction.common.exception.ValidationException;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.common.utils.JsonUtil;
import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.NotificationService;

import java.util.UUID;

public class BidHandler {

	private final BidService bidService;
	private final NotificationService notificationService;

	public BidHandler(BidService bidService, NotificationService notificationService) {
		this.bidService = bidService;
		this.notificationService = notificationService;
	}

	public void register(MessageRouter router) {
		router.register(MessageType.PLACE_BID, (requestId, payloadJson) -> {
			try {
				PlaceBidRequest request = JsonUtil.fromJson(payloadJson, PlaceBidRequest.class);
				return handlePlaceBid(requestId, request);
			} catch (Exception e) {
				return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Invalid PLACE_BID payload");
			}
		});

		router.register(MessageType.LIST_AUCTIONS, (requestId, payloadJson) -> {
			try {
				return handleListAuctions(requestId);
			} catch (Exception e) {
				return new ResponseMessage<>(requestId, "LIST_AUCTIONS_FAILED", e.getMessage());
			}
		});

		router.register(MessageType.GET_AUCTION_DETAIL, (requestId, payloadJson) -> {
			try {
				GetAuctionDetailRequest request = JsonUtil.fromJson(payloadJson, GetAuctionDetailRequest.class);
				return handleGetAuctionDetail(requestId, request);
			} catch (Exception e) {
				return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Invalid GET_AUCTION_DETAIL payload");
			}
		});

		router.register(MessageType.LIST_MY_BIDS, (requestId, payloadJson) -> {
			try {
				BidderRequest request = JsonUtil.fromJson(payloadJson, BidderRequest.class);
				return handleListMyBids(requestId, request);
			} catch (Exception e) {
				return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Invalid LIST_MY_BIDS payload");
			}
		});
	}

	private ResponseMessage<PlaceBidResponse> handlePlaceBid(String requestId, PlaceBidRequest request) {
		try {
			UUID auctionId = UUID.fromString(request.getAuctionId());
			UUID bidderId = UUID.fromString(request.getBidderId());

			BidTransaction bidTransaction = bidService.placeBid(bidderId, auctionId, request.getBidAmount());
			notificationService.broadcastBidUpdate(auctionId, bidTransaction.getAmount(), bidTransaction.getBidderId());

			PlaceBidResponse response = new PlaceBidResponse(
					bidTransaction.getId().toString(),
					bidTransaction.getBidderId().toString(),
					bidTransaction.getAmount().toPlainString()
			);

			return new ResponseMessage<>(requestId, response);
		} catch (InvalidBidException e) {
			return new ResponseMessage<>(requestId, "INVALID_BID", e.getMessage());
		} catch (AuctionClosedException e) {
			return new ResponseMessage<>(requestId, "AUCTION_CLOSED", e.getMessage());
		} catch (UnauthorizedActionException e) {
			return new ResponseMessage<>(requestId, "UNAUTHORIZED", e.getMessage());
		} catch (ValidationException e) {
			return new ResponseMessage<>(requestId, "VALIDATION_ERROR", e.getMessage());
		} catch (IllegalArgumentException e) {
			// UUID parsing failed
			return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Invalid UUID format in request");
		} catch (Exception e) {
			return new ResponseMessage<>(requestId, "BID_FAILED", e.getMessage());
		}
	}

	private ResponseMessage<ListAuctionsResponse> handleListAuctions(String requestId) {
		try {
			ListAuctionsResponse response = bidService.listAllAuctions();
			return new ResponseMessage<>(requestId, response);
		} catch (Exception e) {
			return new ResponseMessage<>(requestId, "LIST_AUCTIONS_FAILED", e.getMessage());
		}
	}

	private ResponseMessage<AuctionDetailDto> handleGetAuctionDetail(String requestId, GetAuctionDetailRequest request) {
		try {
			UUID auctionId = UUID.fromString(request.getAuctionId());
			AuctionDetailDto response = bidService.getAuctionDetail(auctionId);
			return new ResponseMessage<>(requestId, response);
		} catch (ValidationException e) {
			return new ResponseMessage<>(requestId, "VALIDATION_ERROR", e.getMessage());
		} catch (IllegalArgumentException e) {
			// UUID parsing failed
			return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Invalid UUID format in request");
		} catch (Exception e) {
			return new ResponseMessage<>(requestId, "AUCTION_DETAIL_FAILED", e.getMessage());
		}
	}

	private ResponseMessage<MyBidsResponse> handleListMyBids(String requestId, BidderRequest request) {
		try {
			UUID bidderId = UUID.fromString(request.getBidderId());
			MyBidsResponse response = bidService.getMyBids(bidderId);
			return new ResponseMessage<>(requestId, response);
		} catch (IllegalArgumentException e) {
			// UUID parsing failed
			return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Invalid UUID format in request");
		} catch (Exception e) {
			return new ResponseMessage<>(requestId, "LIST_MY_BIDS_FAILED", e.getMessage());
		}
	}

	public static class BidderRequest {
		private String bidderId;

		public BidderRequest() {
		}

		public String getBidderId() {
			return bidderId;
		}

		public void setBidderId(String bidderId) {
			this.bidderId = bidderId;
		}
	}
}
