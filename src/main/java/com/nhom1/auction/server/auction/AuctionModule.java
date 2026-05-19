package com.nhom1.auction.server.auction;

import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.NotificationService;
import javax.sql.DataSource;

public class AuctionModule {

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
        DataSource dataSource,
        MessageRouter router,
        NotificationService notificationService
    ) {
        ItemRepository itemRepository = new ItemRepository(dataSource);
        AuctionRepository auctionRepository = new AuctionRepository(dataSource);
        AuctionService auctionService = new AuctionService(
            auctionRepository,
            itemRepository,
            dataSource
        );
        AuctionHandler auctionHandler = new AuctionHandler(auctionService, notificationService);
        auctionHandler.register(router);
        System.out.println("AuctionModule: Feature initialized successfully.");
        return new AuctionRepositories(auctionRepository, itemRepository);
    }
}
