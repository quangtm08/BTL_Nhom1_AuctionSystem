package com.nhom1.auction.common.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class BaseEntity {
    private UUID id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BaseEntity(){
        id = UUID.randomUUID();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    public final UUID getId() {
        return id;
    }

    public final LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public final LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Only domain mutations should refresh updatedAt.
    public final void touchUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
}
