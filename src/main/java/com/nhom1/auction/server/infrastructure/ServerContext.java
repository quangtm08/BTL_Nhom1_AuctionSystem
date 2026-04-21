package com.nhom1.auction.server.infrastructure;

import com.nhom1.auction.server.auth.AuthModule;
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
        // Pass the shared Connection and Router to each feature
        AuthModule.init(this.connection, this.router);
        

    }

    public MessageRouter getRouter() {
        return router;
    }
}
