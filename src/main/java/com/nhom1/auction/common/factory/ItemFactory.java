package com.nhom1.auction.common.factory;

import com.nhom1.auction.common.entity.Art;
import com.nhom1.auction.common.entity.Electronics;
import com.nhom1.auction.common.entity.Item;
import com.nhom1.auction.common.entity.Vehicle;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.enums.VehicleFuelType;

public class ItemFactory {

    public static Item createElectronics(String name, String description, String brand, ItemCondition condition, String asus, int warrantyMonths) {
        return new Electronics( name, description, 
            ItemCategory.ELECTRONICS, condition, brand, warrantyMonths);
    }

    public static Item createArt(String name, String description, String artist, ItemCondition condition, String era, String postImpressionism) {
        return new Art(name, description, 
            ItemCategory.ART, condition, artist, era);
    }


    public static Item createVehicle( String name, String description, String make, ItemCondition condition, String honda, int productionYear, VehicleFuelType fuelType) {
        return new Vehicle( name, description,
            ItemCategory.VEHICLE, condition, make, productionYear, fuelType);
    }
}