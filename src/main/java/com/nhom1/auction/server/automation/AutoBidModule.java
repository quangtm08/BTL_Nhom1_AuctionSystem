package com.nhom1.auction.server.automation;

import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.NotificationService;
import java.sql.Connection;

public class AutoBidModule {

    /**
     * Cross-team dependency:
     * - caller (ServerContext owner) passes concrete BidGateway adapter from bidding module.
     */
    public static AutoBidService init(Connection connection, MessageRouter router,
                                      BidGateway bidGateway, NotificationService notificationService) {
        AutoBidRepository repository = new AutoBidRepository(connection);
        AutoBidService service = new AutoBidService(repository, bidGateway, notificationService);
        AutoBidHandler handler = new AutoBidHandler(service);
        handler.register(router);
        System.out.println("AutoBidModule: Feature initialized successfully.");
        return service;
    }
}
