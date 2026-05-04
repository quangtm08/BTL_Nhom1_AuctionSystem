package com.nhom1.auction.server.admin;

import java.sql.Connection;

import com.nhom1.auction.server.auction.AuctionRepository;
import com.nhom1.auction.server.auction.ItemRepository;
import com.nhom1.auction.server.auth.UserRepository;
import com.nhom1.auction.server.bidding.BidRepository;
import com.nhom1.auction.server.infrastructure.MessageRouter;

public class AdminModule {

    /**
     * Integration points with other members:
     * - Duy: provides the auction-side implementation used by admin list auctions.
     * - Quang: wires this module in ServerContext after Auth/Auction modules exist.
     */
    public static void init(
            MessageRouter router,
            UserRepository userRepository,
            AuctionRepository auctionRepository,
            ItemRepository itemRepository,
            BidRepository bidRepository,
            AdminAuctionGateway adminAuctionGateway,
            Connection connection) {
        AdminService adminService = new AdminService(userRepository, auctionRepository, itemRepository, bidRepository, adminAuctionGateway, connection);
        AdminHandler adminHandler = new AdminHandler(adminService);
        adminHandler.register(router);

        System.out.println("AdminModule: Feature initialized successfully.");
    }
}
