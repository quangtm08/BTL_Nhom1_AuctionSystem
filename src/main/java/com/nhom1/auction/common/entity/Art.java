package com.nhom1.auction.common.entity;

import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;

public class Art extends Item {
    private String artist;
    private String era;

    public Art(String name, String description,
        ItemCategory category, ItemCondition condition,
        String artist, String era) {
        super(name, description, category, condition);
        this.artist = artist;
        this.era = era;
    }
    //Getter
    public String getArtist(){ return artist; }
    public String getEra(){ return era; }
    //Setter
    public void setArtist(String artist){ this.artist = artist; }
    public void setEra(String era){  this.era = era; }

    @Override
    public void printInfo() {
        System.out.println("[Art] " + name + " | Artist: " + artist + " | Era: " + era);
        
    }
}
