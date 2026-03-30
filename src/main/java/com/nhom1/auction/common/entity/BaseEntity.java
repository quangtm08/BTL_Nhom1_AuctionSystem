package com.nhom1.auction.common.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class BaseEntity {
    private UUID id;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    //Constructor
    public BaseEntity(){
        id = UUID.randomUUID();
        createdAt = LocalDateTime.now();
        modifiedAt = LocalDateTime.now();
    }


    //getters
    //final so that subclasses don't need to override
    public final UUID getId() {
        return id;
    }

    public final LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public final LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    /*
    id and createdAt are fixed
    touchModify to only update the modifiedAt when a change is made
    => prevent creating fake modifiedAt

    final so that subclasses don't need to override
     */

    public final void touchModify(LocalDateTime modifiedAt) {
        this.modifiedAt = LocalDateTime.now();
    }
}
