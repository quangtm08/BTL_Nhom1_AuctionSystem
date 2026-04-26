package com.nhom1.auction.server.admin;

import com.nhom1.auction.common.dto.admin.AdminListAuctionsRequest;
import com.nhom1.auction.common.dto.admin.AdminDeleteUserRequest;
import com.nhom1.auction.common.dto.admin.AdminListUsersRequest;
import com.nhom1.auction.common.dto.admin.AdminAuctionListResponse;
import com.nhom1.auction.common.dto.admin.AdminUserListResponse;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.common.utils.JsonUtil;
import com.nhom1.auction.server.infrastructure.MessageRouter;

public class AdminHandler {
    private final AdminService adminService;

    public AdminHandler(AdminService adminService) {
        this.adminService = adminService;
    }

    public void register(MessageRouter router) {
        router.register(MessageType.ADMIN_LIST_USERS, (requestId, payloadJson) -> {
            try {
                AdminListUsersRequest dto = JsonUtil.fromJson(payloadJson, AdminListUsersRequest.class);
                return handleListUsers(requestId, dto);
            } catch (Exception e) {
                return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Invalid admin list users JSON");
            }
        });

        router.register(MessageType.ADMIN_LIST_AUCTIONS, (requestId, payloadJson) -> {
            try {
                AdminListAuctionsRequest dto = JsonUtil.fromJson(payloadJson, AdminListAuctionsRequest.class);
                return handleListAuctions(requestId, dto);
            } catch (Exception e) {
                return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Invalid admin list auctions JSON");
            }
        });

        router.register(MessageType.ADMIN_DELETE_USER, (requestId, payloadJson) -> {
            try {
                AdminDeleteUserRequest dto = JsonUtil.fromJson(payloadJson, AdminDeleteUserRequest.class);
                return handleDeleteUser(requestId, dto);
            } catch (Exception e) {
                return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Invalid delete user JSON");
            }
        });
    }

    private ResponseMessage<String> handleDeleteUser(String requestId, AdminDeleteUserRequest dto) {
        try {
            if (dto == null) {
                return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Missing delete user payload.");
            }
            String result = adminService.deleteUser(dto.getTargetUserId(), dto.getCallerId());
            return new ResponseMessage<>(requestId, result);
        } catch (IllegalArgumentException e) {
            return new ResponseMessage<>(requestId, "INVALID_ID", "Caller ID or target user ID is invalid.");
        } catch (Exception e) {
            return new ResponseMessage<>(requestId, "ADMIN_ACTION_FAILED", e.getMessage());
        }
    }

    private ResponseMessage<AdminUserListResponse> handleListUsers(String requestId, AdminListUsersRequest dto) {
        try {
            if (dto == null) {
                return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Missing admin list users payload.");
            }
            return new ResponseMessage<>(requestId, adminService.getAllUsers(dto.getCallerId()));
        } catch (IllegalArgumentException e) {
            return new ResponseMessage<>(requestId, "INVALID_ID", "Caller ID is invalid.");
        } catch (Exception e) {
            return new ResponseMessage<>(requestId, "ADMIN_ACTION_FAILED", e.getMessage());
        }
    }

    private ResponseMessage<AdminAuctionListResponse> handleListAuctions(String requestId, AdminListAuctionsRequest dto) {
        try {
            if (dto == null) {
                return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Missing admin list auctions payload.");
            }
            return new ResponseMessage<>(requestId, adminService.getAllAuctions(dto.getCallerId()));
        } catch (IllegalArgumentException e) {
            return new ResponseMessage<>(requestId, "INVALID_ID", "Caller ID is invalid.");
        } catch (Exception e) {
            return new ResponseMessage<>(requestId, "ADMIN_ACTION_FAILED", e.getMessage());
        }
    }
}
