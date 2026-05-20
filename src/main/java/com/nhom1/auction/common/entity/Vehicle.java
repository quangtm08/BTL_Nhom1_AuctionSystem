package com.nhom1.auction.common.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;

public class Vehicle extends Item {

    public Vehicle(String name, String description,
        ItemCategory category, ItemCondition condition) {
        super( name, description, category, condition);
    }

    /**
     * Use this constructor for loading an EXISTING vehicle item from the database.
     */
    public Vehicle(UUID id, String name, String description,
                   ItemCategory category, ItemCondition condition,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, name, description, category, condition, createdAt, updatedAt);
    }

    @Override
    public void printInfo() {

        System.out.println("[Vehicle] " + name);
    
    }
}
