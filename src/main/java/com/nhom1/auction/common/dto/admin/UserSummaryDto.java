package com.nhom1.auction.common.dto.admin;

import com.nhom1.auction.common.enums.UserRole;
import java.time.LocalDateTime;

public class UserSummaryDto {
    private String id;
    private String username;
    private String email;
    private UserRole role;
    private LocalDateTime createdAt;

    public UserSummaryDto() {}

    public UserSummaryDto(String id, String username, String email, UserRole role, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
