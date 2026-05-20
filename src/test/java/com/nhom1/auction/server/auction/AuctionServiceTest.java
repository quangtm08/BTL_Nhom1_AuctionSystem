package com.nhom1.auction.server.auction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nhom1.auction.common.dto.auction.CreateAuctionRequest;
import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.entity.Item;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.exception.NotFoundException;
import com.nhom1.auction.common.exception.UnauthorizedActionException;
import com.nhom1.auction.common.exception.ValidationException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AuctionServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemImageRepository itemImageRepository;

    @Mock
    private Connection connection;

    private AuctionService auctionService;

    @BeforeEach
    public void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        auctionService = new AuctionService(auctionRepository, itemRepository, itemImageRepository, connection);
    }

    @Test
    public void testCreateAuction_ValidRequest_SavesItemAndAuctionReturnsAuction()
        throws Exception {
        String sellerId = UUID.randomUUID().toString();
        CreateAuctionRequest dto = createValidCreateAuctionRequest();
        when(connection.getAutoCommit()).thenReturn(true);

        Auction result = auctionService.createAuction(sellerId, dto);

        assertNotNull(result);
        assertNotNull(result.getItemId());
        assertEquals(UUID.fromString(sellerId), result.getSellerId());
        verify(itemRepository).save(
            any(Item.class),
            eq(UUID.fromString(sellerId)),
            eq(connection)
        );
        verify(auctionRepository).save(any(Auction.class), eq(connection));
        verify(auctionRepository).updateHighestBid(
            any(UUID.class),
            eq(dto.getStartingPrice()),
            isNull(),
            eq(connection)
        );
        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection).setAutoCommit(true);
    }

    @Test
    public void testCreateAuction_AuctionSaveFails_RollsBackAndRestoresAutoCommit()
        throws SQLException {
        String sellerId = UUID.randomUUID().toString();
        CreateAuctionRequest dto = createValidCreateAuctionRequest();
        when(connection.getAutoCommit()).thenReturn(true);
        doThrow(new RuntimeException("save auction failed"))
            .when(auctionRepository)
            .save(any(Auction.class), eq(connection));

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            auctionService.createAuction(sellerId, dto)
        );

        assertEquals("Create auction transaction failed", thrown.getMessage());
        verify(itemRepository).save(
            any(Item.class),
            eq(UUID.fromString(sellerId)),
            eq(connection)
        );
        verify(auctionRepository).save(any(Auction.class), eq(connection));
        verify(auctionRepository, never()).updateHighestBid(
            any(UUID.class),
            any(BigDecimal.class),
            any(),
            eq(connection)
        );
        verify(connection).rollback();
        verify(connection).setAutoCommit(true);
        verify(connection, never()).commit();
    }

    @Test
    public void testCreateAuction_NullSellerId_Throws() {
        CreateAuctionRequest dto = createValidCreateAuctionRequest();

        assertThrows(ValidationException.class, () ->
            auctionService.createAuction(null, dto)
        );
    }

    @Test
    public void testCreateAuction_InvalidSellerId_Throws() {
        CreateAuctionRequest dto = createValidCreateAuctionRequest();

        assertThrows(ValidationException.class, () ->
            auctionService.createAuction("invalid", dto)
        );
    }

    @Test
    public void testCreateAuction_StartingPriceZero_Throws() {
        String sellerId = UUID.randomUUID().toString();
        CreateAuctionRequest dto = createValidCreateAuctionRequest();
        dto.setStartingPrice(BigDecimal.ZERO);

        assertThrows(ValidationException.class, () ->
            auctionService.createAuction(sellerId, dto)
        );
    }

    @Test
    public void testCreateAuction_StartingPriceNegative_Throws() {
        String sellerId = UUID.randomUUID().toString();
        CreateAuctionRequest dto = createValidCreateAuctionRequest();
        dto.setStartingPrice(new BigDecimal("-10.00"));

        assertThrows(ValidationException.class, () ->
            auctionService.createAuction(sellerId, dto)
        );
    }

    @Test
    public void testCreateAuction_EndTimeBeforeStartTime_Throws() {
        String sellerId = UUID.randomUUID().toString();
        CreateAuctionRequest dto = createValidCreateAuctionRequest();
        dto.setEndTime(dto.getStartTime().minusHours(1));

        assertThrows(ValidationException.class, () ->
            auctionService.createAuction(sellerId, dto)
        );
    }

    @Test
    public void testCreateAuction_SellerIdMismatch_Throws() {
        String sellerId = UUID.randomUUID().toString();
        CreateAuctionRequest dto = createValidCreateAuctionRequest();
        dto.setSellerId(UUID.randomUUID().toString());

        assertThrows(ValidationException.class, () ->
            auctionService.createAuction(sellerId, dto)
        );
    }

    @Test
    public void testDeleteAuction_OwnerDeletesOwn_DeletesBoth()
        throws SQLException {
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        UUID parsedSellerId = UUID.fromString(sellerId);
        UUID parsedAuctionId = UUID.fromString(auctionId);
        Auction auction = mock(Auction.class);
        when(auction.getSellerId()).thenReturn(parsedSellerId);
        when(auction.getItemId()).thenReturn(UUID.randomUUID());
        when(auctionRepository.findById(parsedAuctionId)).thenReturn(
            Optional.of(auction)
        );
        when(connection.getAutoCommit()).thenReturn(true);
        when(auctionRepository.deleteById(parsedAuctionId, connection)).thenReturn(1);
        when(itemRepository.deleteById(any(UUID.class), eq(connection))).thenReturn(1);

        assertDoesNotThrow(() ->
            auctionService.deleteAuction(sellerId, auctionId)
        );

        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection).setAutoCommit(true);
        verify(auctionRepository).deleteById(parsedAuctionId, connection);
        verify(itemRepository).deleteById(any(UUID.class), eq(connection));
    }

    @Test
    public void testDeleteAuction_ItemDeleteFails_RollsBackAndRestoresAutoCommit()
        throws Exception {
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        UUID parsedSellerId = UUID.fromString(sellerId);
        UUID parsedAuctionId = UUID.fromString(auctionId);
        Auction auction = mock(Auction.class);
        when(auction.getSellerId()).thenReturn(parsedSellerId);
        when(auction.getItemId()).thenReturn(UUID.randomUUID());
        when(auctionRepository.findById(parsedAuctionId)).thenReturn(
            Optional.of(auction)
        );
        when(connection.getAutoCommit()).thenReturn(true);
        when(auctionRepository.deleteById(parsedAuctionId, connection)).thenReturn(1);
        when(itemRepository.deleteById(any(UUID.class), eq(connection))).thenReturn(0);

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            auctionService.deleteAuction(sellerId, auctionId)
        );
        assertEquals("Delete transaction failed", thrown.getMessage());
        assertTrue(thrown.getCause() instanceof IllegalStateException);

        verify(connection).rollback();
        verify(connection).setAutoCommit(true);
        verify(connection, never()).commit();
    }

    @Test
    public void testDeleteAuction_NonOwner_Throws() {
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        UUID parsedAuctionId = UUID.fromString(auctionId);
        Auction auction = mock(Auction.class);
        when(auction.getSellerId()).thenReturn(UUID.randomUUID()); // different seller
        when(auctionRepository.findById(parsedAuctionId)).thenReturn(
            Optional.of(auction)
        );

        assertThrows(UnauthorizedActionException.class, () ->
            auctionService.deleteAuction(sellerId, auctionId)
        );
    }

    @Test
    public void testDeleteAuction_AuctionNotFound_Throws() {
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        UUID parsedAuctionId = UUID.fromString(auctionId);
        when(auctionRepository.findById(parsedAuctionId)).thenReturn(
            Optional.empty()
        );

        assertThrows(NotFoundException.class, () ->
            auctionService.deleteAuction(sellerId, auctionId)
        );
    }

    private CreateAuctionRequest createValidCreateAuctionRequest() {
        CreateAuctionRequest dto = new CreateAuctionRequest();
        dto.setName("Test Item");
        dto.setDescription("Test Description");
        dto.setCategory(ItemCategory.ART);
        dto.setCondition(ItemCondition.NEW);
        dto.setStartingPrice(new BigDecimal("100.00"));
        dto.setStartTime(LocalDateTime.now());
        dto.setEndTime(LocalDateTime.now().plusHours(1));
        return dto;
    }
}
