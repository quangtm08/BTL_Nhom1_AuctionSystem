package com.nhom1.auction.server.auth;

import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.dto.auth.LoginRequest;
import com.nhom1.auction.common.dto.auth.RegisterRequest;
import com.nhom1.auction.common.entity.User;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.common.utils.JsonUtil;
import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.auth.AuthService;

//It translates JSON into DTOs (Login request, Register request) and calls the AuthService to get result
public class AuthHandler {
    private final AuthService authService;

    public AuthHandler(AuthService authService) {
        this.authService = authService;
    }

    //It tells the Router how to handle LOGIN and REGISTER.
    public void register(MessageRouter router) {
        //HANDLE LOGIN
        router.register(MessageType.LOGIN, (requestId, payloadJson) -> {
            try {
                // 1. Unpack the JSON
                LoginRequest dto = JsonUtil.fromJson(payloadJson, LoginRequest.class);
                // 2. Do the work and return the response
                return handleLogin(requestId, dto);
            } catch (Exception e) {
                return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Invalid Login JSON");
            }
        });

        //HANDLE REGISTER
        router.register(MessageType.REGISTER, (requestId, payloadJson) -> {
            try {
                // 1. Unpack the JSON
                RegisterRequest dto = JsonUtil.fromJson(payloadJson, RegisterRequest.class);
                // 2. Do the work and return the response
                return handleRegister(requestId, dto);
            } catch (Exception e) {
                return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Invalid Register JSON");
            }
        });
    }


    //Methods that interact with services. Takes id and request object (DTO), returns response message
    private ResponseMessage<AuthResponse> handleLogin(String requestId, LoginRequest dto) {
        try {
            // Call the Service (Layer 4)
            User user = authService.login(dto.getIdentifier(), dto.getPassword());

            // Pack the result into a safe DTO
            AuthResponse response = new AuthResponse(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
            );

            return new ResponseMessage<>(requestId, response);
        } catch (Exception e) {
            // If the Service throws an error (e.g. Wrong Password), send it back
            return new ResponseMessage<>(requestId, "AUTH_FAILED", e.getMessage());
        }
    }

    private ResponseMessage<AuthResponse> handleRegister(String requestId, RegisterRequest dto) {
        try {
            // Call the Service (Layer 4)
            User user = authService.register(
                dto.getUsername(), 
                dto.getEmail(), 
                dto.getPassword()
            );

            AuthResponse response = new AuthResponse(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
            );

            return new ResponseMessage<>(requestId, response);
        } catch (Exception e) {
            return new ResponseMessage<>(requestId, "REGISTRATION_FAILED", e.getMessage());
        }
    }
}
