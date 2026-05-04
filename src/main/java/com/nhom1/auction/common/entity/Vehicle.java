package com.nhom1.auction.common.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.enums.VehicleFuelType;

public class Vehicle extends Item {
    private String brand;
    private Integer productionYear;
    private VehicleFuelType fuelType;

    public Vehicle(String name, String description,
        ItemCategory category, ItemCondition condition,
        String brand, Integer productionYear, VehicleFuelType fuelType) {
        super( name, description, category, condition);
        this.brand = brand;
        this.productionYear = productionYear;
        this.fuelType = fuelType;
    }

    /**
     * Use this constructor for loading an EXISTING vehicle item from the database.
     */
    public Vehicle(UUID id, String name, String description,
                   ItemCategory category, ItemCondition condition,
                   String brand, Integer productionYear, VehicleFuelType fuelType,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, name, description, category, condition, createdAt, updatedAt);
        this.brand = brand;
        this.productionYear = productionYear;
        this.fuelType = fuelType;
    }
    //Getter
    public String getBrand(){ return brand; }
    public Integer getProductionYear(){ return productionYear; }
    public VehicleFuelType getFuelType(){ return fuelType; }
    //Setter
    public void setBrand(String brand){ this.brand=brand; }
    public void setProductionYear(Integer productionYear){ this.productionYear=productionYear; }
    public void setFuelType(VehicleFuelType fuelType ){ this.fuelType=fuelType; }

    @Override
    public void printInfo() {

        System.out.println("[Vehicle] " + name + " | Brand: " + brand + " | Year: " + productionYear + " | Fuel: " + fuelType);
    
    }
}
