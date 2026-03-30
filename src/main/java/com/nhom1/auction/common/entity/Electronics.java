package com.nhom1.auction.common.entity;

import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import java.time.LocalDateTime;

public class Electronics extends Item {
    private String brand;
    private int warrantyMonths;

    public Electronics(String id, String name, String description, double startingPrice,
        LocalDateTime startTime, LocalDateTime endTime,
        ItemCategory category, ItemCondition condition,
        String brand, int warrantyMonths) {
        super(id, name, description, startingPrice, startTime, endTime, category, condition);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public void printInfo() {
        System.out.println("[Electronics] " + name + " | Brand: " + brand + " | Warranty: " + warrantyMonths + " months");
        System.out.println("Current Price: $" + currentHighestBid);
    }
}