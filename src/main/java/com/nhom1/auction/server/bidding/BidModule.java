package com.nhom1.auction.server.bidding;

import com.nhom1.auction.server.auction.AuctionRepository;
import com.nhom1.auction.server.auction.ItemRepository;
import com.nhom1.auction.server.auth.UserRepository;
import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.NotificationService;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

public class BidModule {

	public static class BidComponents {
		public final BidService bidService;
		public final BidHandler bidHandler;
		public BidComponents(BidService bidService, BidHandler bidHandler) {
			this.bidService = bidService;
			this.bidHandler = bidHandler;
		}
	}

	public static BidComponents init(
			DataSource dataSource,
			MessageRouter router,
			AuctionRepository auctionRepository,
			ItemRepository itemRepository,
			NotificationService notificationService,
			UserRepository userRepository
	) {
		try {
			Connection conn = dataSource.getConnection();
			BidRepository repository = new BidRepository(conn);
			BidService service = new BidService(repository, auctionRepository, itemRepository, userRepository, conn);
			BidHandler handler = new BidHandler(service, notificationService);
			handler.register(router);
			System.out.println("BidModule: Feature initialized successfully.");
			return new BidComponents(service, handler);
		} catch (SQLException e) {
			throw new RuntimeException("BidModule: Failed to obtain connection from pool", e);
		}
	}
}
