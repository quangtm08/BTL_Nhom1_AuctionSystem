package com.nhom1.auction.client;

public enum AppView {

    LOADING("/views/loading.fxml", null),
    SIGN_IN("/views/sign_in.fxml", null),
    REGISTER("/views/register.fxml", null),
    AUCTION_BROWSE("/views/user/auction_browse.fxml", null),
    MY_LISTINGS("/views/user/my_listings.fxml", null),
    CREATE_LISTING("/views/user/create_auction.fxml", null),
    EDIT_LISTING("/views/user/edit_auction.fxml", null),
    MY_BIDS("/views/user/my_bids.fxml", null),
    PAYMENT("/views/user/payment.fxml", null),

    ADMIN_OVERVIEW("/views/admin/admin_overview.fxml", null),
    USER_MANAGEMENT("/views/admin/user_management.fxml", null),
    AUCTION_MANAGEMENT("/views/admin/auction_management.fxml", null),
    AUCTION_DETAIL("/views/user/auction_detail.fxml", null),
    BID_HISTORY_CHART("/views/user/bid_history_chart.fxml", null);

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
