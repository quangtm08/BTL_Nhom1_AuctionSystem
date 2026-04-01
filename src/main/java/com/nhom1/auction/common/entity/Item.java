package com.nhom1.auction.common.entity;

import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;

public abstract class Item extends BaseEntity {

    protected String name;
    protected String description;
  
    protected ItemCategory category;
    protected ItemCondition condition;

    public Item(String name, String description, 
        ItemCategory category, ItemCondition condition) {
        this.name = name;
        this.description = description;
        
        this.category = category;
        this.condition = condition;
    }

    public abstract void printInfo();

    // Getter
    public String getName() { return name; }
    public ItemCategory getCategory() { return category; }


}