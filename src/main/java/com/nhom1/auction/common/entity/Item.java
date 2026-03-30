package com.nhom1.auction.common.entity;

import java.time.LocalDateTime;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;

public abstract class Item extends BaseEntity {

    protected String name;
    protected String description;
    protected double startingPrice;
    protected double currentHighestBid;
    protected boolean isFirstBid;

    protected LocalDateTime startTime;
    protected LocalDateTime endTime;
    protected ItemCategory category;
    protected ItemCondition condition;

    public Item(String id, String name, String description, double startingPrice,
        LocalDateTime startTime, LocalDateTime endTime,
        ItemCategory category, ItemCondition condition) {
        super(id);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentHighestBid = startingPrice;
        this.isFirstBid = true;

        this.startTime = startTime;
        this.endTime = endTime;
        this.category = category;
        this.condition = condition;
    }

    public abstract void printInfo();

    // Getter
    public String getName() { return name; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public ItemCategory getCategory() { return category; }
    public LocalDateTime getEndTime() { return endTime; }

    protected double calculateDynamicIncrement(double currentPrice, int bidderCount) {
        // Phòng trường hợp dữ liệu lỗi truyền vào số âm
        int activeBidders = Math.max(0, bidderCount);
        //Bước giá phụ thuộc thị trường
        //nhiều người tham gia,bước nhảy càng gắt
        double calculatedIncrement = currentPrice * (0.02 + 0.01 * activeBidders);

        // Đảm bảo bước nhảy tối thiểu luôn là $1.0, tránh trường hợp giá sản phẩm quá thấp
        return Math.max(1.0, calculatedIncrement);
    }


    // Đã xóa phần "throws AuctionClosedException, InvalidBidException"
    public void placeBid(double newBid, int bidderCount) {

        // Chặn nếu hết giờ - Dùng Exception có sẵn của Java (IllegalStateException)
        if (LocalDateTime.now().isAfter(endTime)) {
            throw new IllegalStateException("Time's up! Auction for '" + name + "' has ended ");
        }

        // Tính bước giá dựa vào lượng người tham gia
        double currentIncrement = calculateDynamicIncrement(currentHighestBid, bidderCount);

        // Chốt giá sàn
        double minAcceptedBid = isFirstBid ? startingPrice : (currentHighestBid + currentIncrement);

        // Nếu trả giá thấp -> báo lỗi bằng Exception có sẵn (IllegalArgumentException)
        if (newBid < minAcceptedBid) {
            throw new IllegalArgumentException(
                String.format("Too low! %d persons want this. You have to bid at least $%.2f",
                    bidderCount, minAcceptedBid)
            );
        }

        // Ghi nhận giá mới
        this.currentHighestBid = newBid;
        this.isFirstBid = false;
    }
}