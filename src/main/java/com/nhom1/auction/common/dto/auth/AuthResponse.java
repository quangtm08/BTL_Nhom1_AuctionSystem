package com.nhom1.auction.common.dto.auth;

import com.nhom1.auction.common.enums.UserRole;

public class AuthResponse {
    private String userID; //use String instead of UUID for safer JSON conversion and compatibility
    private String username;
    private String email;
    private UserRole role;

    public AuthResponse() {};

    public AuthResponse(String userID, String username, String email, UserRole role) {
        this.userID = userID;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
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
