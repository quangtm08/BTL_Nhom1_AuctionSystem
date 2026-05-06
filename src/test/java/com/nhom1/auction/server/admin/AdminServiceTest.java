package com.nhom1.auction.server.admin;

import com.nhom1.auction.common.dto.admin.AdminUserListResponse;
import com.nhom1.auction.common.entity.User;
import com.nhom1.auction.common.enums.UserRole;
import com.nhom1.auction.common.exception.AuthenticationException;
import com.nhom1.auction.common.exception.UnauthorizedActionException;
import com.nhom1.auction.common.exception.ValidationException;
import com.nhom1.auction.server.auction.AuctionRepository;
import com.nhom1.auction.server.auction.ItemRepository;
import com.nhom1.auction.server.auth.UserRepository;
import com.nhom1.auction.server.bidding.BidRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    private Connection connection;

    private AdminService adminService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        adminService = new AdminService(userRepository, auctionRepository, itemRepository, bidRepository, adminAuctionGateway, connection);
    }

    @Test
    public void testGetAllUsers_AdminCaller_ReturnsUserList() throws ValidationException, AuthenticationException, UnauthorizedActionException {
        User admin = new User("admin", "admin@example.com", "password", UserRole.ADMIN);
        String callerId = admin.getId().toString();
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        List<User> users = List.of(admin);
        when(userRepository.findAll()).thenReturn(users);

        AdminUserListResponse result = adminService.getAllUsers(callerId);

        assertNotNull(result);
        assertEquals(1, result.getUsers().size());
    }

    @Test
    public void testGetAllUsers_NonAdminCaller_ThrowsUnauthorizedActionException() {
        User user = new User("user", "user@example.com", "password", UserRole.USER);
        String callerId = user.getId().toString();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedActionException.class, () -> adminService.getAllUsers(callerId));
    }

    @Test
    public void testDeleteUser_AdminDeletesNormalUser_DeletesSuccessfully() throws ValidationException, AuthenticationException, UnauthorizedActionException, SQLException {
        User admin = new User("admin", "admin@example.com", "password", UserRole.ADMIN);
        User target = new User("user", "user@example.com", "password", UserRole.USER);
        String callerId = admin.getId().toString();
        String targetId = target.getId().toString();
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(connection.getAutoCommit()).thenReturn(true);
        when(userRepository.deleteById(target.getId())).thenReturn(true);

        String result = adminService.deleteUser(targetId, callerId);

        assertEquals("DELETED", result);
        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection).setAutoCommit(true);
    }

    @Test
    public void testDeleteUser_AdminDeletesSelf_ThrowsUnauthorizedActionException() throws ValidationException, AuthenticationException {
        User admin = new User("admin", "admin@example.com", "password", UserRole.ADMIN);
        String callerId = admin.getId().toString();
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThrows(UnauthorizedActionException.class, () -> adminService.deleteUser(callerId, callerId));
    }

    @Test
    public void testDeleteUser_AdminDeletesAnotherAdmin_ThrowsUnauthorizedActionException() throws ValidationException, AuthenticationException {
        User admin = new User("admin", "admin@example.com", "password", UserRole.ADMIN);
        User targetAdmin = new User("admin2", "admin2@example.com", "password", UserRole.ADMIN);
        String callerId = admin.getId().toString();
        String targetId = targetAdmin.getId().toString();
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(targetAdmin.getId())).thenReturn(Optional.of(targetAdmin));

        assertThrows(UnauthorizedActionException.class, () -> adminService.deleteUser(targetId, callerId));
    }

    @Test
    public void testDeleteUser_TargetNotFound_ThrowsValidationException() throws ValidationException, AuthenticationException {
        User admin = new User("admin", "admin@example.com", "password", UserRole.ADMIN);
        String callerId = admin.getId().toString();
        String targetId = UUID.randomUUID().toString();
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(UUID.fromString(targetId))).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> adminService.deleteUser(targetId, callerId));
    }

    @Test
    public void testCancelAuction_AdminCancels_ReturnsCanceled() throws ValidationException, AuthenticationException, UnauthorizedActionException {
        User admin = new User("admin", "admin@example.com", "password", UserRole.ADMIN);
        String callerId = admin.getId().toString();
        String auctionId = UUID.randomUUID().toString();
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(adminAuctionGateway.cancelAuctionById(auctionId)).thenReturn(true);

        String result = adminService.cancelAuction(auctionId, callerId);

        assertEquals("CANCELED", result);
    }

    @Test
    public void testCancelAuction_NonAdmin_ThrowsUnauthorizedActionException() {
        User user = new User("user", "user@example.com", "password", UserRole.USER);
        String callerId = user.getId().toString();
        String auctionId = UUID.randomUUID().toString();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedActionException.class, () -> adminService.cancelAuction(auctionId, callerId));
    }
}