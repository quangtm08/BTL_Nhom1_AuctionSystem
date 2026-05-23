package com.nhom1.auction.common.entity;

import com.nhom1.auction.common.enums.UserRole;
import java.time.LocalDateTime;
import java.util.UUID;

public class User extends BaseEntity {
  private String username;
  private String email;
  private UserRole role;
  private String password;

  /*
   * Use this constructor for creating a BRAND NEW user (e.g., during Registration).
   * BaseEntity will automatically generate a new UUID and timestamps.
   */
  public User(String username, String email, String password, UserRole role) {
    super();
    this.username = username;
    this.email = email;
    this.password = password;
    this.role = role;
  }

  /*
   * Use this constructor for loading an EXISTING user from the database.
   * It preserves the original ID and timestamps.
   */
  public User(
      UUID id,
      String username,
      String email,
      String password,
      UserRole role,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    super(id, createdAt, updatedAt);
    this.username = username;
    this.email = email;
    this.password = password;
    this.role = role;
  }

  // Default constructor for JSON/Reflection
  public User() {}

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public UserRole getRole() {
    return role;
  }

  public void setRole(UserRole role) {
    this.role = role;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
