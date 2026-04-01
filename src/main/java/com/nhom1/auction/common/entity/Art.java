package com.nhom1.auction.common.entity;

import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;

public class Art extends Item {
    private final String artist;
    private final String era;

    public Art(String name, String description,
        ItemCategory category, ItemCondition condition,
        String artist, String era) {
        super(name, description, category, condition);
        this.artist = artist;
        this.era = era;
    }

    @Override
    public void printInfo() {
        System.out.println("[Art] " + name + " | Artist: " + artist + " | Era: " + era);
        
    }
}