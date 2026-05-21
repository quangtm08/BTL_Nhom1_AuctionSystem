package com.nhom1.auction.server.infrastructure;

import com.nhom1.auction.server.admin.AdminModule;
import com.nhom1.auction.server.auction.AuctionGatewayImpl;
import com.nhom1.auction.server.auction.AuctionModule;
import com.nhom1.auction.server.auth.AuthModule;
import com.nhom1.auction.server.auth.UserRepository;
import com.nhom1.auction.server.automation.AuctionGateway;
import com.nhom1.auction.server.automation.AuctionScheduler;
import com.nhom1.auction.server.automation.AutoBidModule;
import com.nhom1.auction.server.automation.AutoBidService;
import com.nhom1.auction.server.automation.BidGateway;
import com.nhom1.auction.server.bidding.BidGatewayImpl;
import com.nhom1.auction.server.bidding.BidModule;
import com.nhom1.auction.server.bidding.BidModule.BidComponents;
import com.nhom1.auction.server.bidding.BidRepository;
import com.nhom1.auction.server.infrastructure.database.DBConnection;
import com.nhom1.auction.server.payment.PaymentModule;
import com.nhom1.auction.server.infrastructure.database.DatabaseInitializer;
import javax.sql.DataSource;

public class ServerContext {

    private final MessageRouter router;
    private final DataSource dataSource;
    private final ClientRegistry clientRegistry;
    private final NotificationService notificationService;

    public ServerContext() throws Exception {
        // 1. Core Infrastructure
        this.router = new MessageRouter();
        this.dataSource = DBConnection.getDataSource();
        DatabaseInitializer.init(this.dataSource);

        this.clientRegistry = new ClientRegistry();
        this.notificationService = new NotificationService(clientRegistry);

        // 2. Core Modules (Auth, Auction, Bidding)
        UserRepository userRepository = AuthModule.init(
            this.dataSource,
            this.router,
            this.notificationService
        );

        AuctionModule.AuctionRepositories auctionRepos = AuctionModule.init(
            this.dataSource,
            this.router,
            this.notificationService
        );

        BidRepository bidRepository = new BidRepository(this.dataSource);
        BidComponents bidComponents = BidModule.init(
            this.dataSource,
            this.router,
            auctionRepos.auctionRepository,
            auctionRepos.itemRepository,
            auctionRepos.itemImageRepository,
            this.notificationService,
            userRepository
        );

        // 3. Automation & Scheduling
        AuctionGateway auctionGateway = new AuctionGatewayImpl(
            auctionRepos.auctionRepository
        );
        BidGateway bidGateway = new BidGatewayImpl(
            bidComponents.bidService,
            bidRepository
        );

        AutoBidService autoBidService = AutoBidModule.init(
            this.dataSource,
            this.router,
            bidGateway,
            this.notificationService
        );
        bidComponents.bidHandler.setAutoBidService(autoBidService); // Resolve circular dependency

        AuctionScheduler auctionScheduler = new AuctionScheduler(
            auctionGateway,
            bidGateway,
            this.notificationService
        );
        auctionScheduler.start();

        // 4. Admin Module
        AdminModule.init(
            this.router,
            userRepository,
            auctionRepos.auctionRepository,
            auctionRepos.itemRepository,
            bidRepository,
            this.notificationService,
            this.dataSource
        );

        // Payment — depends on Auction infrastructure
        PaymentModule.init(
            this.dataSource,
            this.router,
            auctionRepos.auctionRepository
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
