package edu.hoteluq.model;

import edu.hoteluq.enums.RoomCategory;

public class RoomType {
  private final RoomCategory category;
  private String name;
  private double pricePerNight;
  private int capacity;
  private String description;

  public RoomType(RoomCategory category, double pricePerNight, String name, int capacity, String description) {
    this.category = category;
    this.pricePerNight = pricePerNight;
    this.name = name;
    this.capacity = capacity;
    this.description = description;
  }

  public RoomCategory getCategory() {
    return category;
  }

  public String getName() {
    return name;
  }

  public double getPricePerNight() {
    return pricePerNight;
  }

  public int getCapacity() {
    return capacity;
  }

  public String getDescription() {
    return description;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPricePerNight(double pricePerNight) {
    this.pricePerNight = pricePerNight;
  }

  public void setCapacity(int capacity) {
    this.capacity = capacity;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return name;
  }
}
