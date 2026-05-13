package com.nhom1.auction.server.payment;

import com.nhom1.auction.server.auction.AuctionRepository;
import com.nhom1.auction.server.infrastructure.MessageRouter;
import java.sql.Connection;

public class PaymentModule {

    public static void init(Connection connection, MessageRouter router, AuctionRepository auctionRepository) {
        PaymentRepository paymentRepository = new PaymentRepository(connection);
        PaymentService paymentService = new PaymentService(paymentRepository, auctionRepository, connection);
        PaymentHandler paymentHandler = new PaymentHandler(paymentService);
        paymentHandler.register(router);

        System.out.println("PaymentModule: Feature initialized successfully.");
    }
}
