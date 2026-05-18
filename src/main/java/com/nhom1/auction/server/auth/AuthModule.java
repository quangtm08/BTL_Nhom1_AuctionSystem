package com.nhom1.auction.server.auth;

import com.nhom1.auction.server.infrastructure.MessageRouter;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

public class AuthModule {

    public static UserRepository init(DataSource dataSource, MessageRouter router) {
        try {
            Connection conn = dataSource.getConnection();
            UserRepository repository = new UserRepository(conn);
            AuthService service = new AuthService(repository);
            AuthHandler handler = new AuthHandler(service);
            handler.register(router);
            System.out.println("AuthModule: Feature initialized successfully.");
            return repository;
        } catch (SQLException e) {
            throw new RuntimeException("AuthModule: Failed to obtain connection from pool", e);
        }
    }
}
