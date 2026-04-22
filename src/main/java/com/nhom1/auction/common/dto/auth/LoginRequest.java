package com.nhom1.auction.common.dto.auth;

public class LoginRequest {
    private String identifier; //can be either email or username
    private String password;

    public LoginRequest(){};

    public LoginRequest(String identifier, String password){
        this.identifier = identifier;
        this.password = password;
    }


    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}