package com.nhom1.auction.server.infrastructure;

import com.nhom1.auction.server.auction.AuctionModule;
import com.nhom1.auction.server.auth.AuthModule;
import com.nhom1.auction.server.auth.UserRepository;
import com.nhom1.auction.server.infrastructure.database.DBConnection;
import java.sql.Connection;

public class ServerContext {

    private final MessageRouter router;
    private final Connection connection;
    private final ClientRegistry clientRegistry;
    private final NotificationService notificationService;

    public ServerContext() throws Exception {
        // 1. Initialize shared Infrastructure
        this.router = new MessageRouter();
        this.connection = DBConnection.getConnection();

        if (this.connection == null) {
            throw new Exception("CRITICAL: Database connection failed.");
        }

        // Initialize Real-time Push Infrastructure
        this.clientRegistry = new ClientRegistry();
        this.notificationService = new NotificationService(clientRegistry);

        // 2. Initialize Features (Modules)
        // Auth — returns UserRepository so other modules can reuse it (e.g. for user validation)
        UserRepository userRepository = AuthModule.init(
            this.connection,
            this.router
        );

        // Auction — returns repositories needed by Bidding and Admin modules
        AuctionModule.AuctionRepositories auctionRepos = AuctionModule.init(
            this.connection,
            this.router
        );

        // Bidding — depends on Auction and Item repositories from AuctionModule
        com.nhom1.auction.server.bidding.BidModule.init(
            this.connection,
            this.router,
            auctionRepos.auctionRepository,
            auctionRepos.itemRepository,
            this.notificationService
        );

        System.out.println("========================================");
        System.out.println("   All modules wired successfully.");
        System.out.println("========================================");
    }

    public MessageRouter getRouter() {
        return router;
    }

    public ClientRegistry getClientRegistry() {
        return clientRegistry;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }
}
