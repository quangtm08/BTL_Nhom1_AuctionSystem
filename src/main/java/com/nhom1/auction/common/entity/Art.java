package com.nhom1.auction.common.entity;

import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import java.time.LocalDateTime;

public class Art extends Item {
    private String artist;
    private String era;

    public Art(String id, String name, String description, double startingPrice,
        LocalDateTime startTime, LocalDateTime endTime,
        ItemCategory category, ItemCondition condition,
        String artist, String era) {
        super(id, name, description, startingPrice, startTime, endTime, category, condition);
        this.artist = artist;
        this.era = era;
    }

    @Override
    public void printInfo() {
        System.out.println("[Art] " + name + " | Artist: " + artist + " | Era: " + era);
        System.out.println("Current Price: $" + currentHighestBid);
    }
}