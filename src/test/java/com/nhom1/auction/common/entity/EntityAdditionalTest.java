package com.nhom1.auction.common.entity;

import static org.junit.jupiter.api.Assertions.*;

import com.nhom1.auction.common.enums.BidType;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.enums.UserRole;
import com.nhom1.auction.common.exception.AuctionClosedException;
import com.nhom1.auction.common.exception.InvalidBidException;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class EntityAdditionalTest {

  @Test
  public void testUserGettersSetters() {
    User u = new User();
    u.setUsername("testuser");
    u.setEmail("test@email.com");
    u.setPassword("pass123");
    u.setRole(UserRole.ADMIN);

    assertEquals("testuser", u.getUsername());
    assertEquals("test@email.com", u.getEmail());
    assertEquals("pass123", u.getPassword());
    assertEquals(UserRole.ADMIN, u.getRole());

    // Test constructor
    User u2 = new User("user2", "u2@mail.com", "p", UserRole.USER);
    assertEquals("user2", u2.getUsername());
    assertEquals("u2@mail.com", u2.getEmail());
    assertEquals("p", u2.getPassword());
    assertEquals(UserRole.USER, u2.getRole());
  }

  @Test
  public void testItemSubclassesAndGettersSetters() {
    Art art = new Art("ArtPiece", "Beautiful painting", ItemCategory.ART, ItemCondition.NEW);
    assertEquals("ArtPiece", art.getName());
    assertEquals("Beautiful painting", art.getDescription());
    assertEquals(ItemCategory.ART, art.getCategory());
    assertEquals(ItemCondition.NEW, art.getCondition());

    art.setName("NewArt");
    art.setDescription("NewDesc");
    art.setCategory(ItemCategory.ELECTRONICS);
    art.setCondition(ItemCondition.USED);

    assertEquals("NewArt", art.getName());
    assertEquals("NewDesc", art.getDescription());
    assertEquals(ItemCategory.ELECTRONICS, art.getCategory());
    assertEquals(ItemCondition.USED, art.getCondition());

    // PrintInfo
    art.printInfo();

    // Electronics
    Electronics el =
        new Electronics("Phone", "Smart phone", ItemCategory.ELECTRONICS, ItemCondition.NEW);
    el.printInfo();

    // Vehicle
    Vehicle v = new Vehicle("Car", "Fast car", ItemCategory.VEHICLE, ItemCondition.NEW);
    v.printInfo();

    // Subclass constructors with UUID/dates
    UUID id = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();
    Art art2 = new Art(id, "Art2", "Desc2", ItemCategory.ART, ItemCondition.NEW, now, now);
    assertEquals(id, art2.getId());

    Electronics el2 =
        new Electronics(id, "El2", "Desc2", ItemCategory.ELECTRONICS, ItemCondition.NEW, now, now);
    assertEquals(id, el2.getId());

    Vehicle v2 = new Vehicle(id, "V2", "Desc2", ItemCategory.VEHICLE, ItemCondition.NEW, now, now);
    assertEquals(id, v2.getId());
  }

  @Test
  public void testAuctionBidValidatorPrivateConstructor() throws Exception {
    Constructor<AuctionBidValidator> constructor =
        AuctionBidValidator.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    AuctionBidValidator instance = constructor.newInstance();
    assertNotNull(instance);
  }

  @Test
  public void testAuctionBidValidatorEdgeCases() {
    Auction auction =
        new Auction(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(1));
    auction.startAuction();

    // 1. null bidderId
    assertThrows(
        InvalidBidException.class,
        () ->
            AuctionBidValidator.validatePlaceBid(
                auction, null, BigDecimal.TEN, BidType.MANUAL, LocalDateTime.now()));

    // 2. null amount
    assertThrows(
        InvalidBidException.class,
        () ->
            AuctionBidValidator.validatePlaceBid(
                auction, UUID.randomUUID(), null, BidType.MANUAL, LocalDateTime.now()));

    // 3. null bidType
    assertThrows(
        InvalidBidException.class,
        () ->
            AuctionBidValidator.validatePlaceBid(
                auction, UUID.randomUUID(), BigDecimal.TEN, null, LocalDateTime.now()));

    // 4. null bidTime
    assertThrows(
        InvalidBidException.class,
        () ->
            AuctionBidValidator.validatePlaceBid(
                auction, UUID.randomUUID(), BigDecimal.TEN, BidType.MANUAL, null));

    // 5. amount <= 0
    assertThrows(
        InvalidBidException.class,
        () ->
            AuctionBidValidator.validatePlaceBid(
                auction, UUID.randomUUID(), BigDecimal.ZERO, BidType.MANUAL, LocalDateTime.now()));

    // 6. bidTime after end time
    assertThrows(
        AuctionClosedException.class,
        () ->
            AuctionBidValidator.validatePlaceBid(
                auction,
                UUID.randomUUID(),
                new BigDecimal("150.00"),
                BidType.MANUAL,
                LocalDateTime.now().plusHours(2)));

    // 7. first bid less than starting price
    assertThrows(
        InvalidBidException.class,
        () ->
            AuctionBidValidator.validatePlaceBid(
                auction,
                UUID.randomUUID(),
                new BigDecimal("50.00"),
                BidType.MANUAL,
                LocalDateTime.now()));
  }

  @Test
  public void testWalletAndWalletTransactionEntities() {
    // Test Wallet default constructor
    Wallet w1 = new Wallet();
    assertNotNull(w1.getId());
    assertEquals(BigDecimal.ZERO, w1.getBalance());

    // Test Wallet constructor with UUID and balance
    UUID userId = UUID.randomUUID();
    BigDecimal balance = new BigDecimal("500.00");
    Wallet w2 = new Wallet(userId, balance);
    assertEquals(userId, w2.getUserId());
    assertEquals(balance, w2.getBalance());

    // Test Wallet constructor with all parameters
    LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
    LocalDateTime updatedAt = LocalDateTime.now();
    Wallet w3 = new Wallet(userId, balance, createdAt, updatedAt);
    assertEquals(userId, w3.getUserId());
    assertEquals(balance, w3.getBalance());
    assertEquals(createdAt, w3.getCreatedAt());
    assertEquals(updatedAt, w3.getUpdatedAt());

    // Test Wallet setters
    BigDecimal newBalance = new BigDecimal("1000.00");
    w3.setBalance(newBalance);
    assertEquals(newBalance, w3.getBalance());

    // Test WalletTransaction default constructor
    WalletTransaction tx1 = new WalletTransaction();
    assertNull(tx1.getId());

    // Test WalletTransaction secondary constructor
    String type = "DEPOSIT";
    String refId = "ref-123";
    String desc = "test tx";
    WalletTransaction tx2 = new WalletTransaction(userId, balance, type, refId, desc);
    assertNotNull(tx2.getId());
    assertEquals(userId, tx2.getUserId());
    assertEquals(balance, tx2.getAmount());
    assertEquals(type, tx2.getTransactionType());
    assertEquals(refId, tx2.getReferenceId());
    assertEquals(desc, tx2.getDescription());
    assertNotNull(tx2.getCreatedAt());

    // Test WalletTransaction all parameters constructor
    UUID txId = UUID.randomUUID();
    LocalDateTime txCreatedAt = LocalDateTime.now().minusHours(2);
    WalletTransaction tx3 =
        new WalletTransaction(txId, userId, balance, type, refId, desc, txCreatedAt);
    assertEquals(txId, tx3.getId());
    assertEquals(userId, tx3.getUserId());
    assertEquals(balance, tx3.getAmount());
    assertEquals(type, tx3.getTransactionType());
    assertEquals(refId, tx3.getReferenceId());
    assertEquals(desc, tx3.getDescription());
    assertEquals(txCreatedAt, tx3.getCreatedAt());

    // Test WalletTransaction setters
    UUID newTxId = UUID.randomUUID();
    UUID newUserId = UUID.randomUUID();
    BigDecimal newAmount = new BigDecimal("20.00");
    String newType = "WITHDRAW";
    String newRef = "ref-456";
    String newDesc = "new desc";
    LocalDateTime newCreatedAt = LocalDateTime.now();

    tx3.setId(newTxId);
    tx3.setUserId(newUserId);
    tx3.setAmount(newAmount);
    tx3.setTransactionType(newType);
    tx3.setReferenceId(newRef);
    tx3.setDescription(newDesc);
    tx3.setCreatedAt(newCreatedAt);

    assertEquals(newTxId, tx3.getId());
    assertEquals(newUserId, tx3.getUserId());
    assertEquals(newAmount, tx3.getAmount());
    assertEquals(newType, tx3.getTransactionType());
    assertEquals(newRef, tx3.getReferenceId());
    assertEquals(newDesc, tx3.getDescription());
    assertEquals(newCreatedAt, tx3.getCreatedAt());
  }
}
