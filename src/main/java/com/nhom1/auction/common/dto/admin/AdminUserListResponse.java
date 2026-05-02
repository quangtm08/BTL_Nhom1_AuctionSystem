package com.nhom1.auction.common.dto.admin;

import java.util.List;

public class AdminUserListResponse {
    private List<UserSummaryDto> users;

    public AdminUserListResponse() {}

    public AdminUserListResponse(List<UserSummaryDto> users) {
        this.users = users;
    }

    public List<UserSummaryDto> getUsers() {
        return users;
    }

    public void setUsers(List<UserSummaryDto> users) {
        this.users = users;
    }
}
