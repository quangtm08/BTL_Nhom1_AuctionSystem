package com.nhom1.auction.server.auction;

import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.NotificationService;
import java.sql.Connection;
import java.sql.SQLException;
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
        try {
            ItemRepository itemRepository = new ItemRepository(dataSource);
            AuctionRepository auctionRepository = new AuctionRepository(dataSource);
            Connection conn = dataSource.getConnection(); // Service still uses Connection — Phase C migrates
            AuctionService auctionService = new AuctionService(
                auctionRepository,
                itemRepository,
                conn
            );
            AuctionHandler auctionHandler = new AuctionHandler(auctionService, notificationService);
            auctionHandler.register(router);
            System.out.println("AuctionModule: Feature initialized successfully.");
            return new AuctionRepositories(auctionRepository, itemRepository);
        } catch (SQLException e) {
            throw new RuntimeException("AuctionModule: Failed to obtain connection from pool", e);
        }
    }
}
