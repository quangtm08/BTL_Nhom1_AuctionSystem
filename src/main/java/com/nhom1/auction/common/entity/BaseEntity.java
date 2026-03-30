package com.nhom1.auction.common.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class BaseEntity {
    private UUID id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    //Constructor
    public BaseEntity(){
        id = UUID.randomUUID();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }


    //getters
    //final so that subclasses don't need to override
    public final UUID getId() {
        return id;
    }

    public final LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public final LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /*
    1. id and createdAt are fixed fields => no setters.
    2. touchUpdatedAt only updates updatedAt when a change is made
    => prevent creating fake updatedAt
     */
    public final void touchUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
}
