package com.nhom1.auction.server.admin;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.nhom1.auction.common.dto.admin.AdminAuctionListResponse;
import com.nhom1.auction.common.dto.admin.AdminUserListResponse;
import com.nhom1.auction.common.dto.admin.UserSummaryDto;
import com.nhom1.auction.common.entity.User;
import com.nhom1.auction.common.enums.UserRole;
import com.nhom1.auction.common.exception.AuthenticationException;
import com.nhom1.auction.common.exception.InvalidAuctionStateException;
import com.nhom1.auction.common.exception.UnauthorizedActionException;
import com.nhom1.auction.common.exception.ValidationException;
import com.nhom1.auction.server.auction.AuctionRepository;
import com.nhom1.auction.server.auction.ItemRepository;
import com.nhom1.auction.server.auth.UserRepository;
import com.nhom1.auction.server.bidding.BidRepository;

public class AdminService {
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final ItemRepository itemRepository;
    private final BidRepository bidRepository;
    private final AdminAuctionGateway adminAuctionGateway;
    private final Connection connection;

    public AdminService(
            UserRepository userRepository,
            AuctionRepository auctionRepository,
            ItemRepository itemRepository,
            BidRepository bidRepository,
            AdminAuctionGateway adminAuctionGateway,
            Connection connection) {
        this.userRepository = userRepository;
        this.auctionRepository = auctionRepository;
        this.itemRepository = itemRepository;
        this.bidRepository = bidRepository;
        this.adminAuctionGateway = adminAuctionGateway;
        this.connection = connection;
    }

    public AdminUserListResponse getAllUsers(String callerId)
            throws ValidationException, AuthenticationException, UnauthorizedActionException {
        requireAdmin(callerId);
        List<UserSummaryDto> users = userRepository.findAll().stream()
                .map(this::toUserSummaryDto)
                .toList();
        return new AdminUserListResponse(users);
    }

    public String deleteUser(String targetUserId, String callerId)
            throws ValidationException, AuthenticationException, UnauthorizedActionException {
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new ValidationException("Target user ID is required.");
        }

        User caller = requireAdmin(callerId);
        if (caller.getId().toString().equals(targetUserId)) {
            throw new UnauthorizedActionException("Admin accounts cannot delete themselves from this flow.");
        }

        User target = userRepository.findById(parseUserId(targetUserId, "Target user ID"))
                .orElseThrow(() -> new ValidationException("Target user not found."));
        if (target.getRole() == UserRole.ADMIN) {
            throw new UnauthorizedActionException("Admin accounts cannot be deleted from this flow.");
        }

        try {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                auctionRepository.clearHighestBidderByUserId(target.getId());
                bidRepository.deleteByBidderId(target.getId());

                List<com.nhom1.auction.common.entity.Auction> sellerAuctions = auctionRepository.findBySellerId(target.getId());
                for (com.nhom1.auction.common.entity.Auction auction : sellerAuctions) {
                    bidRepository.deleteByAuctionId(auction.getId());
                    int deletedAuctions = auctionRepository.deleteById(auction.getId());
                    int deletedItems = itemRepository.deleteById(auction.getItemId());
                    if (deletedAuctions == 0 || deletedItems == 0) {
                        throw new ValidationException("Failed to delete auction or item for user.");
                    }
                }

                boolean deleted = userRepository.deleteById(target.getId());
                if (!deleted) {
                    throw new ValidationException("Failed to delete target user.");
                }
                connection.commit();
            } catch (ValidationException ve) {
                connection.rollback();
                throw ve;
            } catch (Exception re) {
                connection.rollback();
                throw new ValidationException("User deletion failed: " + re.getMessage());
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        } catch (SQLException e) {
            throw new ValidationException("User deletion failed: " + e.getMessage());
        }
        return "DELETED";
    }

    public String cancelAuction(String auctionId, String callerId)
            throws ValidationException, AuthenticationException, UnauthorizedActionException, InvalidAuctionStateException {
        requireAdmin(callerId);
        if (auctionId == null || auctionId.isBlank()) {
            throw new ValidationException("Auction ID is required.");
        }
        boolean changed = adminAuctionGateway.cancelAuctionById(auctionId);
        if (!changed) {
            throw new InvalidAuctionStateException("Auction not found or cannot be canceled in current status.");
        }
        return "CANCELED";
    }

    public AdminAuctionListResponse getAllAuctions(String callerId)
            throws ValidationException, AuthenticationException, UnauthorizedActionException {
        requireAdmin(callerId);
        return new AdminAuctionListResponse(adminAuctionGateway.findAllAuctionSummaries());
    }

    private User requireAdmin(String callerId)
            throws ValidationException, AuthenticationException, UnauthorizedActionException {
        if (callerId == null || callerId.isBlank()) {
            throw new ValidationException("Caller ID is required.");
        }

        User caller = userRepository.findById(parseUserId(callerId, "Caller ID"))
                .orElseThrow(() -> new AuthenticationException("Caller not found."));
        if (caller.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedActionException("Only ADMIN can access this admin flow.");
        }
        return caller;
    }

    private UUID parseUserId(String rawId, String fieldName) throws ValidationException {
        try {
            return UUID.fromString(rawId);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(fieldName + " is invalid.");
        }
    }

    private UserSummaryDto toUserSummaryDto(User user) {
        return new UserSummaryDto(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now());
    }
}
