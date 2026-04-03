package com.nhom1.auction.common.entity;

import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;

public class Electronics extends Item {
    private String brand;
    private int warrantyMonths;

    public Electronics(String name, String description, 
        ItemCategory category, ItemCondition condition,
        String brand, int warrantyMonths) {
        super(name, description, category, condition);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }
    //Getter
    public String getBrand(){ return brand; }
    public int getWarrantyMonths(){ return warrantyMonths; }
    //Setter
    public void setBrand(){ this.brand=brand; }
    public void setWarrantyMonths(){ this.warrantyMonths = warrantyMonths; }

    
    @Override
    public void printInfo() {
        System.out.println("[Electronics] " + name + " | Brand: " + brand + " | Warranty: " + warrantyMonths + " months");
    }
}
