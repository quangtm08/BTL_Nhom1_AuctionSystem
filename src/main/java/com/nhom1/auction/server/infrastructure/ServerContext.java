package com.nhom1.auction.server.infrastructure;

import com.nhom1.auction.server.admin.AdminModule;
import com.nhom1.auction.server.admin.SqlAdminAuctionGateway;
import com.nhom1.auction.server.auth.AuthModule;
import com.nhom1.auction.server.auth.UserRepository;
import com.nhom1.auction.server.infrastructure.database.DBConnection;
import java.sql.Connection;


public class ServerContext {
    private final MessageRouter router;
    private final Connection connection;

    public ServerContext() throws Exception {
        this.router = new MessageRouter();
        this.connection = DBConnection.getConnection();

        if (this.connection == null) {
            throw new Exception("CRITICAL: Database connection failed.");
        }

        UserRepository userRepository = AuthModule.init(this.connection, this.router);

        AdminModule.init(this.router, userRepository, new SqlAdminAuctionGateway(this.connection));
    }

    public MessageRouter getRouter() {
        return router;
    }
}
