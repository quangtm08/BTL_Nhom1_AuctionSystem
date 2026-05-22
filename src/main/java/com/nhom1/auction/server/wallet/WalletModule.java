package com.nhom1.auction.server.wallet;

import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.NotificationService;
import javax.sql.DataSource;

public class WalletModule {

    public static WalletService init(
        DataSource dataSource,
        MessageRouter router,
        NotificationService notificationService
    ) {
        WalletRepository repository = new WalletRepository(dataSource);
        WalletService service = new WalletService(repository, notificationService);
        WalletHandler handler = new WalletHandler(service);
        handler.register(router);
        System.out.println("WalletModule: Feature initialized successfully.");
        return service;
    }
}
