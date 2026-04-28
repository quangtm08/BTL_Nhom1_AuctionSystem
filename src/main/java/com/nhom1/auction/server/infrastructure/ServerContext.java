package com.nhom1.auction.server.infrastructure;

import com.nhom1.auction.server.auth.AuthModule;
import com.nhom1.auction.server.auth.UserRepository;
import com.nhom1.auction.server.auction.AuctionModule;
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
        AuctionModule.init(this.connection, this.router);

        // Future modules will be wired here by Member 4:
        // BidModule.init(connection, router, auctionRepo, itemRepo, notificationService);
        // AdminModule.init(connection, router, userRepository, auctionRepo);
        // PaymentModule.init(connection, router, auctionRepo, userRepository);

    }

    public MessageRouter getRouter() {
        return router;
    }
}
