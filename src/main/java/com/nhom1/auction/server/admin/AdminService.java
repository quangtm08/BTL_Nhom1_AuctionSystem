package com.nhom1.auction.server.admin;

import com.nhom1.auction.common.dto.admin.AdminAuctionListResponse;
import com.nhom1.auction.common.dto.admin.AdminUserListResponse;
import com.nhom1.auction.common.dto.admin.UserSummaryDto;
import com.nhom1.auction.common.entity.User;
import com.nhom1.auction.common.enums.UserRole;
import com.nhom1.auction.common.exception.AuthenticationException;
import com.nhom1.auction.common.exception.UnauthorizedActionException;
import com.nhom1.auction.common.exception.ValidationException;
import com.nhom1.auction.server.auth.UserRepository;
import java.util.List;
import java.util.UUID;

public class AdminService {
    private final UserRepository userRepository;
    private final AdminAuctionGateway adminAuctionGateway;

    public AdminService(UserRepository userRepository, AdminAuctionGateway adminAuctionGateway) {
        this.userRepository = userRepository;
        this.adminAuctionGateway = adminAuctionGateway;
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

        // Reuse the same admin gate as the read endpoints so the permission
        // contract stays aligned across list/delete flows.
        User caller = requireAdmin(callerId);

        // Guardrail for shared admin tooling:
        // this branch intentionally blocks self-delete so an admin cannot lock
        // themselves out while other admin-management flows are still incomplete.
        if (callerId.equals(targetUserId)) {
            throw new UnauthorizedActionException("Admin accounts cannot delete themselves from this flow.");
        }

        User target = userRepository.findById(parseUserId(targetUserId, "Target user ID"))
                .orElseThrow(() -> new ValidationException("Target user not found."));
        if (target.getRole() == UserRole.ADMIN) {
            throw new UnauthorizedActionException("Admin accounts cannot be deleted from this flow.");
        }

        userRepository.deleteById(target.getId());
        return "DELETED";
    }

    public AdminAuctionListResponse getAllAuctions(String callerId)
            throws ValidationException, AuthenticationException, UnauthorizedActionException {
        requireAdmin(callerId);
        // Cross-team integration point:
        // auction summaries come from Duy's auction-side mapping, not from the
        // admin module itself.
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
            // Convert low-level UUID parsing failure into the validation
            // contract expected by AdminHandler/client service.
            throw new ValidationException(fieldName + " is invalid.");
        }
    }

    private UserSummaryDto toUserSummaryDto(User user) {
        return new UserSummaryDto(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getRole());
    }
}
