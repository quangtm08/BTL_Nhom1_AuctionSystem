package com.nhom1.auction.server.admin;

import com.nhom1.auction.server.auction.AuctionRepository;
import com.nhom1.auction.server.auction.ItemRepository;
import com.nhom1.auction.server.auth.UserRepository;
import com.nhom1.auction.server.bidding.BidRepository;
import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.NotificationService;
import javax.sql.DataSource;

public class AdminModule {

  public static void init(
      MessageRouter router,
      UserRepository userRepository,
      AuctionRepository auctionRepository,
      ItemRepository itemRepository,
      BidRepository bidRepository,
      NotificationService notificationService,
      DataSource dataSource) {
    AdminAuctionGateway adminAuctionGateway = new SqlAdminAuctionGateway(dataSource);
    AdminService adminService =
        new AdminService(
            userRepository,
            auctionRepository,
            itemRepository,
            bidRepository,
            adminAuctionGateway,
            notificationService,
            dataSource);
    AdminHandler adminHandler = new AdminHandler(adminService);
    adminHandler.register(router);
    System.out.println("AdminModule: Feature initialized successfully.");
  }
}
