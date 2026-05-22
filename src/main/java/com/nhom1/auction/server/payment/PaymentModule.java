package com.nhom1.auction.server.payment;

import com.nhom1.auction.server.auction.AuctionRepository;
import com.nhom1.auction.server.infrastructure.MessageRouter;
import javax.sql.DataSource;

import com.nhom1.auction.server.wallet.WalletService;

public class PaymentModule {

    public static void init(DataSource dataSource, MessageRouter router, AuctionRepository auctionRepository, WalletService walletService) {
        PaymentRepository paymentRepository = new PaymentRepository(dataSource);
        PaymentService paymentService = new PaymentService(paymentRepository, auctionRepository, walletService, dataSource);
        PaymentHandler paymentHandler = new PaymentHandler(paymentService);
        paymentHandler.register(router);

        System.out.println("PaymentModule: Feature initialized successfully.");
    }
}
