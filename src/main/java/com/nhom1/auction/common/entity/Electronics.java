package com.nhom1.auction.common.entity;

import java.time.LocalDateTime;
import java.util.UUID;

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

    /**
     * Use this constructor for loading an EXISTING electronics item from the database.
     */
    public Electronics(UUID id, String name, String description,
                       ItemCategory category, ItemCondition condition,
                       String brand, int warrantyMonths,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, name, description, category, condition, createdAt, updatedAt);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }
    //Getter
    public String getBrand(){ return brand; }
    public int getWarrantyMonths(){ return warrantyMonths; }
    //Setter
    public void setBrand(String brand){ this.brand=brand; }
    public void setWarrantyMonths(int warrantyMonths){ this.warrantyMonths = warrantyMonths; }

    
    @Override
    public void printInfo() {
        System.out.println("[Electronics] " + name + " | Brand: " + brand + " | Warranty: " + warrantyMonths + " months");
    }
}
