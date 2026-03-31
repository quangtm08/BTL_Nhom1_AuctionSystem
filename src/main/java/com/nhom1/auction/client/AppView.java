package com.nhom1.auction.client;

public enum AppView {

    SIGN_IN("/views/sign_in.fxml", null),
    MAIN_DASHBOARD("/views/main_dashboard_explore.fxml",null),
    LOADING("/views/loading.fxml", null),
    REGISTER("/views/register.fxml",null);

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