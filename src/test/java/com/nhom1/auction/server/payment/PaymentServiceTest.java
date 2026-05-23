package com.nhom1.auction.server.payment;

import com.nhom1.auction.common.dto.payment.PendingPaymentsResponse;
import com.nhom1.auction.common.dto.payment.ProcessPaymentResponse;
import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.exception.InvalidAuctionStateException;
import com.nhom1.auction.common.exception.PaymentException;
import com.nhom1.auction.common.exception.UnauthorizedActionException;
import com.nhom1.auction.common.exception.ValidationException;
import com.nhom1.auction.server.auction.AuctionRepository;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private AuctionRepository auctionRepository;
    @Mock private DataSource dataSource;
    @Mock private Connection connection;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        when(dataSource.getConnection()).thenReturn(connection);
        paymentService = new PaymentService(paymentRepository, auctionRepository, dataSource);
    }

    @Test
    void processPayment_WinningBidderOnFinishedAuction_CompletesPayment() {
        Auction auction = finishedAuction();
        when(auctionRepository.findById(auction.getId())).thenReturn(Optional.of(auction));
        try {
            when(connection.getAutoCommit()).thenReturn(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        ProcessPaymentResponse response = paymentService.processPayment(
                auction.getId().toString(),
                auction.getHighestBidderId().toString());

        assertEquals("COMPLETED", response.getStatus());
        assertEquals(auction.getId().toString(), response.getAuctionId());
        verify(paymentRepository).saveCompletedPayment(any(), any(), any(), any(), any(), eq(connection));
        try {
            verify(auctionRepository).updateStatus(auction.getId(), AuctionStatus.PAID, connection);
            verify(connection).commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void processPayment_NonWinner_ThrowsUnauthorized() {
        Auction auction = finishedAuction();
        when(auctionRepository.findById(auction.getId())).thenReturn(Optional.of(auction));

        assertThrows(UnauthorizedActionException.class,
                () -> paymentService.processPayment(auction.getId().toString(), UUID.randomUUID().toString()));
    }

    @Test
    void processPayment_AuctionNotFinished_ThrowsInvalidState() {
        Auction auction = new Auction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                UUID.randomUUID(),
                new BigDecimal("150.00"),
                AuctionStatus.RUNNING,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusHours(1));
        when(auctionRepository.findById(auction.getId())).thenReturn(Optional.of(auction));

        assertThrows(InvalidAuctionStateException.class,
                () -> paymentService.processPayment(auction.getId().toString(), auction.getHighestBidderId().toString()));
    }

    @Test
    void processPayment_AuctionWithoutWinner_ThrowsValidation() {
        Auction auction = new Auction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                null,
                null,
                AuctionStatus.FINISHED,
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusDays(1));
        when(auctionRepository.findById(auction.getId())).thenReturn(Optional.of(auction));

        assertThrows(ValidationException.class,
                () -> paymentService.processPayment(auction.getId().toString(), UUID.randomUUID().toString()));
    }

    @Test
    void processPayment_RollsBackWhenPersistenceFails() throws Exception {
        Auction auction = finishedAuction();
        when(auctionRepository.findById(auction.getId())).thenReturn(Optional.of(auction));
        when(connection.getAutoCommit()).thenReturn(true);
        doThrow(new RuntimeException("db fail")).when(paymentRepository)
                .saveCompletedPayment(any(), any(), any(), any(), any(), eq(connection));

        assertThrows(RuntimeException.class,
                () -> paymentService.processPayment(auction.getId().toString(), auction.getHighestBidderId().toString()));
        verify(connection).rollback();
    }

    @Test
    void listPendingPayments_DelegatesToRepository() {
        UUID bidderId = UUID.randomUUID();
        when(paymentRepository.findPendingPaymentsByBidder(bidderId)).thenReturn(List.of());

        PendingPaymentsResponse response = paymentService.listPendingPayments(bidderId.toString());

        assertFalse(response.getPayments().iterator().hasNext());
    }

    @Test
    void processPayment_AlreadyPaid_ThrowsInvalidState() {
        Auction auction = new Auction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                UUID.randomUUID(),
                new BigDecimal("180.00"),
                AuctionStatus.PAID,
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusHours(2));
        when(auctionRepository.findById(auction.getId())).thenReturn(Optional.of(auction));

        assertThrows(InvalidAuctionStateException.class,
                () -> paymentService.processPayment(auction.getId().toString(), auction.getHighestBidderId().toString()));
    }

    private Auction finishedAuction() {
        return new Auction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                UUID.randomUUID(),
                new BigDecimal("150.00"),
                AuctionStatus.FINISHED,
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusHours(2));
    }

    @Test
    void listPaymentHistory_DelegatesToRepository() {
        UUID userId = UUID.randomUUID();
        when(paymentRepository.findPaymentHistoryForUser(userId)).thenReturn(List.of());

        var response = paymentService.listPaymentHistory(userId.toString());
        assertFalse(response.getEntries().iterator().hasNext());
    }

    @Test
    void listPaymentHistory_InvalidUserId_Throws() {
        assertThrows(ValidationException.class, () -> paymentService.listPaymentHistory("invalid-uuid"));
        assertThrows(ValidationException.class, () -> paymentService.listPaymentHistory(""));
        assertThrows(ValidationException.class, () -> paymentService.listPaymentHistory(null));
    }

    @Test
    void listPendingPayments_InvalidBidderId_Throws() {
        assertThrows(ValidationException.class, () -> paymentService.listPendingPayments("invalid-uuid"));
    }

    @Test
    void processPayment_AuctionNotFound_ThrowsNotFound() {
        UUID auctionId = UUID.randomUUID();
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.empty());

        assertThrows(com.nhom1.auction.common.exception.NotFoundException.class,
                () -> paymentService.processPayment(auctionId.toString(), UUID.randomUUID().toString()));
    }

    @Test
    void processPayment_AlreadyPaidInRepository_ThrowsPaymentException() {
        Auction auction = finishedAuction();
        when(auctionRepository.findById(auction.getId())).thenReturn(Optional.of(auction));
        when(paymentRepository.existsCompletedPaymentForAuction(eq(auction.getId()), any())).thenReturn(true);

        assertThrows(PaymentException.class,
                () -> paymentService.processPayment(auction.getId().toString(), auction.getHighestBidderId().toString()));
    }

    @Test
    void processPayment_CanceledAuction_ThrowsInvalidState() {
        Auction auction = new Auction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                UUID.randomUUID(),
                new BigDecimal("150.00"),
                AuctionStatus.CANCELED,
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusHours(2));
        when(auctionRepository.findById(auction.getId())).thenReturn(Optional.of(auction));

        assertThrows(InvalidAuctionStateException.class,
                () -> paymentService.processPayment(auction.getId().toString(), auction.getHighestBidderId().toString()));
    }

    @Test
    void processPayment_ConnectionThrowsSQLException_ThrowsRuntimeException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Simulated connection failure"));
        Auction auction = finishedAuction();

        assertThrows(RuntimeException.class,
                () -> paymentService.processPayment(auction.getId().toString(), auction.getHighestBidderId().toString()));
    }

    @Test
    void processPayment_AppExceptionDuringSave_RollsBackAndRethrows() throws Exception {
        Auction auction = finishedAuction();
        when(auctionRepository.findById(auction.getId())).thenReturn(Optional.of(auction));
        when(connection.getAutoCommit()).thenReturn(true);

        PaymentException appEx = new PaymentException("App failure");

        doThrow(appEx).when(paymentRepository)
                .saveCompletedPayment(any(), any(), any(), any(), any(), eq(connection));

        assertThrows(PaymentException.class,
                () -> paymentService.processPayment(auction.getId().toString(), auction.getHighestBidderId().toString()));
        verify(connection).rollback();
        verify(connection).setAutoCommit(true);
    }
}
