package com.nhom1.auction.server.automation;

import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.NotificationService;
import javax.sql.DataSource;

public class AutoBidModule {

    public static AutoBidService init(DataSource dataSource, MessageRouter router,
                                      AuctionGateway auctionGateway,
                                      BidGateway bidGateway, NotificationService notificationService) {
        AutoBidRepository repository = new AutoBidRepository(dataSource);
        AutoBidService service = new AutoBidService(repository, auctionGateway, bidGateway, notificationService);
        AutoBidHandler handler = new AutoBidHandler(service);
        handler.register(router);
        System.out.println("AutoBidModule: Feature initialized successfully.");
        return service;
    }
}
