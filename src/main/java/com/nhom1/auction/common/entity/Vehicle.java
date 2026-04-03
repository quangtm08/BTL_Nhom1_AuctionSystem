package com.nhom1.auction.common.entity;

import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.enums.VehicleFuelType;

public class Vehicle extends Item {
    private String make;
    private int productionYear;
    private VehicleFuelType fuelType;

    public Vehicle(String name, String description,
        ItemCategory category, ItemCondition condition,
        String make, int productionYear, VehicleFuelType fuelType) {
        super( name, description, category, condition);
        this.make = make;
        this.productionYear = productionYear;
        this.fuelType = fuelType;
    }
    //Getter
    public String getMake(){ return make; }
    public int getProductionYear(){ return productionYear; }
    public VehicleFuelType getFuelType(){ return fuelType; }
    //Setter
    public void setMake(String make){ this.make=make; }
    public void setProductionYear(int productionYear){ this.productionYear=productionYear; }
    public void setFuelType(VehicleFuelType fuelType ){ this.fuelType=fuelType; }

    @Override
    public void printInfo() {

        System.out.println("[Vehicle] " + name + " | Make: " + make + " | Year: " + productionYear + " | Fuel: " + fuelType);
    
    }
}
