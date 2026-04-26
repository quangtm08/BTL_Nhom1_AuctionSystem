package com.nhom1.auction.common.dto.admin;

import com.nhom1.auction.common.enums.UserRole;

public class UserSummaryDto {
    private String id;
    private String username;
    private String email;
    private UserRole role;

    public UserSummaryDto() {}

    public UserSummaryDto(String id, String username, String email, UserRole role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
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
}
