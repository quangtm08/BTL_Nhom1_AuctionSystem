package com.nhom1.auction.common.dto.auction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.enums.VehicleFuelType;
public class CreateAuctionRequest {
    
    // Item information
    private String name;
    private String description;
    private ItemCategory category;
    private ItemCondition condition;
    
    // Auction pricing and timing
    private BigDecimal startingPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    // Seller information
    private String sellerId;
    
    // Category-specific fields: Art
    private String artist;
    private String era;
    
    // Category-specific fields: Electronics
    private String brand;
    private Integer warrantyMonths;
    
    // Category-specific fields: Vehicle
    private Integer productionYear;
    private VehicleFuelType fuelType;

    public CreateAuctionRequest() {}

    public CreateAuctionRequest(String name, String description, ItemCategory category, ItemCondition condition, BigDecimal startingPrice, LocalDateTime startTime, LocalDateTime endTime, String sellerId, String artist, String era, String brand, Integer warrantyMonths, Integer productionYear, VehicleFuelType fuelType) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.condition = condition;
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sellerId = sellerId;
        this.artist = artist;
        this.era = era;
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
        this.productionYear = productionYear;
        this.fuelType = fuelType;
    }

    

    // Getters and Setters for basic item info
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ItemCategory getCategory() {
        return category;
    }

    public void setCategory(ItemCategory category) {
        this.category = category;
    }

    public ItemCondition getCondition() {
        return condition;
    }

    public void setCondition(ItemCondition condition) {
        this.condition = condition;
    }

    // Getters and Setters for auction pricing and timing
    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    // Getters and Setters for seller
    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    // Getters and Setters for Art-specific fields
    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getEra() {
        return era;
    }

    public void setEra(String era) {
        this.era = era;
    }

    // Getters and Setters for Electronics-specific fields
    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Integer getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(Integer warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    // Getters and Setters for Vehicle-specific fields
    public Integer getProductionYear() {
        return productionYear;
    }

    public void setProductionYear(Integer productionYear) {
        this.productionYear = productionYear;
    }

    public VehicleFuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(VehicleFuelType fuelType) {
        this.fuelType = fuelType;
    }
}