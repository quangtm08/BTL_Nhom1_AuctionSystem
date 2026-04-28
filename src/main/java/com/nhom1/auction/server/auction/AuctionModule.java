package com.nhom1.auction.server.auction;

import java.sql.Connection;

import com.nhom1.auction.server.infrastructure.MessageRouter;

public class AuctionModule {

    public static AuctionRepository init(Connection connection, MessageRouter router) {
        ItemRepository itemRepository = new ItemRepository(connection);
        AuctionRepository auctionRepository = new AuctionRepository(connection);
        AuctionService auctionService = new AuctionService(auctionRepository, itemRepository);
        AuctionHandler auctionHandler = new AuctionHandler(auctionService);

        auctionHandler.register(router);

        System.out.println("AuctionModule: Feature initialized successfully.");
        return auctionRepository;
    }
}
