package com.nhom1.auction.common.entity;


import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.enums.VehicleFuelType;

public class Vehicle extends Item {
    private final String make;
    private final int productionYear;
    private final VehicleFuelType fuelType;

    public Vehicle( String name, String description,
        ItemCategory category, ItemCondition condition,
        String make, int productionYear, VehicleFuelType fuelType) {
        super(name, description,category, condition);
        this.make = make;
        this.productionYear = productionYear;
        this.fuelType = fuelType;
    }

    @Override
    public void printInfo() {

        System.out.println("[Vehicle] " + name + " | Make: " + make + " | Year: " + productionYear + " | Fuel: " + fuelType);
        
    }
}