package com.nhom1.auction.server.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nhom1.auction.common.dto.payment.PaymentItemDto;
import com.nhom1.auction.common.dto.payment.PaymentListResponse;
import com.nhom1.auction.common.dto.payment.ProcessPaymentResponse;
import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.entity.User;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.enums.UserRole;
import com.nhom1.auction.common.exception.UnauthorizedActionException;
import com.nhom1.auction.common.exception.ValidationException;
import com.nhom1.auction.server.auction.AuctionRepository;
import com.nhom1.auction.server.auth.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuctionRepository auctionRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        paymentService = new PaymentService(paymentRepository, userRepository, auctionRepository);
    }

    @Test
    void processPayment_WinningBidderOnFinishedAuction_MarksAuctionPaid() throws Exception {
        User bidder = new User("winner", "winner@example.com", "password", UserRole.USER);
        Auction auction = createFinishedAuctionFor(bidder.getId());

        when(userRepository.findById(bidder.getId())).thenReturn(Optional.of(bidder));
        when(auctionRepository.findById(auction.getId())).thenReturn(Optional.of(auction));

        ProcessPaymentResponse response = paymentService.processPayment(
                auction.getId().toString(),
                bidder.getId().toString());

        assertNotNull(response);
        assertEquals(auction.getId().toString(), response.getAuctionId());
        assertEquals(AuctionStatus.PAID, response.getNewStatus());
        verify(auctionRepository).updateStatus(auction.getId(), AuctionStatus.PAID);
    }

    @Test
    void processPayment_NonWinner_ThrowsUnauthorized() throws Exception {
        User winner = new User("winner", "winner@example.com", "password", UserRole.USER);
        User otherUser = new User("other", "other@example.com", "password", UserRole.USER);
        Auction auction = createFinishedAuctionFor(winner.getId());

        when(userRepository.findById(otherUser.getId())).thenReturn(Optional.of(otherUser));
        when(auctionRepository.findById(auction.getId())).thenReturn(Optional.of(auction));

        assertThrows(UnauthorizedActionException.class, () ->
                paymentService.processPayment(auction.getId().toString(), otherUser.getId().toString()));
    }

    @Test
    void processPayment_UnfinishedAuction_ThrowsValidation() throws Exception {
        User bidder = new User("winner", "winner@example.com", "password", UserRole.USER);
        Auction auction = new Auction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                bidder.getId(),
                new BigDecimal("100.00"),
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().plusHours(2),
                bidder.getId(),
                new BigDecimal("125.00"),
                AuctionStatus.RUNNING,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now());

        when(userRepository.findById(bidder.getId())).thenReturn(Optional.of(bidder));
        when(auctionRepository.findById(auction.getId())).thenReturn(Optional.of(auction));

        assertThrows(ValidationException.class, () ->
                paymentService.processPayment(auction.getId().toString(), bidder.getId().toString()));
    }

    @Test
    void processPayment_AlreadyPaid_ThrowsValidation() throws Exception {
        User bidder = new User("winner", "winner@example.com", "password", UserRole.USER);
        Auction auction = new Auction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                bidder.getId(),
                new BigDecimal("100.00"),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusHours(1),
                bidder.getId(),
                new BigDecimal("125.00"),
                AuctionStatus.PAID,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now());

        when(userRepository.findById(bidder.getId())).thenReturn(Optional.of(bidder));
        when(auctionRepository.findById(auction.getId())).thenReturn(Optional.of(auction));

        assertThrows(ValidationException.class, () ->
                paymentService.processPayment(auction.getId().toString(), bidder.getId().toString()));
    }

    @Test
    void getPendingPayments_ReturnsRepositoryResults() throws Exception {
        User bidder = new User("winner", "winner@example.com", "password", UserRole.USER);
        PaymentItemDto item = new PaymentItemDto(
                UUID.randomUUID().toString(),
                "Vintage Camera",
                "ELECTRONICS",
                new BigDecimal("400.00"),
                LocalDateTime.now(),
                "Awaiting payment");

        when(userRepository.findById(bidder.getId())).thenReturn(Optional.of(bidder));
        when(paymentRepository.findPendingPayments(bidder.getId())).thenReturn(List.of(item));

        PaymentListResponse response = paymentService.getPendingPayments(bidder.getId().toString());

        assertEquals(1, response.getPayments().size());
        assertEquals("Vintage Camera", response.getPayments().get(0).getItemName());
    }

    @Test
    void getPaymentHistory_ReturnsRepositoryResults() throws Exception {
        User bidder = new User("winner", "winner@example.com", "password", UserRole.USER);
        PaymentItemDto item = new PaymentItemDto(
                UUID.randomUUID().toString(),
                "Antique Watch",
                "ART",
                new BigDecimal("900.00"),
                LocalDateTime.now(),
                "Paid");

        when(userRepository.findById(bidder.getId())).thenReturn(Optional.of(bidder));
        when(paymentRepository.findPaymentHistory(bidder.getId())).thenReturn(List.of(item));

        PaymentListResponse response = paymentService.getPaymentHistory(bidder.getId().toString());

        assertEquals(1, response.getPayments().size());
        assertEquals("Paid", response.getPayments().get(0).getStatusLabel());
    }

    private Auction createFinishedAuctionFor(UUID bidderId) {
        return new Auction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusHours(1),
                bidderId,
                new BigDecimal("125.00"),
                AuctionStatus.FINISHED,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now());
    }
}
