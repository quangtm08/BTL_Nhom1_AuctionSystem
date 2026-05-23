package com.nhom1.auction.server.auction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.nhom1.auction.common.dto.auction.CreateAuctionRequest;
import com.nhom1.auction.common.dto.auction.UpdateAuctionRequest;
import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.entity.Item;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.exception.InvalidAuctionStateException;
import com.nhom1.auction.common.exception.NotFoundException;
import com.nhom1.auction.common.exception.UnauthorizedActionException;
import com.nhom1.auction.common.exception.ValidationException;
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

public class AuctionServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private DataSource dataSource;

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
        auctionService = new AuctionService(
            auctionRepository,
            itemRepository,
            itemImageRepository,
            dataSource
        );
        when(dataSource.getConnection()).thenReturn(connection);
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
        verify(auctionRepository).updateStatus(
            any(UUID.class),
            eq(AuctionStatus.PENDING),
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
        verify(auctionRepository, never()).updateStatus(
            any(UUID.class),
            any(AuctionStatus.class),
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
        when(auction.getStatus()).thenReturn(AuctionStatus.OPEN);
        when(auctionRepository.findById(parsedAuctionId)).thenReturn(
            Optional.of(auction)
        );
        when(connection.getAutoCommit()).thenReturn(true);
        when(
            auctionRepository.deleteById(parsedAuctionId, connection)
        ).thenReturn(1);
        when(
            itemRepository.deleteById(any(UUID.class), eq(connection))
        ).thenReturn(1);

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
        when(auction.getStatus()).thenReturn(AuctionStatus.OPEN);
        when(auctionRepository.findById(parsedAuctionId)).thenReturn(
            Optional.of(auction)
        );
        when(connection.getAutoCommit()).thenReturn(true);
        when(
            auctionRepository.deleteById(parsedAuctionId, connection)
        ).thenReturn(1);
        when(
            itemRepository.deleteById(any(UUID.class), eq(connection))
        ).thenReturn(0);

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

    @Test
    public void testDeleteAuction_NonOpenAuction_ThrowsInvalidState() {
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        UUID parsedSellerId = UUID.fromString(sellerId);
        UUID parsedAuctionId = UUID.fromString(auctionId);
        Auction auction = mock(Auction.class);
        when(auction.getSellerId()).thenReturn(parsedSellerId);
        when(auction.getStatus()).thenReturn(AuctionStatus.RUNNING);
        when(auctionRepository.findById(parsedAuctionId)).thenReturn(
            Optional.of(auction)
        );

        assertThrows(InvalidAuctionStateException.class, () ->
            auctionService.deleteAuction(sellerId, auctionId)
        );
    }

    @Test
    public void testUpdateAuction_PendingAuction_UpdatesItemAndAuction()
        throws SQLException {
        String sellerId = UUID.randomUUID().toString();
        UUID parsedSellerId = UUID.fromString(sellerId);
        UUID parsedAuctionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UpdateAuctionRequest dto = createValidUpdateAuctionRequest(
            sellerId,
            parsedAuctionId.toString()
        );
        Auction auction = mock(Auction.class);
        when(auction.getSellerId()).thenReturn(parsedSellerId);
        when(auction.getItemId()).thenReturn(itemId);
        when(auction.getStatus()).thenReturn(AuctionStatus.PENDING);
        when(auctionRepository.findById(parsedAuctionId)).thenReturn(
            Optional.of(auction)
        );
        when(connection.getAutoCommit()).thenReturn(true);
        when(
            itemRepository.updateBasicInfo(
                eq(itemId),
                eq(dto.getName()),
                eq(dto.getDescription()),
                eq(dto.getCategory()),
                eq(dto.getCondition()),
                eq(connection)
            )
        ).thenReturn(1);
        when(
            auctionRepository.updateOpenAuctionForEdit(
                eq(parsedAuctionId),
                eq(dto.getStartingPrice()),
                eq(dto.getEndTime()),
                eq(connection)
            )
        ).thenReturn(1);

        assertDoesNotThrow(() -> auctionService.updateAuction(dto));

        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection).setAutoCommit(true);
    }

    @Test
    public void testUpdateAuction_RunningAuction_ThrowsInvalidState() {
        String sellerId = UUID.randomUUID().toString();
        UUID parsedSellerId = UUID.fromString(sellerId);
        UUID parsedAuctionId = UUID.randomUUID();
        UpdateAuctionRequest dto = createValidUpdateAuctionRequest(
            sellerId,
            parsedAuctionId.toString()
        );
        Auction auction = mock(Auction.class);
        when(auction.getSellerId()).thenReturn(parsedSellerId);
        when(auction.getStatus()).thenReturn(AuctionStatus.RUNNING);
        when(auctionRepository.findById(parsedAuctionId)).thenReturn(
            Optional.of(auction)
        );

        assertThrows(InvalidAuctionStateException.class, () ->
            auctionService.updateAuction(dto)
        );
    }

    private CreateAuctionRequest createValidCreateAuctionRequest() {
        CreateAuctionRequest dto = new CreateAuctionRequest();
        dto.setName("Test Item");
        dto.setDescription("Test Description");
        dto.setCategory(ItemCategory.ART);
        dto.setCondition(ItemCondition.NEW);
        dto.setStartingPrice(new BigDecimal("100.00"));
        dto.setStartTime(LocalDateTime.now().plusDays(1));
        dto.setEndTime(LocalDateTime.now().plusDays(2));
        return dto;
    }

    @Test
    public void testGetMyListings_Success() {
        UUID sellerId = UUID.randomUUID();
        Auction auction = new Auction(
            UUID.randomUUID(),
            sellerId,
            new BigDecimal("100"),
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1)
        );
        Item item = new Item(
            "Art Item",
            "Desc",
            ItemCategory.ART,
            ItemCondition.NEW
        ) {
            @Override
            public void printInfo() {}
        };

        when(auctionRepository.findBySellerId(sellerId)).thenReturn(
            java.util.List.of(auction)
        );
        when(itemRepository.findById(auction.getItemId())).thenReturn(
            Optional.of(item)
        );

        var result = auctionService.getMyListings(sellerId.toString());
        assertEquals(1, result.size());
        assertEquals("Art Item", result.get(0).getItemName());
        assertEquals("ART", result.get(0).getItemCategory());
    }

    @Test
    public void testGetMyListings_ItemNotFound_FiltersRow() {
        UUID sellerId = UUID.randomUUID();
        Auction auction = new Auction(
            UUID.randomUUID(),
            sellerId,
            new BigDecimal("100"),
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1)
        );

        when(auctionRepository.findBySellerId(sellerId)).thenReturn(
            java.util.List.of(auction)
        );
        when(itemRepository.findById(auction.getItemId())).thenReturn(
            Optional.empty()
        );

        var result = auctionService.getMyListings(sellerId.toString());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCreateAuction_NullRequest_Throws() {
        assertThrows(ValidationException.class, () ->
            auctionService.createAuction(UUID.randomUUID().toString(), null)
        );
    }

    @Test
    public void testCreateAuction_MissingTimes_Throws() {
        CreateAuctionRequest request = createValidCreateAuctionRequest();
        request.setStartTime(null);
        assertThrows(ValidationException.class, () ->
            auctionService.createAuction(UUID.randomUUID().toString(), request)
        );
    }

    @Test
    public void testCreateAuction_NullCategory_Throws() {
        CreateAuctionRequest request = createValidCreateAuctionRequest();
        request.setCategory(null);
        assertThrows(ValidationException.class, () ->
            auctionService.createAuction(UUID.randomUUID().toString(), request)
        );
    }

    @Test
    public void testCreateAuction_ElectronicsAndVehicleCategory()
        throws Exception {
        String sellerId = UUID.randomUUID().toString();
        CreateAuctionRequest reqElectronics = createValidCreateAuctionRequest();
        reqElectronics.setCategory(ItemCategory.ELECTRONICS);

        CreateAuctionRequest reqVehicle = createValidCreateAuctionRequest();
        reqVehicle.setCategory(ItemCategory.VEHICLE);

        when(connection.getAutoCommit()).thenReturn(true);

        assertNotNull(auctionService.createAuction(sellerId, reqElectronics));
        assertNotNull(auctionService.createAuction(sellerId, reqVehicle));
    }

    @Test
    public void testDeleteAuction_BlankOrInvalidUUID_Throws() {
        String sellerId = UUID.randomUUID().toString();
        assertThrows(ValidationException.class, () ->
            auctionService.deleteAuction(sellerId, null)
        );
        assertThrows(ValidationException.class, () ->
            auctionService.deleteAuction(sellerId, "   ")
        );
        assertThrows(ValidationException.class, () ->
            auctionService.deleteAuction(sellerId, "invalid-uuid")
        );
    }

    @Test
    public void testDeleteAuction_ZeroAuctionsDeleted_ThrowsIllegalStateException()
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
        when(
            auctionRepository.deleteById(parsedAuctionId, connection)
        ).thenReturn(0);

        assertThrows(RuntimeException.class, () ->
            auctionService.deleteAuction(sellerId, auctionId)
        );
    }

    @Test
    public void testDeleteAuction_ZeroItemsDeleted_ThrowsIllegalStateException()
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
        when(
            auctionRepository.deleteById(parsedAuctionId, connection)
        ).thenReturn(1);
        when(
            itemRepository.deleteById(any(UUID.class), eq(connection))
        ).thenReturn(0);

        assertThrows(RuntimeException.class, () ->
            auctionService.deleteAuction(sellerId, auctionId)
        );
    }

    @Test
    public void testDeleteAuction_SqlConnectionFails_ThrowsRuntimeException()
        throws SQLException {
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        UUID parsedAuctionId = UUID.fromString(auctionId);
        UUID parsedSellerId = UUID.fromString(sellerId);
        Auction auction = mock(Auction.class);
        when(auction.getSellerId()).thenReturn(parsedSellerId);
        when(auction.getItemId()).thenReturn(UUID.randomUUID());
        when(auctionRepository.findById(parsedAuctionId)).thenReturn(
            Optional.of(auction)
        );
        when(dataSource.getConnection()).thenThrow(
            new SQLException("Connection failed")
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            auctionService.deleteAuction(sellerId, auctionId)
        );
        assertEquals("Delete transaction failed", ex.getMessage());
    }

    // ======================== updateAuction ========================

    private UpdateAuctionRequest buildValidUpdateRequest(
        String sellerId,
        String auctionId
    ) {
        UpdateAuctionRequest req = new UpdateAuctionRequest();
        req.setSellerId(sellerId);
        req.setAuctionId(auctionId);
        req.setName("Updated Name");
        req.setDescription("Updated Desc");
        req.setCategory(ItemCategory.ART);
        req.setCondition(ItemCondition.NEW);
        req.setStartingPrice(new BigDecimal("200.00"));
        req.setEndTime(LocalDateTime.now().plusDays(5));
        return req;
    }

    private Auction buildOpenAuction(UUID auctionUuid, UUID sellerUuid) {
        Auction auction = mock(Auction.class);
        when(auction.getId()).thenReturn(auctionUuid);
        when(auction.getSellerId()).thenReturn(sellerUuid);
        when(auction.getStatus()).thenReturn(AuctionStatus.OPEN);
        when(auction.getHighestBidderId()).thenReturn(null);
        when(auction.getStartTime()).thenReturn(
            LocalDateTime.now().minusDays(1)
        );
        when(auction.getItemId()).thenReturn(UUID.randomUUID());
        return auction;
    }

    @Test
    public void testUpdateAuction_NullRequest_Throws() {
        assertThrows(ValidationException.class, () ->
            auctionService.updateAuction(null)
        );
    }

    @Test
    public void testUpdateAuction_NullSellerId_Throws() {
        UpdateAuctionRequest req = buildValidUpdateRequest(
            null,
            UUID.randomUUID().toString()
        );
        assertThrows(ValidationException.class, () ->
            auctionService.updateAuction(req)
        );
    }

    @Test
    public void testUpdateAuction_BlankSellerId_Throws() {
        UpdateAuctionRequest req = buildValidUpdateRequest(
            "",
            UUID.randomUUID().toString()
        );
        assertThrows(ValidationException.class, () ->
            auctionService.updateAuction(req)
        );
    }

    @Test
    public void testUpdateAuction_InvalidSellerUUID_Throws() {
        UpdateAuctionRequest req = buildValidUpdateRequest(
            "not-a-uuid",
            UUID.randomUUID().toString()
        );
        assertThrows(ValidationException.class, () ->
            auctionService.updateAuction(req)
        );
    }

    @Test
    public void testUpdateAuction_NullAuctionId_Throws() {
        UpdateAuctionRequest req = buildValidUpdateRequest(
            UUID.randomUUID().toString(),
            null
        );
        assertThrows(ValidationException.class, () ->
            auctionService.updateAuction(req)
        );
    }

    @Test
    public void testUpdateAuction_BlankAuctionId_Throws() {
        UpdateAuctionRequest req = buildValidUpdateRequest(
            UUID.randomUUID().toString(),
            ""
        );
        assertThrows(ValidationException.class, () ->
            auctionService.updateAuction(req)
        );
    }

    @Test
    public void testUpdateAuction_InvalidAuctionUUID_Throws() {
        UpdateAuctionRequest req = buildValidUpdateRequest(
            UUID.randomUUID().toString(),
            "bad-uuid"
        );
        assertThrows(ValidationException.class, () ->
            auctionService.updateAuction(req)
        );
    }

    @Test
    public void testUpdateAuction_AuctionNotFound_Throws() {
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        when(auctionRepository.findById(UUID.fromString(auctionId))).thenReturn(
            Optional.empty()
        );
        UpdateAuctionRequest req = buildValidUpdateRequest(sellerId, auctionId);
        assertThrows(NotFoundException.class, () ->
            auctionService.updateAuction(req)
        );
    }

    @Test
    public void testUpdateAuction_NonOwner_Throws() {
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        UUID auctionUuid = UUID.fromString(auctionId);
        Auction auction = buildOpenAuction(auctionUuid, UUID.randomUUID()); // different seller
        when(auctionRepository.findById(auctionUuid)).thenReturn(
            Optional.of(auction)
        );
        UpdateAuctionRequest req = buildValidUpdateRequest(sellerId, auctionId);
        assertThrows(UnauthorizedActionException.class, () ->
            auctionService.updateAuction(req)
        );
    }

    @Test
    public void testUpdateAuction_StatusNotOpen_Throws() {
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        UUID sellerUuid = UUID.fromString(sellerId);
        UUID auctionUuid = UUID.fromString(auctionId);
        Auction auction = mock(Auction.class);
        when(auction.getSellerId()).thenReturn(sellerUuid);
        when(auction.getStatus()).thenReturn(AuctionStatus.FINISHED);
        when(auction.getStartTime()).thenReturn(
            LocalDateTime.now().minusDays(1)
        );
        when(auctionRepository.findById(auctionUuid)).thenReturn(
            Optional.of(auction)
        );
        UpdateAuctionRequest req = buildValidUpdateRequest(sellerId, auctionId);
        assertThrows(ValidationException.class, () ->
            auctionService.updateAuction(req)
        );
    }

    @Test
    public void testUpdateAuction_HasBidder_Throws() {
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        UUID sellerUuid = UUID.fromString(sellerId);
        UUID auctionUuid = UUID.fromString(auctionId);
        Auction auction = mock(Auction.class);
        when(auction.getSellerId()).thenReturn(sellerUuid);
        when(auction.getStatus()).thenReturn(AuctionStatus.OPEN);
        when(auction.getHighestBidderId()).thenReturn(UUID.randomUUID()); // has a bidder
        when(auction.getStartTime()).thenReturn(
            LocalDateTime.now().minusDays(1)
        );
        when(auctionRepository.findById(auctionUuid)).thenReturn(
            Optional.of(auction)
        );
        UpdateAuctionRequest req = buildValidUpdateRequest(sellerId, auctionId);
        assertThrows(ValidationException.class, () ->
            auctionService.updateAuction(req)
        );
    }

    @Test
    public void testUpdateAuction_NullEndTime_Throws() {
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        UUID sellerUuid = UUID.fromString(sellerId);
        UUID auctionUuid = UUID.fromString(auctionId);
        Auction auction = buildOpenAuction(auctionUuid, sellerUuid);
        when(auctionRepository.findById(auctionUuid)).thenReturn(
            Optional.of(auction)
        );
        UpdateAuctionRequest req = buildValidUpdateRequest(sellerId, auctionId);
        req.setEndTime(null);
        assertThrows(ValidationException.class, () ->
            auctionService.updateAuction(req)
        );
    }

    @Test
    public void testUpdateAuction_EndTimeNotAfterStartTime_Throws() {
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        UUID sellerUuid = UUID.fromString(sellerId);
        UUID auctionUuid = UUID.fromString(auctionId);
        Auction auction = mock(Auction.class);
        when(auction.getSellerId()).thenReturn(sellerUuid);
        when(auction.getStatus()).thenReturn(AuctionStatus.OPEN);
        when(auction.getHighestBidderId()).thenReturn(null);
        LocalDateTime startTime = LocalDateTime.now().plusDays(2);
        when(auction.getStartTime()).thenReturn(startTime);
        when(auctionRepository.findById(auctionUuid)).thenReturn(
            Optional.of(auction)
        );
        UpdateAuctionRequest req = buildValidUpdateRequest(sellerId, auctionId);
        req.setEndTime(startTime.minusHours(1)); // end before start
        assertThrows(ValidationException.class, () ->
            auctionService.updateAuction(req)
        );
    }

    @Test
    public void testUpdateAuction_EndTimeInPast_Throws() {
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        UUID sellerUuid = UUID.fromString(sellerId);
        UUID auctionUuid = UUID.fromString(auctionId);
        Auction auction = buildOpenAuction(auctionUuid, sellerUuid);
        when(auctionRepository.findById(auctionUuid)).thenReturn(
            Optional.of(auction)
        );
        UpdateAuctionRequest req = buildValidUpdateRequest(sellerId, auctionId);
        req.setEndTime(LocalDateTime.now().minusHours(1)); // past
        assertThrows(ValidationException.class, () ->
            auctionService.updateAuction(req)
        );
    }

    @Test
    public void testUpdateAuction_ZeroStartingPrice_Throws() {
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        UUID sellerUuid = UUID.fromString(sellerId);
        UUID auctionUuid = UUID.fromString(auctionId);
        Auction auction = buildOpenAuction(auctionUuid, sellerUuid);
        when(auctionRepository.findById(auctionUuid)).thenReturn(
            Optional.of(auction)
        );
        UpdateAuctionRequest req = buildValidUpdateRequest(sellerId, auctionId);
        req.setStartingPrice(BigDecimal.ZERO);
        assertThrows(ValidationException.class, () ->
            auctionService.updateAuction(req)
        );
    }

    @Test
    public void testUpdateAuction_NullStartingPrice_Throws() {
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        UUID sellerUuid = UUID.fromString(sellerId);
        UUID auctionUuid = UUID.fromString(auctionId);
        Auction auction = buildOpenAuction(auctionUuid, sellerUuid);
        when(auctionRepository.findById(auctionUuid)).thenReturn(
            Optional.of(auction)
        );
        UpdateAuctionRequest req = buildValidUpdateRequest(sellerId, auctionId);
        req.setStartingPrice(null);
        assertThrows(ValidationException.class, () ->
            auctionService.updateAuction(req)
        );
    }

    @Test
    public void testUpdateAuction_BlankName_Throws() {
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        UUID sellerUuid = UUID.fromString(sellerId);
        UUID auctionUuid = UUID.fromString(auctionId);
        Auction auction = buildOpenAuction(auctionUuid, sellerUuid);
        when(auctionRepository.findById(auctionUuid)).thenReturn(
            Optional.of(auction)
        );
        UpdateAuctionRequest req = buildValidUpdateRequest(sellerId, auctionId);
        req.setName("  ");
        assertThrows(ValidationException.class, () ->
            auctionService.updateAuction(req)
        );
    }

    @Test
    public void testUpdateAuction_NullCategory_Throws() {
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        UUID sellerUuid = UUID.fromString(sellerId);
        UUID auctionUuid = UUID.fromString(auctionId);
        Auction auction = buildOpenAuction(auctionUuid, sellerUuid);
        when(auctionRepository.findById(auctionUuid)).thenReturn(
            Optional.of(auction)
        );
        UpdateAuctionRequest req = buildValidUpdateRequest(sellerId, auctionId);
        req.setCategory(null);
        assertThrows(ValidationException.class, () ->
            auctionService.updateAuction(req)
        );
    }

    @Test
    public void testUpdateAuction_Success() throws Exception {
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        UUID sellerUuid = UUID.fromString(sellerId);
        UUID auctionUuid = UUID.fromString(auctionId);
        Auction auction = buildOpenAuction(auctionUuid, sellerUuid);
        when(auctionRepository.findById(auctionUuid)).thenReturn(
            Optional.of(auction)
        );
        when(connection.getAutoCommit()).thenReturn(true);
        when(
            itemRepository.updateBasicInfo(
                any(),
                any(),
                any(),
                any(),
                any(),
                eq(connection)
            )
        ).thenReturn(1);
        when(
            auctionRepository.updateOpenAuctionForEdit(
                any(),
                any(),
                any(),
                eq(connection)
            )
        ).thenReturn(1);
        UpdateAuctionRequest req = buildValidUpdateRequest(sellerId, auctionId);
        assertDoesNotThrow(() -> auctionService.updateAuction(req));
        verify(connection).commit();
    }

    @Test
    public void testUpdateAuction_AuctionNoLongerEditable_Throws()
        throws Exception {
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        UUID sellerUuid = UUID.fromString(sellerId);
        UUID auctionUuid = UUID.fromString(auctionId);
        Auction auction = buildOpenAuction(auctionUuid, sellerUuid);
        when(auctionRepository.findById(auctionUuid)).thenReturn(
            Optional.of(auction)
        );
        when(connection.getAutoCommit()).thenReturn(true);
        when(
            itemRepository.updateBasicInfo(
                any(),
                any(),
                any(),
                any(),
                any(),
                eq(connection)
            )
        ).thenReturn(1);
        when(
            auctionRepository.updateOpenAuctionForEdit(
                any(),
                any(),
                any(),
                eq(connection)
            )
        ).thenReturn(0);
        UpdateAuctionRequest req = buildValidUpdateRequest(sellerId, auctionId);
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            auctionService.updateAuction(req)
        );
        // inner cause is ValidationException
        assertTrue(
            ex.getCause() instanceof ValidationException ||
                ex instanceof ValidationException
        );
        verify(connection).rollback();
    }

    @Test
    public void testUpdateAuction_ItemNotFound_Throws() throws Exception {
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        UUID sellerUuid = UUID.fromString(sellerId);
        UUID auctionUuid = UUID.fromString(auctionId);
        Auction auction = buildOpenAuction(auctionUuid, sellerUuid);
        when(auctionRepository.findById(auctionUuid)).thenReturn(
            Optional.of(auction)
        );
        when(connection.getAutoCommit()).thenReturn(true);
        when(
            itemRepository.updateBasicInfo(
                any(),
                any(),
                any(),
                any(),
                any(),
                eq(connection)
            )
        ).thenReturn(0);
        when(
            auctionRepository.updateOpenAuctionForEdit(
                any(),
                any(),
                any(),
                eq(connection)
            )
        ).thenReturn(1);
        UpdateAuctionRequest req = buildValidUpdateRequest(sellerId, auctionId);
        assertThrows(RuntimeException.class, () ->
            auctionService.updateAuction(req)
        );
        verify(connection).rollback();
    }

    @Test
    public void testUpdateAuction_SqlConnectionFails_Throws() throws Exception {
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        UUID sellerUuid = UUID.fromString(sellerId);
        UUID auctionUuid = UUID.fromString(auctionId);
        Auction auction = buildOpenAuction(auctionUuid, sellerUuid);
        when(auctionRepository.findById(auctionUuid)).thenReturn(
            Optional.of(auction)
        );
        when(dataSource.getConnection()).thenThrow(new SQLException("DB down"));
        UpdateAuctionRequest req = buildValidUpdateRequest(sellerId, auctionId);
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            auctionService.updateAuction(req)
        );
        assertEquals("Update auction transaction failed", ex.getMessage());
    }

    @Test
    public void testUpdateAuction_RunningStatusRevertedToOpen_ThenEdited()
        throws Exception {
        // When auction is RUNNING but start_time is in the future, it gets reverted to OPEN
        String sellerId = UUID.randomUUID().toString();
        String auctionId = UUID.randomUUID().toString();
        UUID sellerUuid = UUID.fromString(sellerId);
        UUID auctionUuid = UUID.fromString(auctionId);

        Auction runningAuction = mock(Auction.class);
        when(runningAuction.getId()).thenReturn(auctionUuid);
        when(runningAuction.getSellerId()).thenReturn(sellerUuid);
        when(runningAuction.getStatus()).thenReturn(AuctionStatus.RUNNING);
        when(runningAuction.getStartTime()).thenReturn(
            LocalDateTime.now().plusDays(2)
        ); // future start
        when(runningAuction.getHighestBidderId()).thenReturn(null);
        when(runningAuction.getItemId()).thenReturn(UUID.randomUUID());

        Auction openAuction = buildOpenAuction(auctionUuid, sellerUuid);

        // First findById returns running, second returns open (after status revert)
        when(auctionRepository.findById(auctionUuid))
            .thenReturn(Optional.of(runningAuction))
            .thenReturn(Optional.of(openAuction));
        when(connection.getAutoCommit()).thenReturn(true);
        when(
            itemRepository.updateBasicInfo(
                any(),
                any(),
                any(),
                any(),
                any(),
                eq(connection)
            )
        ).thenReturn(1);
        when(
            auctionRepository.updateOpenAuctionForEdit(
                any(),
                any(),
                any(),
                eq(connection)
            )
        ).thenReturn(1);
        UpdateAuctionRequest req = buildValidUpdateRequest(sellerId, auctionId);
        assertDoesNotThrow(() -> auctionService.updateAuction(req));
        verify(auctionRepository).updateStatus(auctionUuid, AuctionStatus.OPEN);
    }

    // ======================== getMyListings edge cases ========================

    @Test
    public void testGetMyListings_NullCategory_UsesUnknown() {
        UUID sellerId = UUID.randomUUID();
        Auction auction = new Auction(
            UUID.randomUUID(),
            sellerId,
            new BigDecimal("100"),
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1)
        );
        Item item = mock(Item.class);
        when(item.getName()).thenReturn("Art");
        when(item.getCategory()).thenReturn(null); // null category
        when(auctionRepository.findBySellerId(sellerId)).thenReturn(
            List.of(auction)
        );
        when(itemRepository.findById(auction.getItemId())).thenReturn(
            Optional.of(item)
        );
        var result = auctionService.getMyListings(sellerId.toString());
        assertEquals(1, result.size());
        assertEquals("UNKNOWN", result.get(0).getItemCategory());
    }

    @Test
    public void testGetMyListings_RepoThrows_ReturnsEmpty() {
        UUID sellerId = UUID.randomUUID();
        when(auctionRepository.findBySellerId(sellerId)).thenThrow(
            new RuntimeException("DB error")
        );
        var result = auctionService.getMyListings(sellerId.toString());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCreateAuction_DurationDaysZero_Throws() {
        String sellerId = UUID.randomUUID().toString();
        CreateAuctionRequest dto = createValidCreateAuctionRequest();
        dto.setEndTime(null);
        dto.setDurationDays(0); // 0 is invalid
        assertThrows(ValidationException.class, () ->
            auctionService.createAuction(sellerId, dto)
        );
    }

    @Test
    public void testCreateAuction_NullDurationDays_DefaultsToSeven()
        throws Exception {
        String sellerId = UUID.randomUUID().toString();
        CreateAuctionRequest dto = createValidCreateAuctionRequest();
        dto.setEndTime(null);
        dto.setDurationDays(null); // null -> defaults to 7
        when(connection.getAutoCommit()).thenReturn(true);
        Auction result = auctionService.createAuction(sellerId, dto);
        assertNotNull(result);
        // end time should be ~7 days from start time
        assertTrue(result.getEndTime().isAfter(dto.getStartTime().plusDays(6)));
    }

    @Test
    public void testCreateAuction_DurationDaysPositive_UsesSpecified()
        throws Exception {
        String sellerId = UUID.randomUUID().toString();
        CreateAuctionRequest dto = createValidCreateAuctionRequest();
        dto.setEndTime(null);
        dto.setDurationDays(14);
        when(connection.getAutoCommit()).thenReturn(true);
        Auction result = auctionService.createAuction(sellerId, dto);
        assertNotNull(result);
        assertTrue(
            result.getEndTime().isAfter(dto.getStartTime().plusDays(13))
        );
    }

    @Test
    public void testCreateAuction_NegativeDurationDays_Throws() {
        String sellerId = UUID.randomUUID().toString();
        CreateAuctionRequest dto = createValidCreateAuctionRequest();
        dto.setDurationDays(-3);
        assertThrows(ValidationException.class, () ->
            auctionService.createAuction(sellerId, dto)
        );
    }

    @Test
    public void testCreateAuction_StartTimeInPast_Throws() {
        String sellerId = UUID.randomUUID().toString();
        CreateAuctionRequest dto = createValidCreateAuctionRequest();
        dto.setStartTime(LocalDateTime.now().minusDays(1));
        assertThrows(ValidationException.class, () ->
            auctionService.createAuction(sellerId, dto)
        );
    }

    @Test
    public void testCreateAuction_SellerIdInRequestMatchesSeller_Succeeds()
        throws Exception {
        String sellerId = UUID.randomUUID().toString();
        CreateAuctionRequest dto = createValidCreateAuctionRequest();
        dto.setSellerId(sellerId); // same as parameter
        when(connection.getAutoCommit()).thenReturn(true);
        assertNotNull(auctionService.createAuction(sellerId, dto));
    }

    @Test
    public void testCreateAuction_AppExceptionRollsBack() throws SQLException {
        String sellerId = UUID.randomUUID().toString();
        CreateAuctionRequest dto = createValidCreateAuctionRequest();
        when(connection.getAutoCommit()).thenReturn(true);
        doThrow(new ValidationException("bad item"))
            .when(itemRepository)
            .save(any(), any(), eq(connection));

        assertThrows(ValidationException.class, () ->
            auctionService.createAuction(sellerId, dto)
        );
        verify(connection).rollback();
    }
}
