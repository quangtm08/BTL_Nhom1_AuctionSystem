package com.nhom1.auction.server.infrastructure;

import java.sql.Connection;

import com.nhom1.auction.server.admin.AdminModule;
import com.nhom1.auction.server.admin.SqlAdminAuctionGateway;
import com.nhom1.auction.server.auction.AuctionModule;
import com.nhom1.auction.server.auth.AuthModule;
import com.nhom1.auction.server.auth.UserRepository;
import com.nhom1.auction.server.infrastructure.database.DBConnection;

public class ServerContext {

    private final MessageRouter router;
    private final Connection connection;
    private final ClientRegistry clientRegistry;
    private final NotificationService notificationService;

    public ServerContext() throws Exception {
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
            this.router,
            this.notificationService
            
        );

        com.nhom1.auction.server.bidding.BidRepository bidRepository = new com.nhom1.auction.server.bidding.BidRepository(this.connection);

        // Bidding — depends on Auction and Item repositories from AuctionModule
        com.nhom1.auction.server.bidding.BidModule.BidComponents bidComponents = com.nhom1.auction.server.bidding.BidModule.init(
            this.connection,
            this.router,
            auctionRepos.auctionRepository,
            auctionRepos.itemRepository,
            this.notificationService
        );

        // 3. Initialize Automation features
        com.nhom1.auction.server.automation.AuctionGateway auctionGateway = 
            new com.nhom1.auction.server.auction.AuctionGatewayImpl(auctionRepos.auctionRepository);
            
        com.nhom1.auction.server.automation.BidGateway bidGateway = 
            new com.nhom1.auction.server.bidding.BidGatewayImpl(bidComponents.bidService, bidRepository);

        com.nhom1.auction.server.automation.AutoBidService autoBidService = 
            com.nhom1.auction.server.automation.AutoBidModule.init(this.connection, this.router, bidGateway);

        // Resolve circular dependency
        bidComponents.bidHandler.setAutoBidService(autoBidService);

        com.nhom1.auction.server.automation.AuctionScheduler auctionScheduler = 
            new com.nhom1.auction.server.automation.AuctionScheduler(auctionGateway, bidGateway, this.notificationService);
        auctionScheduler.start();

        // Admin — depends on Auth and Auction infrastructure
        AdminModule.init(
            this.router,
            userRepository,
            auctionRepos.auctionRepository,
            auctionRepos.itemRepository,
            bidRepository,
            new SqlAdminAuctionGateway(this.connection),
            this.connection
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
