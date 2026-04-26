package com.nhom1.auction.server.infrastructure;

import com.nhom1.auction.server.auth.AuthModule;
import com.nhom1.auction.server.auth.UserRepository;
import com.nhom1.auction.server.infrastructure.database.DBConnection;
import java.sql.Connection;


public class ServerContext {
    private final MessageRouter router;
    private final Connection connection;

    public ServerContext() throws Exception {
        // 1. Initialize shared Infrastructure
        this.router = new MessageRouter();
        this.connection = DBConnection.getConnection();
        
        if (this.connection == null) {
            throw new Exception("CRITICAL: Database connection failed.");
        }

        // 2. Initialize Features (Modules)
        // Auth — returns UserRepository so other modules (Admin, Payment) can reuse it
        UserRepository userRepository = AuthModule.init(this.connection, this.router);

        // Future modules will be wired here by Member 4:
        // AuctionRepository auctionRepo = AuctionModule.init(connection, router);
        // BidModule.init(connection, router, auctionRepo, itemRepo, notificationService);
        // AdminModule.init(router, userRepository, adminAuctionGateway);
        // Admin branch is ready, but final wiring still depends on:
        // - Duy: concrete auction-side implementation for admin auction summaries
        // - Quang: ServerContext merge order after AuctionModule lands

    }

    public MessageRouter getRouter() {
        return router;
    }
}
