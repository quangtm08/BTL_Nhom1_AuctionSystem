package com.nhom1.auction.server.bidding;

import com.nhom1.auction.server.auction.AuctionRepository;
import com.nhom1.auction.server.auction.ItemRepository;
import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.NotificationService;

import java.sql.Connection;

public class BidModule {

	public static BidService init(
			Connection connection,
			MessageRouter router,
			AuctionRepository auctionRepository,
			ItemRepository itemRepository,
			NotificationService notificationService
	) {
		BidRepository repository = new BidRepository(connection);
		BidService service = new BidService(repository, auctionRepository, itemRepository);
		BidHandler handler = new BidHandler(service, notificationService);
		handler.register(router);

		System.out.println("BidModule: Feature initialized successfully.");
		return service;
	}
}
