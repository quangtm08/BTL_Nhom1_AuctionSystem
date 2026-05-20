package com.nhom1.auction.common.dto.bidding;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
public class AuctionDetailDto {
    private String auctionId;
    private String itemID;
    private String itemName;
    private String itemDescription;
    private ItemCategory itemCategory;
    private ItemCondition itemCondition;
    private String sellerID;
    private BigDecimal currentHighestBid; 
    private String currentHighestBidderId;
    private BigDecimal minBidIncrement;
    private AuctionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<BidSummaryDto> bidHistory;
    private String sellerName;
    private List<String> imageUrls;
    public AuctionDetailDto() {}
    public AuctionDetailDto(String auctionId, String itemID, String itemName, String itemDescription, ItemCategory itemCategory, ItemCondition itemCondition, String sellerID, BigDecimal currentHighestBid, String currentHighestBidderId, BigDecimal minBidIncrement, AuctionStatus status, LocalDateTime startTime, LocalDateTime endTime, List<BidSummaryDto> bidHistory) {
        this.auctionId = auctionId;
        this.itemID = itemID;
        this.itemName = itemName;
        this.itemDescription = itemDescription;
        this.itemCategory = itemCategory;
        this.itemCondition = itemCondition;
        this.sellerID = sellerID;
        this.currentHighestBid = currentHighestBid;
        this.currentHighestBidderId = currentHighestBidderId;
        this.minBidIncrement = minBidIncrement;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.bidHistory = bidHistory;
    }
    // Getters and Setters
    public String getAuctionId() {
        return auctionId;
    }
    public String getItemID() {
        return itemID;
    }
    public String getItemName() {
        return itemName;
    }
    public String getItemDescription() {
        return itemDescription;
    }
    public ItemCategory getItemCategory() {
        return itemCategory;
    }
    public ItemCondition getItemCondition() {
        return itemCondition;
    }
    public String getSellerID() {
        return sellerID;
    }
    public BigDecimal getCurrentHighestBid() {
        return currentHighestBid;
    }
    public String getCurrentHighestBidderId() {
        return currentHighestBidderId;
    }
    public BigDecimal getMinBidIncrement() {
        return minBidIncrement;
    }
    public void setMinBidIncrement(BigDecimal minBidIncrement) {
        this.minBidIncrement = minBidIncrement;
    }
    public AuctionStatus getStatus() {
        return status;
    }
    public LocalDateTime getStartTime() {
        return startTime;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }
    public List<BidSummaryDto> getBidHistory() {
        return bidHistory;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}
