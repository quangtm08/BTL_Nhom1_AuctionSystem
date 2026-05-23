package com.nhom1.auction.common.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nhom1.auction.common.enums.BidType;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.enums.UserRole;
import com.nhom1.auction.common.exception.AuctionClosedException;
import com.nhom1.auction.common.exception.InvalidBidException;

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
        Electronics el = new Electronics("Phone", "Smart phone", ItemCategory.ELECTRONICS, ItemCondition.NEW);
        el.printInfo();

        // Vehicle
        Vehicle v = new Vehicle("Car", "Fast car", ItemCategory.VEHICLE, ItemCondition.NEW);
        v.printInfo();

        // Subclass constructors with UUID/dates
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Art art2 = new Art(id, "Art2", "Desc2", ItemCategory.ART, ItemCondition.NEW, now, now);
        assertEquals(id, art2.getId());

        Electronics el2 = new Electronics(id, "El2", "Desc2", ItemCategory.ELECTRONICS, ItemCondition.NEW, now, now);
        assertEquals(id, el2.getId());

        Vehicle v2 = new Vehicle(id, "V2", "Desc2", ItemCategory.VEHICLE, ItemCondition.NEW, now, now);
        assertEquals(id, v2.getId());
    }

    @Test
    public void testAuctionBidValidatorPrivateConstructor() throws Exception {
        Constructor<AuctionBidValidator> constructor = AuctionBidValidator.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        AuctionBidValidator instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Test
    public void testAuctionBidValidatorEdgeCases() {
        Auction auction = new Auction(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(1)
        );
        auction.startAuction();

        // 1. null bidderId
        assertThrows(InvalidBidException.class, () -> 
            AuctionBidValidator.validatePlaceBid(auction, null, BigDecimal.TEN, BidType.MANUAL, LocalDateTime.now())
        );

        // 2. null amount
        assertThrows(InvalidBidException.class, () -> 
            AuctionBidValidator.validatePlaceBid(auction, UUID.randomUUID(), null, BidType.MANUAL, LocalDateTime.now())
        );

        // 3. null bidType
        assertThrows(InvalidBidException.class, () -> 
            AuctionBidValidator.validatePlaceBid(auction, UUID.randomUUID(), BigDecimal.TEN, null, LocalDateTime.now())
        );

        // 4. null bidTime
        assertThrows(InvalidBidException.class, () -> 
            AuctionBidValidator.validatePlaceBid(auction, UUID.randomUUID(), BigDecimal.TEN, BidType.MANUAL, null)
        );

        // 5. amount <= 0
        assertThrows(InvalidBidException.class, () -> 
            AuctionBidValidator.validatePlaceBid(auction, UUID.randomUUID(), BigDecimal.ZERO, BidType.MANUAL, LocalDateTime.now())
        );

        // 6. bidTime after end time
        assertThrows(AuctionClosedException.class, () -> 
            AuctionBidValidator.validatePlaceBid(auction, UUID.randomUUID(), new BigDecimal("150.00"), BidType.MANUAL, LocalDateTime.now().plusHours(2))
        );

        // 7. first bid less than starting price
        assertThrows(InvalidBidException.class, () -> 
            AuctionBidValidator.validatePlaceBid(auction, UUID.randomUUID(), new BigDecimal("50.00"), BidType.MANUAL, LocalDateTime.now())
        );
    }
}
