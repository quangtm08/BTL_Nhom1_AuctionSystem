package com.nhom1.auction.server.bidding;

import com.nhom1.auction.server.auction.AuctionRepository;
import com.nhom1.auction.server.auction.ItemImageRepository;
import com.nhom1.auction.server.auction.ItemRepository;
import com.nhom1.auction.server.auth.UserRepository;
import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.NotificationService;

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
			ItemImageRepository itemImageRepository,
			NotificationService notificationService,
			UserRepository userRepository,
			com.nhom1.auction.server.wallet.WalletRepository walletRepository
	) {
		BidRepository repository = new BidRepository(dataSource);
		BidService service = new BidService(repository, auctionRepository, itemRepository, itemImageRepository, userRepository, walletRepository, dataSource);
		BidHandler handler = new BidHandler(service, notificationService);
		handler.register(router);
		System.out.println("BidModule: Feature initialized successfully.");
		return new BidComponents(service, handler);
	}
}
