package com.nhom1.auction.server.bidding;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nhom1.auction.common.dto.bidding.AuctionDetailDto;
import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.entity.BidTransaction;
import com.nhom1.auction.common.entity.Item;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.enums.BidType;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.exception.ConflictException;
import com.nhom1.auction.common.exception.NotFoundException;
import com.nhom1.auction.server.auction.AuctionRepository;
import com.nhom1.auction.server.auction.ItemImageRepository;
import com.nhom1.auction.server.auction.ItemRepository;
import com.nhom1.auction.server.auth.UserRepository;
import com.nhom1.auction.server.wallet.WalletService;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BidServiceTest {

  @Mock private BidRepository bidRepository;

  @Mock private AuctionRepository auctionRepository;

  @Mock private ItemRepository itemRepository;

  @Mock private UserRepository userRepository;

  @Mock private WalletService walletService;

  @Mock private DataSource dataSource;

  @Mock private Connection connection;

  private BidService bidService;

  @Mock private ItemImageRepository itemImageRepository;

  @BeforeEach
  public void setUp() throws SQLException {
    MockitoAnnotations.openMocks(this);
    when(dataSource.getConnection()).thenReturn(connection);
    bidService =
        new BidService(
            bidRepository,
            auctionRepository,
            itemRepository,
            itemImageRepository,
            userRepository,
            walletService,
            dataSource);
  }

  @Test
  public void testPlaceBid_ValidBid_SavesAndUpdatesHighestBid() throws Exception {
    UUID bidderId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("150.00");
    LocalDateTime now = LocalDateTime.now();
    Auction auction =
        new Auction(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            now.minusHours(1),
            now.plusHours(1),
            null,
            null,
            AuctionStatus.RUNNING,
            now,
            now,
            null);
    UUID auctionId = auction.getId();
    when(connection.getAutoCommit()).thenReturn(true);
    when(auctionRepository.findById(auctionId, connection)).thenReturn(Optional.of(auction));
    when(auctionRepository.updateHighestBid(
            eq(auctionId), eq(amount), eq(bidderId), anyLong(), eq(connection)))
        .thenReturn(1);

    BidTransaction result = bidService.placeBid(bidderId, auctionId, amount, BidType.MANUAL);

    assertNotNull(result);
    assertEquals(amount, result.getAmount());
    assertEquals(bidderId, result.getBidderId());
    verify(bidRepository).save(any(BidTransaction.class), eq(connection));
    verify(auctionRepository)
        .updateHighestBid(eq(auctionId), eq(amount), eq(bidderId), anyLong(), eq(connection));
    verify(connection).setAutoCommit(false);
    verify(connection).commit();
    verify(connection).setAutoCommit(true);
  }

  @Test
  public void testPlaceBid_LostRace_RollsBackAndThrowsConflictException() throws Exception {
    UUID bidderId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("150.00");
    UUID auctionId = UUID.randomUUID();
    when(connection.getAutoCommit()).thenReturn(true);
    when(auctionRepository.findById(eq(auctionId), eq(connection)))
        .thenAnswer(
            inv -> {
              Auction a =
                  new Auction(
                      auctionId,
                      UUID.randomUUID(),
                      UUID.randomUUID(),
                      new BigDecimal("100.00"),
                      LocalDateTime.now().minusHours(1),
                      LocalDateTime.now().plusHours(1),
                      null,
                      null,
                      AuctionStatus.RUNNING,
                      LocalDateTime.now(),
                      LocalDateTime.now(),
                      7,
                      0);
              return Optional.of(a);
            });
    when(auctionRepository.updateHighestBid(
            eq(auctionId), eq(amount), eq(bidderId), anyLong(), eq(connection)))
        .thenReturn(0);

    ConflictException thrown =
        assertThrows(
            ConflictException.class,
            () -> bidService.placeBid(bidderId, auctionId, amount, BidType.MANUAL));

    assertTrue(thrown.getMessage().contains("Bid lost race"));
    verify(bidRepository, times(2)).save(any(BidTransaction.class), eq(connection));
    verify(connection, times(2)).rollback();
    verify(connection, times(2)).setAutoCommit(true);
    verify(connection, never()).commit();
  }

  @Test
  public void testPlaceBid_AuctionNotFound_ThrowsNotFoundException() throws SQLException {
    UUID bidderId = UUID.randomUUID();
    UUID auctionId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("150.00");
    when(connection.getAutoCommit()).thenReturn(true);
    when(auctionRepository.findById(auctionId, connection)).thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> bidService.placeBid(bidderId, auctionId, amount, BidType.MANUAL));
    verify(connection).rollback();
    verify(connection).setAutoCommit(true);
    verify(connection, never()).commit();
  }

  @Test
  public void testPlaceBid_UpdateHighestBidFails_RollsBackAndRestoresAutoCommit() throws Exception {
    UUID bidderId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("150.00");
    LocalDateTime now = LocalDateTime.now();
    Auction auction =
        new Auction(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            now.minusHours(1),
            now.plusHours(1),
            null,
            null,
            AuctionStatus.RUNNING,
            now,
            now,
            null);
    UUID auctionId = auction.getId();
    when(connection.getAutoCommit()).thenReturn(false);
    when(auctionRepository.findById(auctionId, connection)).thenReturn(Optional.of(auction));
    doThrow(new RuntimeException("update failed"))
        .when(auctionRepository)
        .updateHighestBid(eq(auctionId), eq(amount), eq(bidderId), anyLong(), eq(connection));

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class,
            () -> bidService.placeBid(bidderId, auctionId, amount, BidType.MANUAL));

    assertTrue(thrown.getMessage().startsWith("Bid placement failed"));
    verify(bidRepository).save(any(BidTransaction.class), eq(connection));
    verify(connection).rollback();
    verify(connection, times(2)).setAutoCommit(false);
    verify(connection, never()).setAutoCommit(true);
    verify(connection, never()).commit();
  }

  @Test
  public void testGetAuctionDetail_ValidAuction_ReturnsDtoWithBidHistory() throws Exception {
    Item item =
        new Item("Test Item", "Test Description", ItemCategory.ART, ItemCondition.NEW) {
          @Override
          public void printInfo() {}
        };
    UUID itemId = item.getId();
    UUID sellerId = UUID.randomUUID();
    LocalDateTime startTime = LocalDateTime.now().minusHours(1);
    LocalDateTime endTime = startTime.plusHours(2);
    Auction auction =
        new Auction(
            UUID.randomUUID(),
            itemId,
            sellerId,
            new BigDecimal("100.00"),
            startTime,
            endTime,
            null,
            null,
            AuctionStatus.RUNNING,
            LocalDateTime.now(),
            LocalDateTime.now(),
            null);
    UUID auctionId = auction.getId();
    UUID highestBidderId = UUID.randomUUID();
    BigDecimal currentHighestBid = new BigDecimal("150.00");
    auction.placeBid(highestBidderId, currentHighestBid, BidType.MANUAL, LocalDateTime.now());

    when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
    when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
    when(bidRepository.findByAuctionId(auctionId)).thenReturn(List.of());
    when(itemImageRepository.findImageUrlsByItemId(itemId)).thenReturn(List.of());

    AuctionDetailDto result = bidService.getAuctionDetail(auctionId);

    assertNotNull(result);
    assertEquals(auctionId.toString(), result.getAuctionId());
    assertEquals(itemId.toString(), result.getItemID());
    assertEquals("Test Item", result.getItemName());
    assertEquals(currentHighestBid, result.getCurrentHighestBid());
    assertEquals(highestBidderId.toString(), result.getCurrentHighestBidderId());
    assertEquals(new BigDecimal("5.00"), result.getMinBidIncrement());
  }

  @Test
  public void testGetAuctionDetail_AuctionNotFound_ThrowsNotFoundException() {
    UUID auctionId = UUID.randomUUID();
    when(auctionRepository.findById(auctionId)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> bidService.getAuctionDetail(auctionId));
  }

  @Test
  public void testGetAuctionDetail_ItemNotFound_ThrowsNotFoundException() {
    UUID auctionId = UUID.randomUUID();
    Auction auction =
        new Auction(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1));
    when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
    when(itemRepository.findById(auction.getItemId())).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> bidService.getAuctionDetail(auctionId));
  }

  @Test
  public void testGetAuctionDetail_ExceptionsInSubcalls_Handled() {
    Item item =
        new Item("Test Item", "Test Description", ItemCategory.ART, ItemCondition.NEW) {
          @Override
          public void printInfo() {}
        };
    UUID itemId = item.getId();
    Auction auction =
        new Auction(
            itemId,
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1));
    UUID auctionId = auction.getId();

    when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
    when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));

    // Throws exception during user lookup
    when(userRepository.findById(any())).thenThrow(new RuntimeException("DB offline"));
    // Throws exception during bid lookup
    when(bidRepository.findByAuctionId(any())).thenThrow(new RuntimeException("DB offline"));
    // Throws exception during image lookup
    when(itemImageRepository.findImageUrlsByItemId(any()))
        .thenThrow(new RuntimeException("S3 offline"));

    AuctionDetailDto dto = bidService.getAuctionDetail(auctionId);
    assertNotNull(dto);
    assertEquals("Unknown", dto.getSellerName());
    assertTrue(dto.getBidHistory().isEmpty());
    assertTrue(dto.getImageUrls().isEmpty());
  }

  @Test
  public void testListAllAuctions_AndToAuctionSummaryDtoNullItem() {
    Auction auction1 =
        new Auction(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            null,
            null,
            AuctionStatus.RUNNING,
            LocalDateTime.now(),
            LocalDateTime.now(),
            null);
    Auction auction2 =
        new Auction(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("200.00"),
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            null,
            null,
            AuctionStatus.RUNNING,
            LocalDateTime.now(),
            LocalDateTime.now(),
            null);

    Item item =
        new Item("Item 1", "Desc", ItemCategory.ELECTRONICS, ItemCondition.NEW) {
          @Override
          public void printInfo() {}
        };

    when(auctionRepository.findAll()).thenReturn(List.of(auction1, auction2));
    when(itemRepository.findById(auction1.getItemId())).thenReturn(Optional.of(item));
    when(itemRepository.findById(auction2.getItemId())).thenReturn(Optional.empty());

    var response = bidService.listAllAuctions();
    assertEquals(2, response.getAuctions().size());
    assertEquals("Item 1", response.getAuctions().get(0).getItemName());
    assertEquals("Unknown item", response.getAuctions().get(1).getItemName());
  }

  @Test
  public void testGetMyBids() {
    UUID bidderId = UUID.randomUUID();
    var bids = List.of(new com.nhom1.auction.common.dto.bidding.BidWithAuctionDto());
    when(bidRepository.findByBidderId(bidderId)).thenReturn(bids);

    var response = bidService.getMyBids(bidderId);
    assertEquals(bids, response.getBids());
  }

  @Test
  public void testToBidSummaryDto_UserLookupSuccessAndFailure() {
    Item item =
        new Item("Test Item", "Test Description", ItemCategory.ART, ItemCondition.NEW) {
          @Override
          public void printInfo() {}
        };
    UUID itemId = item.getId();
    Auction auction =
        new Auction(
            itemId,
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1));
    UUID auctionId = auction.getId();

    BidTransaction bid1 =
        new BidTransaction(
            UUID.randomUUID(),
            auctionId,
            UUID.randomUUID(),
            BigDecimal.TEN,
            BidType.MANUAL,
            LocalDateTime.now(),
            LocalDateTime.now());
    BidTransaction bid2 =
        new BidTransaction(
            UUID.randomUUID(),
            auctionId,
            UUID.randomUUID(),
            BigDecimal.valueOf(12),
            BidType.MANUAL,
            LocalDateTime.now(),
            LocalDateTime.now());

    when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
    when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
    when(bidRepository.findByAuctionId(auctionId)).thenReturn(List.of(bid1, bid2));

    com.nhom1.auction.common.entity.User user =
        new com.nhom1.auction.common.entity.User(
            "bidder1", "email", "pass", com.nhom1.auction.common.enums.UserRole.USER);
    when(userRepository.findById(bid1.getBidderId())).thenReturn(Optional.of(user));
    when(userRepository.findById(bid2.getBidderId())).thenReturn(Optional.empty());

    AuctionDetailDto dto = bidService.getAuctionDetail(auctionId);
    assertEquals(2, dto.getBidHistory().size());
    assertEquals("bidder1", dto.getBidHistory().get(0).getBidderName());
    assertEquals("Unknown", dto.getBidHistory().get(1).getBidderName());
  }
}
