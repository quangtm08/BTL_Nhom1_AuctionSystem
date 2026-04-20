package com.nhom1.auction.common.dto.auth;

import com.nhom1.auction.common.enums.UserRole;

public class AuthResponse {
    private String userID; //use String instead of UUID for safer JSON conversion and compatibility
    private String username;
    private String emaill;
    private UserRole role;




    public AuthResponse() {};

    public AuthResponse(String userID, String username, String emaill, UserRole role) {
        this.userID = userID;
        this.username = username;
        this.emaill = emaill;
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

    public String getEmaill() {
        return emaill;
    }

    public void setEmaill(String emaill) {
        this.emaill = emaill;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
