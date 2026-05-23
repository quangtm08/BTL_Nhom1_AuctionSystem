package com.nhom1.auction.common.entity;

import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import java.time.LocalDateTime;
import java.util.UUID;

public abstract class Item extends BaseEntity {

  protected String name;
  protected String description;

  protected ItemCategory category;
  protected ItemCondition condition;

  public Item(String name, String description, ItemCategory category, ItemCondition condition) {
    super();
    this.name = name;
    this.description = description;

    this.category = category;
    this.condition = condition;
  }

  /*
   * Use this constructor for loading an EXISTING item from the database.
   */
  protected Item(
      UUID id,
      String name,
      String description,
      ItemCategory category,
      ItemCondition condition,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    super(id, createdAt, updatedAt);
    this.name = name;
    this.description = description;
    this.category = category;
    this.condition = condition;
  }

  public abstract void printInfo();

  // Getter
  public String getName() {
    return name;
  }

  public ItemCategory getCategory() {
    return category;
  }

  public ItemCondition getCondition() {
    return condition;
  }

  public String getDescription() {
    return description;
  }

  // Setter
  public void setCondition(ItemCondition condition) {
    this.condition = condition;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setCategory(ItemCategory category) {
    this.category = category;
  }

  public void setName(String name) {
    this.name = name;
  }
}
