package com.nhom1.auction.server.payment;

import com.nhom1.auction.common.dto.payment.PaymentListResponse;
import com.nhom1.auction.common.dto.payment.ProcessPaymentResponse;
import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.entity.User;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.exception.AuthenticationException;
import com.nhom1.auction.common.exception.InvalidAuctionStateException;
import com.nhom1.auction.common.exception.UnauthorizedActionException;
import com.nhom1.auction.common.exception.ValidationException;
import com.nhom1.auction.server.auction.AuctionRepository;
import com.nhom1.auction.server.auth.UserRepository;
import java.util.UUID;

public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            AuctionRepository auctionRepository) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.auctionRepository = auctionRepository;
    }

    public PaymentListResponse getPendingPayments(String bidderId)
            throws ValidationException, AuthenticationException {
        User bidder = requireUser(bidderId);
        return new PaymentListResponse(paymentRepository.findPendingPayments(bidder.getId()));
    }

    public PaymentListResponse getPaymentHistory(String bidderId)
            throws ValidationException, AuthenticationException {
        User bidder = requireUser(bidderId);
        return new PaymentListResponse(paymentRepository.findPaymentHistory(bidder.getId()));
    }

    public ProcessPaymentResponse processPayment(String auctionId, String bidderId)
            throws ValidationException, AuthenticationException, UnauthorizedActionException {
        User bidder = requireUser(bidderId);
        UUID parsedAuctionId = parseUuid(auctionId, "Auction ID");

        Auction auction = auctionRepository.findById(parsedAuctionId)
                .orElseThrow(() -> new ValidationException("Auction not found."));

        if (auction.getHighestBidderId() == null || !auction.getHighestBidderId().equals(bidder.getId())) {
            throw new UnauthorizedActionException("Only the winning bidder can pay for this auction.");
        }

        if (auction.getStatus() == AuctionStatus.PAID) {
            throw new ValidationException("This auction has already been paid.");
        }

        if (auction.getStatus() != AuctionStatus.FINISHED) {
            throw new ValidationException("Only finished auctions can be paid.");
        }

        try {
            auction.markAsPaid();
        } catch (InvalidAuctionStateException e) {
            throw new ValidationException(e.getMessage());
        }

        auctionRepository.updateStatus(parsedAuctionId, AuctionStatus.PAID);
        return new ProcessPaymentResponse(
                parsedAuctionId.toString(),
                AuctionStatus.PAID,
                "Payment recorded successfully.");
    }

    private User requireUser(String bidderId) throws ValidationException, AuthenticationException {
        UUID parsedBidderId = parseUuid(bidderId, "Bidder ID");
        return userRepository.findById(parsedBidderId)
                .orElseThrow(() -> new AuthenticationException("User not found."));
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
