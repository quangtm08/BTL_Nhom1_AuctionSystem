package com.nhom1.auction.common.factory;

import com.nhom1.auction.common.entity.*;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.enums.VehicleFuelType;
import java.time.LocalDateTime;

public class ItemFactory {

    public static Item createElectronics(String id, String name, String description, double startingPrice,
        LocalDateTime startTime, LocalDateTime endTime,
        ItemCondition condition, String brand, int warrantyMonths) {
        return new Electronics(id, name, description, startingPrice, startTime, endTime,
            ItemCategory.ELECTRONICS, condition, brand, warrantyMonths);
    }

    public static Item createArt(String id, String name, String description, double startingPrice,
        LocalDateTime startTime, LocalDateTime endTime,
        ItemCondition condition, String artist, String era) {
        return new Art(id, name, description, startingPrice, startTime, endTime,
            ItemCategory.ART, condition, artist, era);
    }


    public static Item createVehicle(String id, String name, String description, double startingPrice,
        LocalDateTime startTime, LocalDateTime endTime,
        ItemCondition condition, String make, int productionYear, VehicleFuelType fuelType) {
        return new Vehicle(id, name, description, startingPrice, startTime, endTime,
            ItemCategory.VEHICLE, condition, make, productionYear, fuelType);
    }
}