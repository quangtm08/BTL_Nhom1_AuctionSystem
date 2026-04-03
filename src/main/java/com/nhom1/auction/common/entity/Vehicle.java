package com.nhom1.auction.common.entity;

import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.enums.VehicleFuelType;

public class Vehicle extends Item {
    private String brand;
    private int productionYear;
    private VehicleFuelType fuelType;

    public Vehicle(String name, String description,
        ItemCategory category, ItemCondition condition,
        String brand, int productionYear, VehicleFuelType fuelType) {
        super( name, description, category, condition);
        this.brand = brand;
        this.productionYear = productionYear;
        this.fuelType = fuelType;
    }
    //Getter
    public String getBrand(){ return brand; }
    public int getProductionYear(){ return productionYear; }
    public VehicleFuelType getFuelType(){ return fuelType; }
    //Setter
    public void setBrand(String brand){ this.brand=brand; }
    public void setProductionYear(int productionYear){ this.productionYear=productionYear; }
    public void setFuelType(VehicleFuelType fuelType ){ this.fuelType=fuelType; }

    @Override
    public void printInfo() {

        System.out.println("[Vehicle] " + name + " | Brand: " + brand + " | Year: " + productionYear + " | Fuel: " + fuelType);
    
    }
}
