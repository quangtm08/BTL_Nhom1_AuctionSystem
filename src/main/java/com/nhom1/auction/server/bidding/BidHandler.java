package com.nhom1.auction.server.bidding;

import com.nhom1.auction.common.dto.bidding.AuctionDetailDto;
import com.nhom1.auction.common.dto.bidding.GetAuctionDetailRequest;
import com.nhom1.auction.common.dto.bidding.ListAuctionsResponse;
import com.nhom1.auction.common.dto.bidding.ListMyBidsRequest;
import com.nhom1.auction.common.dto.bidding.MyBidsResponse;
import com.nhom1.auction.common.dto.bidding.PlaceBidRequest;
import com.nhom1.auction.common.dto.bidding.PlaceBidResponse;
import com.nhom1.auction.common.entity.BidTransaction;
import com.nhom1.auction.common.enums.BidType;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.common.utils.JsonUtil;
import com.nhom1.auction.server.automation.AutoBidService;
import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.NotificationService;
import com.nhom1.auction.server.infrastructure.ResponseFactory;
import java.util.UUID;

public class BidHandler {

  private final BidService bidService;
  private final NotificationService notificationService;
  private AutoBidService autoBidService;

  public BidHandler(BidService bidService, NotificationService notificationService) {
    this.bidService = bidService;
    this.notificationService = notificationService;
  }

  public void setAutoBidService(AutoBidService autoBidService) {
    this.autoBidService = autoBidService;
  }

  public void register(MessageRouter router) {
    router.register(
        MessageType.PLACE_BID,
        (requestId, payloadJson) -> {
          try {
            PlaceBidRequest request = JsonUtil.fromJson(payloadJson, PlaceBidRequest.class);
            return handlePlaceBid(requestId, request);
          } catch (Exception e) {
            return ResponseFactory.invalidFormat(requestId, "Invalid PLACE_BID payload");
          }
        });

    router.register(
        MessageType.LIST_AUCTIONS,
        (requestId, payloadJson) -> {
          try {
            return handleListAuctions(requestId);
          } catch (Exception e) {
            return ResponseFactory.fromException(requestId, e);
          }
        });

    router.register(
        MessageType.GET_AUCTION_DETAIL,
        (requestId, payloadJson) -> {
          try {
            GetAuctionDetailRequest request =
                JsonUtil.fromJson(payloadJson, GetAuctionDetailRequest.class);
            return handleGetAuctionDetail(requestId, request);
          } catch (Exception e) {
            return ResponseFactory.invalidFormat(requestId, "Invalid GET_AUCTION_DETAIL payload");
          }
        });

    router.register(
        MessageType.LIST_MY_BIDS,
        (requestId, payloadJson) -> {
          try {
            ListMyBidsRequest request = JsonUtil.fromJson(payloadJson, ListMyBidsRequest.class);
            return handleListMyBids(requestId, request);
          } catch (Exception e) {
            return ResponseFactory.invalidFormat(requestId, "Invalid LIST_MY_BIDS payload");
          }
        });
  }

  private ResponseMessage<PlaceBidResponse> handlePlaceBid(
      String requestId, PlaceBidRequest request) {
    try {
      UUID auctionId = UUID.fromString(request.getAuctionId());
      UUID bidderId = UUID.fromString(request.getBidderId());

      BidTransaction bidTransaction =
          bidService.placeBid(bidderId, auctionId, request.getBidAmount(), BidType.MANUAL);
      notificationService.broadcastBidUpdate(
          auctionId, bidTransaction.getAmount(), bidTransaction.getBidderId());

      // Fire-and-forget: schedule auto-bids asynchronously, do NOT block
      if (autoBidService != null) {
        autoBidService.scheduleAutoBids(
            auctionId, bidTransaction.getAmount(), bidTransaction.getBidderId());
      }

      PlaceBidResponse response =
          new PlaceBidResponse(
              bidTransaction.getId().toString(),
              bidTransaction.getBidderId().toString(),
              bidTransaction.getAmount().toPlainString());
      return ResponseFactory.success(requestId, response);
    } catch (IllegalArgumentException e) {
      return ResponseFactory.invalidFormat(requestId, "Invalid UUID format in request");
    } catch (Exception e) {
      return ResponseFactory.fromException(requestId, e);
    }
  }

  private ResponseMessage<ListAuctionsResponse> handleListAuctions(String requestId) {
    try {
      return ResponseFactory.success(requestId, bidService.listAllAuctions());
    } catch (Exception e) {
      return ResponseFactory.fromException(requestId, e);
    }
  }

  private ResponseMessage<AuctionDetailDto> handleGetAuctionDetail(
      String requestId, GetAuctionDetailRequest request) {
    try {
      UUID auctionId = UUID.fromString(request.getAuctionId());
      return ResponseFactory.success(requestId, bidService.getAuctionDetail(auctionId));
    } catch (IllegalArgumentException e) {
      return ResponseFactory.invalidFormat(requestId, "Invalid UUID format in request");
    } catch (Exception e) {
      return ResponseFactory.fromException(requestId, e);
    }
  }

  private ResponseMessage<MyBidsResponse> handleListMyBids(
      String requestId, ListMyBidsRequest request) {
    try {
      UUID bidderId = UUID.fromString(request.getBidderId());
      return ResponseFactory.success(requestId, bidService.getMyBids(bidderId));
    } catch (IllegalArgumentException e) {
      return ResponseFactory.invalidFormat(requestId, "Invalid UUID format in request");
    } catch (Exception e) {
      return ResponseFactory.fromException(requestId, e);
    }
  }
}
