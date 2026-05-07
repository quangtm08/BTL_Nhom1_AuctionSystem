package com.nhom1.auction.server.auction;

import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.NotificationService;
import java.sql.Connection;

public class AuctionModule {

    /**
     * Container to hold repositories created within the AuctionModule.
     * This allows the Coordinator (ServerContext) to pass these repositories
     * to other modules (like BidModule or AdminModule) that depend on them.
     */
    public static class AuctionRepositories {

        public final AuctionRepository auctionRepository;
        public final ItemRepository itemRepository;

        public AuctionRepositories(
            AuctionRepository auctionRepository,
            ItemRepository itemRepository
        ) {
            this.auctionRepository = auctionRepository;
            this.itemRepository = itemRepository;
        }
    }

    public static AuctionRepositories init(
        Connection connection,
        MessageRouter router,
        NotificationService notificationService
    ) {
        ItemRepository itemRepository = new ItemRepository(connection);
        AuctionRepository auctionRepository = new AuctionRepository(connection);
        AuctionService auctionService = new AuctionService(
            auctionRepository,
            itemRepository,
            connection
        );
        AuctionHandler auctionHandler = new AuctionHandler(auctionService, notificationService);

        auctionHandler.register(router);

        System.out.println("AuctionModule: Feature initialized successfully.");
        return new AuctionRepositories(auctionRepository, itemRepository);
    }
}
