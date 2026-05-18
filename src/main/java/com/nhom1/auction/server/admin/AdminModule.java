package com.nhom1.auction.server.admin;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import com.nhom1.auction.server.auction.AuctionRepository;
import com.nhom1.auction.server.auction.ItemRepository;
import com.nhom1.auction.server.auth.UserRepository;
import com.nhom1.auction.server.bidding.BidRepository;
import com.nhom1.auction.server.infrastructure.MessageRouter;

public class AdminModule {

    public static void init(
            MessageRouter router,
            UserRepository userRepository,
            AuctionRepository auctionRepository,
            ItemRepository itemRepository,
            BidRepository bidRepository,
            DataSource dataSource) {
        try {
            Connection conn = dataSource.getConnection();
            AdminAuctionGateway adminAuctionGateway = new SqlAdminAuctionGateway(conn);
            AdminService adminService = new AdminService(
                userRepository, auctionRepository, itemRepository,
                bidRepository, adminAuctionGateway, conn);
            AdminHandler adminHandler = new AdminHandler(adminService);
            adminHandler.register(router);
            System.out.println("AdminModule: Feature initialized successfully.");
        } catch (SQLException e) {
            throw new RuntimeException("AdminModule: Failed to obtain connection from pool", e);
        }
    }
}
