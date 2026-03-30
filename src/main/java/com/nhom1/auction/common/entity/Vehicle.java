package com.nhom1.auction.common.entity;

import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.enums.VehicleFuelType;
import java.time.LocalDateTime;

public class Vehicle extends Item {
    private String make;
    private int productionYear;
    private VehicleFuelType fuelType;

    public Vehicle(String id, String name, String description, double startingPrice,
        LocalDateTime startTime, LocalDateTime endTime,
        ItemCategory category, ItemCondition condition,
        String make, int productionYear, VehicleFuelType fuelType) {
        super(id, name, description, startingPrice, startTime, endTime, category, condition);
        this.make = make;
        this.productionYear = productionYear;
        this.fuelType = fuelType;
    }

    @Override
    public void printInfo() {

        System.out.println("[Vehicle] " + name + " | Make: " + make + " | Year: " + productionYear + " | Fuel: " + fuelType);
        System.out.println("Current Price: $" + currentHighestBid);
    }
}