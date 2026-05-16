package com.nhom1.auction.server.admin;

import com.nhom1.auction.common.dto.admin.AdminAuctionListResponse;
import com.nhom1.auction.common.dto.admin.AdminCancelAuctionRequest;
import com.nhom1.auction.common.dto.admin.AdminDeleteUserRequest;
import com.nhom1.auction.common.dto.admin.AdminListAuctionsRequest;
import com.nhom1.auction.common.dto.admin.AdminListUsersRequest;
import com.nhom1.auction.common.dto.admin.AdminUserListResponse;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.common.utils.JsonUtil;
import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.ResponseFactory;

public class AdminHandler {
    private final AdminService adminService;

    public AdminHandler(AdminService adminService) {
        this.adminService = adminService;
    }

    public void register(MessageRouter router) {
        router.register(MessageType.ADMIN_LIST_USERS, (requestId, payloadJson) -> {
            try {
                return handleListUsers(requestId, JsonUtil.fromJson(payloadJson, AdminListUsersRequest.class));
            } catch (Exception e) {
                return ResponseFactory.invalidFormat(requestId, "Invalid admin list users JSON");
            }
        });

        router.register(MessageType.ADMIN_LIST_AUCTIONS, (requestId, payloadJson) -> {
            try {
                return handleListAuctions(requestId, JsonUtil.fromJson(payloadJson, AdminListAuctionsRequest.class));
            } catch (Exception e) {
                return ResponseFactory.invalidFormat(requestId, "Invalid admin list auctions JSON");
            }
        });

        router.register(MessageType.ADMIN_DELETE_USER, (requestId, payloadJson) -> {
            try {
                return handleDeleteUser(requestId, JsonUtil.fromJson(payloadJson, AdminDeleteUserRequest.class));
            } catch (Exception e) {
                return ResponseFactory.invalidFormat(requestId, "Invalid delete user JSON");
            }
        });

        router.register(MessageType.ADMIN_CANCEL_AUCTION, (requestId, payloadJson) -> {
            try {
                return handleCancelAuction(requestId, JsonUtil.fromJson(payloadJson, AdminCancelAuctionRequest.class));
            } catch (Exception e) {
                return ResponseFactory.invalidFormat(requestId, "Invalid cancel auction JSON");
            }
        });
    }

    private ResponseMessage<String> handleDeleteUser(String requestId, AdminDeleteUserRequest dto) {
        try {
            if (dto == null) {
                return ResponseFactory.invalidFormat(requestId, "Missing delete user payload.");
            }
            return ResponseFactory.success(requestId, adminService.deleteUser(dto.getTargetUserId(), dto.getCallerId()));
        } catch (Exception e) {
            return ResponseFactory.fromException(requestId, e);
        }
    }

    private ResponseMessage<String> handleCancelAuction(String requestId, AdminCancelAuctionRequest dto) {
        try {
            if (dto == null) {
                return ResponseFactory.invalidFormat(requestId, "Missing cancel auction payload.");
            }
            return ResponseFactory.success(requestId, adminService.cancelAuction(dto.getAuctionId(), dto.getCallerId()));
        } catch (Exception e) {
            return ResponseFactory.fromException(requestId, e);
        }
    }

    private ResponseMessage<AdminUserListResponse> handleListUsers(String requestId, AdminListUsersRequest dto) {
        try {
            if (dto == null) {
                return ResponseFactory.invalidFormat(requestId, "Missing admin list users payload.");
            }
            return ResponseFactory.success(requestId, adminService.getAllUsers(dto.getCallerId()));
        } catch (Exception e) {
            return ResponseFactory.fromException(requestId, e);
        }
    }

    private ResponseMessage<AdminAuctionListResponse> handleListAuctions(String requestId, AdminListAuctionsRequest dto) {
        try {
            if (dto == null) {
                return ResponseFactory.invalidFormat(requestId, "Missing admin list auctions payload.");
            }
            return ResponseFactory.success(requestId, adminService.getAllAuctions(dto.getCallerId()));
        } catch (Exception e) {
            return ResponseFactory.fromException(requestId, e);
        }
    }
}
