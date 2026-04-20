package com.nhom1.auction.common.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class BaseEntity {
    private final UUID id;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BaseEntity(){
        id = UUID.randomUUID();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }


    //this constructor is used by repository to create object with already existed id and timestamps
    protected BaseEntity(UUID id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
