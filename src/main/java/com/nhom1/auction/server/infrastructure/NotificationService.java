package com.nhom1.auction.server.infrastructure;

import java.math.BigDecimal;
import java.util.UUID;

public class NotificationService {
    public void broadcastBidUpdate(UUID auctionId, BigDecimal newBid, UUID newHighestBidderId){
    }

    public void broadcastAuctionEnded(UUID auctionId, UUID winnerId){};



}
