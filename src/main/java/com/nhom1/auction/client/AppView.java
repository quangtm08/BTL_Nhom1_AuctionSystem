package com.nhom1.auction.client;

public enum AppView {

    LOADING("/views/loading.fxml", null),
    SIGN_IN("/views/sign_in.fxml", null),
    REGISTER("/views/register.fxml",null),
    EXPLORE("/views/user/main_dashboard_explore.fxml",null),
    LISTINGS("/views/user/main_dashboard_listings.fxml",null),
    BIDS("/views/user/main_dashboard_bids.fxml",null),
    PAYMENT("/views/user/main_dashboard_payment.fxml", null),
    MAIN_DASHBOARD("/views/admin/main_dashboard.fxml" , null),
    MAIN_DASHBOARD_USER_MANAGEMENT("/views/admin/main_dashboard_user_management.fxml" , null),
    MAIN_DASHBOARD_AUCTION_MANAGEMENT("/views/admin/main_dashboard_auction_management.fxml" , null),
    LIVE_AUCTION_BID("/views/user/live_auction_bid.fxml" , null);

    private final String fxml;
    private final String css;

    AppView(String fxml, String css) {
        this.fxml = fxml;
        this.css = css;
    }

    public String getFxml() {
        return fxml;
    }

    public String getCss() {
        return css;
    }
}