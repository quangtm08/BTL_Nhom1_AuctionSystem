package com.nhom1.auction.common.factory;

import com.nhom1.auction.common.entity.Art;
import com.nhom1.auction.common.entity.Electronics;
import com.nhom1.auction.common.entity.Item;
import com.nhom1.auction.common.entity.Vehicle;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;

public class ItemFactory {

  public static Item createElectronics(String name, String description, ItemCondition condition) {
    return new Electronics(name, description, ItemCategory.ELECTRONICS, condition);
  }

  public static Item createArt(String name, String description, ItemCondition condition) {
    return new Art(name, description, ItemCategory.ART, condition);
  }

  public static Item createVehicle(String name, String description, ItemCondition condition) {
    return new Vehicle(name, description, ItemCategory.VEHICLE, condition);
  }
}
