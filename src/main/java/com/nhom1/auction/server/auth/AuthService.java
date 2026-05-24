package com.nhom1.auction.server.auth;

import com.nhom1.auction.common.entity.User;
import com.nhom1.auction.common.enums.UserRole;
import com.nhom1.auction.common.exception.AuthenticationException;
import com.nhom1.auction.common.exception.UserAlreadyExistsException;
import com.nhom1.auction.server.infrastructure.NotificationService;
import java.util.List;

public class AuthService {
  private final UserRepository userRepository;
  private final NotificationService notificationService;

  public AuthService(UserRepository userRepository, NotificationService notificationService) {
    this.userRepository = userRepository;
    this.notificationService = notificationService;
  }

  // Business operations
  public User login(String identifier, String password) {
    return userRepository
        .findByIdentifier(identifier)
        .filter(user -> user.getPassword().equals(password))
        .orElseThrow(() -> new AuthenticationException("Wrong email/username or password"));
  }

  public User register(String username, String email, String password) {
    if (userRepository.existsByEmail(email)) {
      throw new UserAlreadyExistsException("Email already exists");
    }
    if (userRepository.existsByUsername(username)) {
      throw new UserAlreadyExistsException("Username already exists");
    }

    User newUser = new User(username, email, password, UserRole.USER);
    userRepository.save(newUser);

    // Broadcast user created to all connected admin clients
    notificationService.broadcastUserCreated(newUser.getId().toString(), username, email);

    return newUser;
  }

  // Query operations
  public List<User> getAllUsers() {
    return userRepository.findAll();
  }
}
