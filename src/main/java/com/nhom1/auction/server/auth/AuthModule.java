package com.nhom1.auction.server.auth;

import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.NotificationService;
import javax.sql.DataSource;

public class AuthModule {

    public static UserRepository init(
        DataSource dataSource,
        MessageRouter router,
        NotificationService notificationService
    ) {
        UserRepository repository = new UserRepository(dataSource);
        AuthService service = new AuthService(repository, notificationService);
        AuthHandler handler = new AuthHandler(service);
        handler.register(router);
        System.out.println("AuthModule: Feature initialized successfully.");
        return repository;
    }
}
