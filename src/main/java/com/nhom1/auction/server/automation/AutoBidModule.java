package com.nhom1.auction.server.automation;

import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.NotificationService;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

public class AutoBidModule {

    public static AutoBidService init(DataSource dataSource, MessageRouter router,
                                      BidGateway bidGateway, NotificationService notificationService) {
        try {
            Connection conn = dataSource.getConnection();
            AutoBidRepository repository = new AutoBidRepository(conn);
            AutoBidService service = new AutoBidService(repository, bidGateway, notificationService);
            AutoBidHandler handler = new AutoBidHandler(service);
            handler.register(router);
            System.out.println("AutoBidModule: Feature initialized successfully.");
            return service;
        } catch (SQLException e) {
            throw new RuntimeException("AutoBidModule: Failed to obtain connection from pool", e);
        }
    }
}
