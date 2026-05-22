package com.nhom1.auction.server.admin;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import com.nhom1.auction.common.dto.admin.AdminAuctionListResponse;
import com.nhom1.auction.common.dto.admin.AdminUserListResponse;
import com.nhom1.auction.common.dto.admin.UserSummaryDto;
import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.entity.User;
import com.nhom1.auction.common.enums.UserRole;
import com.nhom1.auction.common.exception.AppException;
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

public class AdminService {

    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final ItemRepository itemRepository;
    private final BidRepository bidRepository;
    private final AdminAuctionGateway adminAuctionGateway;
    private final NotificationService notificationService;
    private final DataSource dataSource;

    public AdminService(
        UserRepository userRepository,
        AuctionRepository auctionRepository,
        ItemRepository itemRepository,
        BidRepository bidRepository,
        AdminAuctionGateway adminAuctionGateway,
        NotificationService notificationService,
        DataSource dataSource
    ) {
        this.userRepository = userRepository;
        this.auctionRepository = auctionRepository;
        this.itemRepository = itemRepository;
        this.bidRepository = bidRepository;
        this.adminAuctionGateway = adminAuctionGateway;
        this.notificationService = notificationService;
        this.dataSource = dataSource;
    }

    public AdminUserListResponse getAllUsers(String callerId) {
        requireAdmin(callerId);
        List<UserSummaryDto> users = userRepository
            .findAll()
            .stream()
            .map(this::toUserSummaryDto)
            .toList();
        return new AdminUserListResponse(users);
    }

    public String deleteUser(String targetUserId, String callerId) {
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new ValidationException("Target user ID is required.");
        }

        User caller = requireAdmin(callerId);
        if (caller.getId().toString().equals(targetUserId)) {
            throw new UnauthorizedActionException(
                "Admin accounts cannot delete themselves from this flow."
            );
        }

        User target = userRepository
            .findById(parseUserId(targetUserId, "Target user ID"))
            .orElseThrow(() -> new NotFoundException("Target user not found."));
        if (target.getRole() == UserRole.ADMIN) {
            throw new UnauthorizedActionException(
                "Admin accounts cannot be deleted from this flow."
            );
        }

        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                auctionRepository.clearHighestBidderByUserId(
                    target.getId(),
                    connection
                );
                bidRepository.deleteByBidderId(target.getId(), connection);

                List<Auction> sellerAuctions =
                    auctionRepository.findBySellerId(target.getId(), connection);
                for (Auction auction : sellerAuctions) {
                    bidRepository.deleteByAuctionId(auction.getId(), connection);
                    int deletedAuctions = auctionRepository.deleteById(
                        auction.getId(),
                        connection
                    );
                    int deletedItems = itemRepository.deleteById(
                        auction.getItemId(),
                        connection
                    );
                    if (deletedAuctions == 0 || deletedItems == 0) {
                        throw new IllegalStateException(
                            "Failed to delete auction or item for user."
                        );
                    }
                }

                boolean deleted = userRepository.deleteById(target.getId(), connection);
                if (!deleted) {
                    throw new IllegalStateException(
                        "Failed to delete target user."
                    );
                }
                connection.commit();
                notificationService.broadcastUserDeleted(targetUserId);
            } catch (AppException e) {
                connection.rollback();
                throw e;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                "User deletion failed due to database error",
                e
            );
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("User deletion failed", e);
        }

        return "DELETED";
    }

    public String cancelAuction(String auctionId, String callerId) {
        requireAdmin(callerId);
        if (auctionId == null || auctionId.isBlank()) {
            throw new ValidationException("Auction ID is required.");
        }
        parseUserId(auctionId, "Auction ID");
        boolean changed = adminAuctionGateway.cancelAuctionById(auctionId);
        if (!changed) {
            throw new InvalidAuctionStateException(
                "Auction not found or cannot be canceled in current status."
            );
        }
        notificationService.broadcastAuctionEnded(java.util.UUID.fromString(auctionId), null, null);
        return "CANCELED";
    }

    public AdminAuctionListResponse getAllAuctions(String callerId) {
        requireAdmin(callerId);
        return new AdminAuctionListResponse(
            adminAuctionGateway.findAllAuctionSummaries()
        );
    }

    public String approveAuction(String auctionId, String callerId, String openingDateStr) {
        requireAdmin(callerId);
        if (auctionId == null || auctionId.isBlank()) throw new ValidationException("Auction ID is required.");
        UUID parsedAuctionId;
        try { parsedAuctionId = UUID.fromString(auctionId); } catch (IllegalArgumentException ex) { throw new ValidationException("auctionId is not a valid UUID"); }

        Auction auction = auctionRepository.findById(parsedAuctionId).orElseThrow(() -> new NotFoundException("Auction not found"));
        if (auction.getStatus() != com.nhom1.auction.common.enums.AuctionStatus.PENDING) {
            throw new InvalidAuctionStateException("Only PENDING auctions can be approved");
        }

        LocalDateTime scheduledStart = auction.getStartTime();
        if (openingDateStr != null && !openingDateStr.isBlank()) {
            try {
                scheduledStart = LocalDate.parse(openingDateStr).atStartOfDay();
            } catch (Exception ex) {
                throw new ValidationException("Opening date is not a valid date");
            }
        }
        if (scheduledStart == null) {
            throw new ValidationException("Opening date is required for approval");
        }
        if (!scheduledStart.isAfter(LocalDateTime.now())) {
            throw new ValidationException("Opening date must be in the future");
        }

        Integer duration = auction.getDurationDays();
        if (duration == null || duration <= 0) duration = 7;
        LocalDateTime start = scheduledStart;
        LocalDateTime end = start.plusDays(duration);

        try (Connection connection = dataSource.getConnection()) {
            boolean oldAuto = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                boolean updated = auctionRepository.updateStartEndAndStatus(parsedAuctionId, start, end, com.nhom1.auction.common.enums.AuctionStatus.OPEN, connection);
                if (!updated) throw new IllegalStateException("Auction not found or not pending");

                connection.commit();
            } catch (AppException ex) {
                connection.rollback();
                throw ex;
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(oldAuto);
            }
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Approve auction failed", ex);
        }

        // Broadcast new auction so clients can show it in explore
        try {
            String itemName = itemRepository.findById(auction.getItemId()).map(i -> i.getName()).orElse("Unknown");
            notificationService.broadcastNewAuction(auctionId, itemName, auction.getStartingPrice());
        } catch (Exception ignored) {}

        return "APPROVED";
    }

    private User requireAdmin(String callerId) {
        if (callerId == null || callerId.isBlank()) {
            throw new ValidationException("Caller ID is required.");
        }

        User caller = userRepository
            .findById(parseUserId(callerId, "Caller ID"))
            .orElseThrow(() ->
                new AuthenticationException("Caller not found.")
            );
        if (caller.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedActionException(
                "Only ADMIN can access this admin flow."
            );
        }
        return caller;
    }

    private UUID parseUserId(String rawId, String fieldName) {
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
            user.getCreatedAt() != null
                ? user.getCreatedAt()
                : LocalDateTime.now()
        );
    }
}
