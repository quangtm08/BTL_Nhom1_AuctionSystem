package com.nhom1.auction.server;

public enum AppView {

    LOADING("/views/loading.fxml", null),
    SIGN_IN("/views/sign_in.fxml", null),
    REGISTER("/views/register.fxml",null),
    MAIN_DASHBOARD("/views/admin/main_dashboard.fxml" , null),
    MAIN_DASHBOARD_USER_MANAGEMENT("/views/admin/main_dashboard_user_management.fxml" , null);

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