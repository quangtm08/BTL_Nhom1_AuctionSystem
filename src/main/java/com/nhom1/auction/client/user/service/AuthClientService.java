package com.nhom1.auction.client.user.service;

import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.dto.auth.LoginRequest;
import com.nhom1.auction.common.dto.auth.RegisterRequest;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.utils.AppContext;
import java.util.concurrent.CompletableFuture;

public class AuthClientService extends BaseClientService {

  public CompletableFuture<AuthResponse> login(String identifier, String password) {
    if (identifier == null || identifier.isBlank()) return validationError("Username is required.");
    if (password == null || password.isBlank()) return validationError("Password is required.");

    RequestMessage<LoginRequest> request =
        new RequestMessage<>(MessageType.LOGIN, new LoginRequest(identifier, password));
    return send(request, AuthResponse.class)
        .thenApply(
            authData -> {
              AppContext.setCurrentUser(authData);
              return authData;
            });
  }

  public CompletableFuture<AuthResponse> register(
      String username, String email, String password, String repeatPassword) {
    if (username == null || username.isBlank()) return validationError("Username is required.");
    if (password == null
        || password.isBlank()
        || repeatPassword == null
        || repeatPassword.isBlank()) return validationError("Password is required.");
    if (password.length() < 8)
      return validationError("Password must be at least 8 characters long");
    if (email == null || email.isBlank()) return validationError("Email is required");
    if (!password.equals(repeatPassword)) return validationError("Passwords do not match.");
    if (!email.contains("@") || !email.contains("."))
      return validationError("Invalid email format");

    RequestMessage<RegisterRequest> request =
        new RequestMessage<>(MessageType.REGISTER, new RegisterRequest(username, email, password));
    return send(request, AuthResponse.class)
        .thenApply(
            authData -> {
              AppContext.setCurrentUser(authData);
              return authData;
            });
  }

  public void logout() {
    AppContext.clearSession();
  }
}
