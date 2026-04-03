package com.nhom1.auction.client;

public enum AppView {

    LOADING("/views/loading.fxml", null),
    SIGN_IN("/views/sign_in.fxml", null),
    REGISTER("/views/register.fxml",null),
    MAIN_DASHBOARD("/views/main_dashboard_explore.fxml",null),
    CHA_CHING("/views/cha_ching.fxml", null);

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