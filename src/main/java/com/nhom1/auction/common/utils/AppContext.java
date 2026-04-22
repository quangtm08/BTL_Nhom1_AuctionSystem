package com.nhom1.auction.common.utils;

import com.nhom1.auction.common.dto.auth.AuthResponse;

/*
 - AppContext: Global Session and Configuration storage.
 - It holds data that needs to be shared across multiple UI screens (Controllers),
  (E.g. current logged-in user, server status,..)
 */
public class AppContext {
    private static boolean isServer;
    // Allows the entire Client application to know who is logged in.
    private static AuthResponse currentUser;

    public static void setServer(boolean value) {
        isServer = value;
    }

    public static boolean isServer() {
        return isServer;
    }


     // Stores the authenticated user data after a successful login.
    public static void setCurrentUser(AuthResponse user) {
        currentUser = user;
    }


    public static AuthResponse getCurrentUser() {
        return currentUser;
    }

    // Clears the current session (Logout)
    public static void clearSession() {
        currentUser = null;
    }
}
