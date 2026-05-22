package com.nhom1.auction.server.payment;

import com.nhom1.auction.common.dto.payment.PaymentHistoryResponse;
import com.nhom1.auction.common.dto.payment.PendingPaymentsResponse;
import com.nhom1.auction.common.dto.payment.ProcessPaymentResponse;
import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.exception.AppException;
import com.nhom1.auction.common.exception.InvalidAuctionStateException;
import com.nhom1.auction.common.exception.NotFoundException;
import com.nhom1.auction.common.exception.PaymentException;
import com.nhom1.auction.common.exception.UnauthorizedActionException;
import com.nhom1.auction.common.exception.ValidationException;
import com.nhom1.auction.server.auction.AuctionRepository;
import com.nhom1.auction.server.wallet.WalletService;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final AuctionRepository auctionRepository;
    private final WalletService walletService;
    private final DataSource dataSource;

    public PaymentService(PaymentRepository paymentRepository, AuctionRepository auctionRepository, WalletService walletService, DataSource dataSource) {
        this.paymentRepository = paymentRepository;
        this.auctionRepository = auctionRepository;
        this.walletService = walletService;
        this.dataSource = dataSource;
    }

    public PendingPaymentsResponse listPendingPayments(String bidderId) {
        UUID parsedBidderId = parseUuid(bidderId, "Bidder ID");
        return new PendingPaymentsResponse(paymentRepository.findPendingPaymentsByBidder(parsedBidderId));
    }

    public PaymentHistoryResponse listPaymentHistory(String userId) {
        UUID parsedUserId = parseUuid(userId, "User ID");
        return new PaymentHistoryResponse(paymentRepository.findPaymentHistoryForUser(parsedUserId));
    }

    public ProcessPaymentResponse processPayment(String auctionId, String bidderId) {
        UUID parsedAuctionId = parseUuid(auctionId, "Auction ID");
        UUID parsedBidderId = parseUuid(bidderId, "Bidder ID");

        Auction auction = auctionRepository.findById(parsedAuctionId)
                .orElseThrow(() -> new NotFoundException("Auction not found."));

        try (Connection connection = dataSource.getConnection()) {
            validatePaymentEligibility(auction, parsedBidderId, connection);

            LocalDateTime now = LocalDateTime.now();
            BigDecimal amount = auction.getCurrentHighestBid();

            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                paymentRepository.saveCompletedPayment(parsedAuctionId, parsedBidderId, auction.getSellerId(), amount, now, connection);
                walletService.transfer(parsedBidderId, auction.getSellerId(), amount, parsedAuctionId.toString(), "Payment for auction " + auction.getId(), connection);
                auctionRepository.updateStatus(parsedAuctionId, AuctionStatus.PAID, connection);
                connection.commit();

                // Trigger real-time wallet push updates post-commit
                try {
                    walletService.pushWalletUpdate(parsedBidderId);
                    walletService.pushWalletUpdate(auction.getSellerId());
                } catch (Exception e) {
                    System.err.println("[PaymentService] Error sending real-time wallet push updates: " + e.getMessage());
                }

                return new ProcessPaymentResponse(parsedAuctionId.toString(), amount, "COMPLETED", now);
            } catch (AppException e) {
                connection.rollback();
                throw e;
            } catch (Exception e) {
                connection.rollback();
                throw new RuntimeException("Payment transaction failed", e);
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to acquire connection for payment", e);
        }
    }

    private void validatePaymentEligibility(Auction auction, UUID bidderId, Connection connection) {
        if (auction.getHighestBidderId() == null || auction.getCurrentHighestBid() == null) {
            throw new ValidationException("Auction has no payable winning bid.");
        }
        if (!bidderId.equals(auction.getHighestBidderId())) {
            throw new UnauthorizedActionException("Only the winning bidder can pay for this auction.");
        }
        if (auction.getStatus() == AuctionStatus.PAID) {
            throw new InvalidAuctionStateException("Auction has already been paid.");
        }
        if (auction.getStatus() == AuctionStatus.CANCELED) {
            throw new InvalidAuctionStateException("Canceled auctions cannot be paid.");
        }
        if (auction.getStatus() != AuctionStatus.FINISHED) {
            throw new InvalidAuctionStateException("Only FINISHED auctions can be paid.");
        }
        if (paymentRepository.existsCompletedPaymentForAuction(auction.getId(), connection)) {
            throw new PaymentException("Payment has already been completed for this auction.");
        }
    }

    private UUID parseUuid(String rawId, String fieldName) {
        if (rawId == null || rawId.isBlank()) {
            throw new ValidationException(fieldName + " is required.");
        }
        try {
            return UUID.fromString(rawId);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(fieldName + " is invalid.");
        }
    }
}
