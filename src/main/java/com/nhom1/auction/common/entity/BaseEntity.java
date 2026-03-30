package com.nhom1.auction.common.entity;

// Lớp gốc cung cấp khóa chính (ID) cho toàn bộ hệ thống(đẻ ra tất cả mọi thứ)
public abstract class BaseEntity {
    protected String id;

    public BaseEntity(String id) {
        this.id = id;
    }

    public String getId() { return id; }
}