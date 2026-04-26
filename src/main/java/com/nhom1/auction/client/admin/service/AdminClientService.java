package com.nhom1.auction.client.admin.service;

import com.nhom1.auction.client.user.service.BaseClientService;
import com.nhom1.auction.common.dto.admin.AdminListAuctionsRequest;
import com.nhom1.auction.common.dto.admin.AdminListUsersRequest;
import com.nhom1.auction.common.dto.admin.AdminAuctionListResponse;
import com.nhom1.auction.common.dto.admin.AdminDeleteUserRequest;
import com.nhom1.auction.common.dto.admin.AdminUserListResponse;
import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.enums.UserRole;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.utils.AppContext;
import java.util.concurrent.CompletableFuture;

public class AdminClientService extends BaseClientService {

    public CompletableFuture<AdminUserListResponse> listUsers() {
        AuthResponse currentUser = requireAdminUser();
        if (currentUser == null) {
            return validationError("You must sign in as admin before viewing users.");
        }

        return send(
                new RequestMessage<>(MessageType.ADMIN_LIST_USERS, new AdminListUsersRequest(currentUser.getUserID())),
                AdminUserListResponse.class);
    }

    public CompletableFuture<AdminAuctionListResponse> listAllAuctions() {
        AuthResponse currentUser = requireAdminUser();
        if (currentUser == null) {
            return validationError("You must sign in as admin before viewing auctions.");
        }

        return send(
                new RequestMessage<>(MessageType.ADMIN_LIST_AUCTIONS, new AdminListAuctionsRequest(currentUser.getUserID())),
                AdminAuctionListResponse.class);
    }

    public CompletableFuture<String> deleteUser(String targetUserId) {
        if (targetUserId == null || targetUserId.isBlank()) {
            return validationError("Target user ID is required.");
        }

        AuthResponse currentUser = requireAdminUser();
        if (currentUser == null) {
            return validationError("You must sign in as admin before deleting users.");
        }

        RequestMessage<AdminDeleteUserRequest> request = new RequestMessage<>(
                MessageType.ADMIN_DELETE_USER,
                new AdminDeleteUserRequest(targetUserId, currentUser.getUserID()));

        return send(request, String.class);
    }

    private AuthResponse requireAdminUser() {
        AuthResponse currentUser = AppContext.getCurrentUser();
        if (currentUser == null || currentUser.getUserID() == null || currentUser.getUserID().isBlank()) {
            return null;
        }
        if (currentUser.getRole() != UserRole.ADMIN) {
            return null;
        }
        return currentUser;
    }
}
