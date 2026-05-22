package com.nhom1.auction.server.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nhom1.auction.common.dto.admin.AdminAuctionListResponse;
import com.nhom1.auction.common.dto.admin.AdminUserListResponse;
import com.nhom1.auction.common.dto.admin.UserSummaryDto;
import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.entity.User;
import com.nhom1.auction.common.enums.UserRole;
import com.nhom1.auction.common.exception.AuthenticationException;
import com.nhom1.auction.common.exception.InvalidAuctionStateException;
import com.nhom1.auction.common.exception.NotFoundException;
import com.nhom1.auction.common.exception.UnauthorizedActionException;
import com.nhom1.auction.common.exception.ValidationException;
import com.nhom1.auction.server.auction.AuctionRepository;
import com.nhom1.auction.server.auction.ItemRepository;
import com.nhom1.auction.server.auth.UserRepository;
import com.nhom1.auction.server.bidding.BidRepository;
import com.nhom1.auction.server.infrastructure.NotificationService;
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


public class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private AdminAuctionGateway adminAuctionGateway;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    private AdminService adminService;

    @BeforeEach
    public void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        when(dataSource.getConnection()).thenReturn(connection);
        adminService = new AdminService(
            userRepository,
            auctionRepository,
            itemRepository,
            bidRepository,
            adminAuctionGateway,
            notificationService,
            dataSource
        );
    }

    @Test
    public void testGetAllUsers_AdminCaller_ReturnsUserList() {
        User admin = new User(
            "admin",
            "admin@example.com",
            "password",
            UserRole.ADMIN
        );
        String callerId = admin.getId().toString();
        when(userRepository.findById(admin.getId())).thenReturn(
            Optional.of(admin)
        );
        List<User> users = List.of(admin);
        when(userRepository.findAll()).thenReturn(users);

        AdminUserListResponse result = adminService.getAllUsers(callerId);

        assertNotNull(result);
        assertEquals(1, result.getUsers().size());
    }

    @Test
    public void testGetAllUsers_NonAdminCaller_ThrowsUnauthorizedActionException() {
        User user = new User(
            "user",
            "user@example.com",
            "password",
            UserRole.USER
        );
        String callerId = user.getId().toString();
        when(userRepository.findById(user.getId())).thenReturn(
            Optional.of(user)
        );

        assertThrows(UnauthorizedActionException.class, () ->
            adminService.getAllUsers(callerId)
        );
    }

    @Test
    public void testDeleteUser_AdminDeletesNormalUser_DeletesSuccessfully()
        throws SQLException {
        User admin = new User(
            "admin",
            "admin@example.com",
            "password",
            UserRole.ADMIN
        );
        User target = new User(
            "user",
            "user@example.com",
            "password",
            UserRole.USER
        );
        String callerId = admin.getId().toString();
        String targetId = target.getId().toString();
        when(userRepository.findById(admin.getId())).thenReturn(
            Optional.of(admin)
        );
        when(userRepository.findById(target.getId())).thenReturn(
            Optional.of(target)
        );
        when(connection.getAutoCommit()).thenReturn(true);
        when(userRepository.deleteById(target.getId(), connection)).thenReturn(true);

        String result = adminService.deleteUser(targetId, callerId);

        assertEquals("DELETED", result);
        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection).setAutoCommit(true);
    }

    @Test
    public void testDeleteUser_BidDeleteFails_RollsBackAndRestoresAutoCommit()
        throws SQLException {
        User admin = new User(
            "admin",
            "admin@example.com",
            "password",
            UserRole.ADMIN
        );
        User target = new User(
            "user",
            "user@example.com",
            "password",
            UserRole.USER
        );
        String callerId = admin.getId().toString();
        String targetId = target.getId().toString();
        when(userRepository.findById(admin.getId())).thenReturn(
            Optional.of(admin)
        );
        when(userRepository.findById(target.getId())).thenReturn(
            Optional.of(target)
        );
        when(connection.getAutoCommit()).thenReturn(true);
        doThrow(new RuntimeException("delete bids failed"))
            .when(bidRepository)
            .deleteByBidderId(target.getId(), connection);

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            adminService.deleteUser(targetId, callerId)
        );

        assertEquals("User deletion failed", thrown.getMessage());
        assertEquals("delete bids failed", thrown.getCause().getMessage());
        verify(connection).rollback();
        verify(connection).setAutoCommit(true);
        verify(connection, never()).commit();
    }

    @Test
    public void testDeleteUser_AdminDeletesSelf_ThrowsUnauthorizedActionException() {
        User admin = new User(
            "admin",
            "admin@example.com",
            "password",
            UserRole.ADMIN
        );
        String callerId = admin.getId().toString();
        when(userRepository.findById(admin.getId())).thenReturn(
            Optional.of(admin)
        );

        assertThrows(UnauthorizedActionException.class, () ->
            adminService.deleteUser(callerId, callerId)
        );
    }

    @Test
    public void testDeleteUser_AdminDeletesAnotherAdmin_ThrowsUnauthorizedActionException() {
        User admin = new User(
            "admin",
            "admin@example.com",
            "password",
            UserRole.ADMIN
        );
        User targetAdmin = new User(
            "admin2",
            "admin2@example.com",
            "password",
            UserRole.ADMIN
        );
        String callerId = admin.getId().toString();
        String targetId = targetAdmin.getId().toString();
        when(userRepository.findById(admin.getId())).thenReturn(
            Optional.of(admin)
        );
        when(userRepository.findById(targetAdmin.getId())).thenReturn(
            Optional.of(targetAdmin)
        );

        assertThrows(UnauthorizedActionException.class, () ->
            adminService.deleteUser(targetId, callerId)
        );
    }

    @Test
    public void testDeleteUser_TargetNotFound_ThrowsNotFoundException() {
        User admin = new User(
            "admin",
            "admin@example.com",
            "password",
            UserRole.ADMIN
        );
        String callerId = admin.getId().toString();
        String targetId = UUID.randomUUID().toString();
        when(userRepository.findById(admin.getId())).thenReturn(
            Optional.of(admin)
        );
        when(userRepository.findById(UUID.fromString(targetId))).thenReturn(
            Optional.empty()
        );

        assertThrows(NotFoundException.class, () ->
            adminService.deleteUser(targetId, callerId)
        );
    }

    @Test
    public void testCancelAuction_AdminCancels_ReturnsCanceled() {
        User admin = new User(
            "admin",
            "admin@example.com",
            "password",
            UserRole.ADMIN
        );
        String callerId = admin.getId().toString();
        String auctionId = UUID.randomUUID().toString();
        when(userRepository.findById(admin.getId())).thenReturn(
            Optional.of(admin)
        );
        when(adminAuctionGateway.cancelAuctionById(auctionId)).thenReturn(true);

        String result = adminService.cancelAuction(auctionId, callerId);

        assertEquals("CANCELED", result);
    }

    @Test
    public void testCancelAuction_NonAdmin_ThrowsUnauthorizedActionException() {
        User user = new User(
            "user",
            "user@example.com",
            "password",
            UserRole.USER
        );
        String callerId = user.getId().toString();
        String auctionId = UUID.randomUUID().toString();
        when(userRepository.findById(user.getId())).thenReturn(
            Optional.of(user)
        );

        assertThrows(UnauthorizedActionException.class, () ->
            adminService.cancelAuction(auctionId, callerId)
        );
    }

    @Test
    public void testCancelAuction_InvalidStatus_ThrowsInvalidAuctionStateException() {
        User admin = new User(
            "admin",
            "admin@example.com",
            "password",
            UserRole.ADMIN
        );
        String callerId = admin.getId().toString();
        String auctionId = UUID.randomUUID().toString();
        when(userRepository.findById(admin.getId())).thenReturn(
            Optional.of(admin)
        );
        when(adminAuctionGateway.cancelAuctionById(auctionId)).thenReturn(
            false
        );

        assertThrows(InvalidAuctionStateException.class, () ->
            adminService.cancelAuction(auctionId, callerId)
        );
    }

    @Test
    public void testDeleteUser_RollsBackWhenDeleteFails() throws SQLException {
        User admin = new User(
            "admin",
            "admin@example.com",
            "password",
            UserRole.ADMIN
        );
        User target = new User(
            "user",
            "user@example.com",
            "password",
            UserRole.USER
        );
        when(userRepository.findById(admin.getId())).thenReturn(
            Optional.of(admin)
        );
        when(userRepository.findById(target.getId())).thenReturn(
            Optional.of(target)
        );
        when(connection.getAutoCommit()).thenReturn(true);
        when(userRepository.deleteById(target.getId(), connection)).thenReturn(false);

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            adminService.deleteUser(
                target.getId().toString(),
                admin.getId().toString()
            )
        );
        assertEquals("User deletion failed", thrown.getMessage());
        assertTrue(thrown.getCause() instanceof IllegalStateException);
        verify(connection).rollback();
    }

    @Test
    public void testGetAllAuctions_AdminCaller_Success() {
        User admin = new User("admin", "admin@example.com", "password", UserRole.ADMIN);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(adminAuctionGateway.findAllAuctionSummaries()).thenReturn(List.of());

        AdminAuctionListResponse response = adminService.getAllAuctions(admin.getId().toString());
        assertNotNull(response);
        assertNotNull(response.getAuctions());
    }

    @Test
    public void testGetAllAuctions_NonAdminCaller_ThrowsUnauthorizedActionException() {
        User user = new User("user", "user@example.com", "password", UserRole.USER);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedActionException.class, () ->
            adminService.getAllAuctions(user.getId().toString())
        );
    }

    @Test
    public void testDeleteUser_TargetUserIdBlank_ThrowsValidationException() {
        assertThrows(ValidationException.class, () ->
            adminService.deleteUser("", UUID.randomUUID().toString())
        );
        assertThrows(ValidationException.class, () ->
            adminService.deleteUser(null, UUID.randomUUID().toString())
        );
    }

    @Test
    public void testDeleteUser_CallerIdBlank_ThrowsValidationException() {
        assertThrows(ValidationException.class, () ->
            adminService.deleteUser(UUID.randomUUID().toString(), "")
        );
        assertThrows(ValidationException.class, () ->
            adminService.deleteUser(UUID.randomUUID().toString(), null)
        );
    }

    @Test
    public void testDeleteUser_CallerNotFound_ThrowsAuthenticationException() {
        UUID callerId = UUID.randomUUID();
        when(userRepository.findById(callerId)).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class, () ->
            adminService.deleteUser(UUID.randomUUID().toString(), callerId.toString())
        );
    }

    @Test
    public void testDeleteUser_CallerNotAdmin_ThrowsUnauthorizedActionException() {
        User user = new User("user", "user@example.com", "password", UserRole.USER);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedActionException.class, () ->
            adminService.deleteUser(UUID.randomUUID().toString(), user.getId().toString())
        );
    }

    @Test
    public void testDeleteUser_InvalidTargetUserId_ThrowsValidationException() {
        User admin = new User("admin", "admin@example.com", "password", UserRole.ADMIN);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThrows(ValidationException.class, () ->
            adminService.deleteUser("invalid-uuid", admin.getId().toString())
        );
    }

    @Test
    public void testDeleteUser_InvalidCallerId_ThrowsValidationException() {
        assertThrows(ValidationException.class, () ->
            adminService.deleteUser(UUID.randomUUID().toString(), "invalid-uuid")
        );
    }

    @Test
    public void testDeleteUser_SellerAuctionsSuccess_DeletesSellerAuctionsAndItems() throws SQLException {
        User admin = new User("admin", "admin@example.com", "password", UserRole.ADMIN);
        User seller = new User("seller", "seller@example.com", "password", UserRole.USER);
        UUID auctionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Auction auction = new Auction(
            auctionId,
            itemId,
            seller.getId(),
            BigDecimal.TEN,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            null,
            null,
            com.nhom1.auction.common.enums.AuctionStatus.OPEN,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(seller.getId())).thenReturn(Optional.of(seller));
        when(connection.getAutoCommit()).thenReturn(true);
        when(auctionRepository.findBySellerId(seller.getId(), connection)).thenReturn(List.of(auction));
        when(auctionRepository.deleteById(auctionId, connection)).thenReturn(1);
        when(itemRepository.deleteById(itemId, connection)).thenReturn(1);
        when(userRepository.deleteById(seller.getId(), connection)).thenReturn(true);

        String result = adminService.deleteUser(seller.getId().toString(), admin.getId().toString());
        assertEquals("DELETED", result);
        verify(auctionRepository).clearHighestBidderByUserId(seller.getId(), connection);
        verify(bidRepository).deleteByBidderId(seller.getId(), connection);
        verify(bidRepository).deleteByAuctionId(auctionId, connection);
        verify(connection).commit();
    }

    @Test
    public void testDeleteUser_SellerAuctionsDeleteAuctionFails_RollsBack() throws SQLException {
        User admin = new User("admin", "admin@example.com", "password", UserRole.ADMIN);
        User seller = new User("seller", "seller@example.com", "password", UserRole.USER);
        UUID auctionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Auction auction = new Auction(
            auctionId,
            itemId,
            seller.getId(),
            BigDecimal.TEN,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            null,
            null,
            com.nhom1.auction.common.enums.AuctionStatus.OPEN,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(seller.getId())).thenReturn(Optional.of(seller));
        when(connection.getAutoCommit()).thenReturn(true);
        when(auctionRepository.findBySellerId(seller.getId(), connection)).thenReturn(List.of(auction));
        when(auctionRepository.deleteById(auctionId, connection)).thenReturn(0);

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            adminService.deleteUser(seller.getId().toString(), admin.getId().toString())
        );
        assertTrue(thrown.getCause() instanceof IllegalStateException);
        verify(connection).rollback();
    }

    @Test
    public void testDeleteUser_SellerAuctionsDeleteItemFails_RollsBack() throws SQLException {
        User admin = new User("admin", "admin@example.com", "password", UserRole.ADMIN);
        User seller = new User("seller", "seller@example.com", "password", UserRole.USER);
        UUID auctionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Auction auction = new Auction(
            auctionId,
            itemId,
            seller.getId(),
            BigDecimal.TEN,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            null,
            null,
            com.nhom1.auction.common.enums.AuctionStatus.OPEN,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(seller.getId())).thenReturn(Optional.of(seller));
        when(connection.getAutoCommit()).thenReturn(true);
        when(auctionRepository.findBySellerId(seller.getId(), connection)).thenReturn(List.of(auction));
        when(auctionRepository.deleteById(auctionId, connection)).thenReturn(1);
        when(itemRepository.deleteById(itemId, connection)).thenReturn(0);

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            adminService.deleteUser(seller.getId().toString(), admin.getId().toString())
        );
        assertTrue(thrown.getCause() instanceof IllegalStateException);
        verify(connection).rollback();
    }

    @Test
    public void testCancelAuction_AuctionIdBlank_ThrowsValidationException() {
        User admin = new User("admin", "admin@example.com", "password", UserRole.ADMIN);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThrows(ValidationException.class, () ->
            adminService.cancelAuction("", admin.getId().toString())
        );
        assertThrows(ValidationException.class, () ->
            adminService.cancelAuction(null, admin.getId().toString())
        );
    }

    @Test
    public void testCancelAuction_InvalidAuctionId_ThrowsValidationException() {
        User admin = new User("admin", "admin@example.com", "password", UserRole.ADMIN);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThrows(ValidationException.class, () ->
            adminService.cancelAuction("invalid-uuid", admin.getId().toString())
        );
    }

    @Test
    public void testGetAllUsers_UserSummaryDtoWithNullCreatedAt() {
        User admin = new User("admin", "admin@example.com", "password", UserRole.ADMIN);
        User user = new User(UUID.randomUUID(), "user", "user@example.com", "password", UserRole.USER, null, null);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findAll()).thenReturn(List.of(user));

        AdminUserListResponse response = adminService.getAllUsers(admin.getId().toString());
        assertNotNull(response);
        assertEquals(1, response.getUsers().size());
        assertNotNull(response.getUsers().get(0).getCreatedAt());
    }
}

