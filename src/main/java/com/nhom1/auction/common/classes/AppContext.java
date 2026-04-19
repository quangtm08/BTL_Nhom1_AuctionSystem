package com.nhom1.auction.common.classes;

public class AppContext {
    private static boolean isServer;

    public static void setServer(boolean value) {
        isServer = value;
    }

    public static boolean isServer() {
        return isServer;
    }
}