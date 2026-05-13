package com.nhom1.auction.server.payment;

import com.nhom1.auction.common.dto.payment.PaymentHistoryResponse;
import com.nhom1.auction.common.dto.payment.PendingPaymentsResponse;
import com.nhom1.auction.common.dto.payment.ProcessPaymentResponse;
import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.exception.InvalidAuctionStateException;
import com.nhom1.auction.common.exception.NotFoundException;
import com.nhom1.auction.common.exception.PaymentException;
import com.nhom1.auction.common.exception.UnauthorizedActionException;
import com.nhom1.auction.common.exception.ValidationException;
import com.nhom1.auction.server.auction.AuctionRepository;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final AuctionRepository auctionRepository;
    private final Connection connection;

    public PaymentService(PaymentRepository paymentRepository, AuctionRepository auctionRepository, Connection connection) {
        this.paymentRepository = paymentRepository;
        this.auctionRepository = auctionRepository;
        this.connection = connection;
    }

    public PendingPaymentsResponse listPendingPayments(String bidderId) throws ValidationException {
        UUID parsedBidderId = parseUuid(bidderId, "Bidder ID");
        return new PendingPaymentsResponse(paymentRepository.findPendingPaymentsByBidder(parsedBidderId));
    }

    public PaymentHistoryResponse listPaymentHistory(String userId) throws ValidationException {
        UUID parsedUserId = parseUuid(userId, "User ID");
        return new PaymentHistoryResponse(paymentRepository.findPaymentHistoryForUser(parsedUserId));
    }

    public ProcessPaymentResponse processPayment(String auctionId, String bidderId)
            throws ValidationException, NotFoundException, InvalidAuctionStateException, UnauthorizedActionException, PaymentException {
        UUID parsedAuctionId = parseUuid(auctionId, "Auction ID");
        UUID parsedBidderId = parseUuid(bidderId, "Bidder ID");

        Auction auction = auctionRepository.findById(parsedAuctionId)
                .orElseThrow(() -> new NotFoundException("Auction not found."));
        validatePaymentEligibility(auction, parsedBidderId);

        LocalDateTime now = LocalDateTime.now();
        BigDecimal amount = auction.getCurrentHighestBid();

        try {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                paymentRepository.saveCompletedPayment(parsedAuctionId, parsedBidderId, auction.getSellerId(), amount, now);
                auctionRepository.updateStatus(parsedAuctionId, AuctionStatus.PAID);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw new PaymentException("Payment could not be completed.");
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        } catch (SQLException e) {
            throw new PaymentException("Payment could not be completed.");
        }

        return new ProcessPaymentResponse(parsedAuctionId.toString(), amount, "COMPLETED", now);
    }

    private void validatePaymentEligibility(Auction auction, UUID bidderId)
            throws InvalidAuctionStateException, UnauthorizedActionException, ValidationException, PaymentException {
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
        if (paymentRepository.existsCompletedPaymentForAuction(auction.getId())) {
            throw new PaymentException("Payment has already been completed for this auction.");
        }
    }

    private UUID parseUuid(String rawId, String fieldName) throws ValidationException {
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
