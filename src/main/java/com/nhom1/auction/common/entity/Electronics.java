package com.nhom1.auction.common.entity;

import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import java.time.LocalDateTime;
import java.util.UUID;

public class Electronics extends Item {

  public Electronics(
      String name, String description, ItemCategory category, ItemCondition condition) {
    super(name, description, category, condition);
  }

  /** Use this constructor for loading an EXISTING electronics item from the database. */
  public Electronics(
      UUID id,
      String name,
      String description,
      ItemCategory category,
      ItemCondition condition,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    super(id, name, description, category, condition, createdAt, updatedAt);
  }

  @Override
  public void printInfo() {
    System.out.println("[Electronics] " + name);
  }
}
