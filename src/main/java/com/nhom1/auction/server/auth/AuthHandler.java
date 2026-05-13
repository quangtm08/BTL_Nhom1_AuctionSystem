package com.nhom1.auction.server.auth;

import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.dto.auth.LoginRequest;
import com.nhom1.auction.common.dto.auth.RegisterRequest;
import com.nhom1.auction.common.entity.User;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.common.utils.JsonUtil;
import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.ResponseFactory;

public class AuthHandler {
    private final AuthService authService;

    public AuthHandler(AuthService authService) {
        this.authService = authService;
    }

    public void register(MessageRouter router) {
        router.register(MessageType.LOGIN, (requestId, payloadJson) -> {
            try {
                LoginRequest dto = JsonUtil.fromJson(payloadJson, LoginRequest.class);
                return handleLogin(requestId, dto);
            } catch (Exception e) {
                return ResponseFactory.invalidFormat(requestId, "Invalid Login JSON");
            }
        });

        router.register(MessageType.REGISTER, (requestId, payloadJson) -> {
            try {
                RegisterRequest dto = JsonUtil.fromJson(payloadJson, RegisterRequest.class);
                return handleRegister(requestId, dto);
            } catch (Exception e) {
                return ResponseFactory.invalidFormat(requestId, "Invalid Register JSON");
            }
        });
    }

    private ResponseMessage<AuthResponse> handleLogin(String requestId, LoginRequest dto) {
        try {
            User user = authService.login(dto.getIdentifier(), dto.getPassword());
            AuthResponse response = new AuthResponse(
                    user.getId().toString(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole()
            );
            return ResponseFactory.success(requestId, response);
        } catch (Exception e) {
            return ResponseFactory.fromException(requestId, e);
        }
    }

    private ResponseMessage<AuthResponse> handleRegister(String requestId, RegisterRequest dto) {
        try {
            User user = authService.register(dto.getUsername(), dto.getEmail(), dto.getPassword());
            AuthResponse response = new AuthResponse(
                    user.getId().toString(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole()
            );
            return ResponseFactory.success(requestId, response);
        } catch (Exception e) {
            return ResponseFactory.fromException(requestId, e);
        }
    }
}
