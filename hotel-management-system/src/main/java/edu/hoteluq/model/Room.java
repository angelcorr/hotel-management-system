package edu.hoteluq.model;

import java.util.List;

public abstract class Room {
  private final int number;
  private final int floor;
  private final RoomType type;
  private boolean active = true;

  protected Room(int number, int floor, RoomType type) {
    this.number = number;
    this.floor = floor;
    this.type = type;
  }

  // Each room has different features depending on its type
  public abstract List<String> getFeatures();

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public int getNumber() {
    return number;
  }

  public RoomType getType() {
    return type;
  }

  public int getFloor() {
    return floor;
  }

  public double getPricePerNight() {
    return type.getPricePerNight();
  }

  public int getCapacity() {
    return type.getCapacity();
  }

  @Override
  public String toString() {
    return "Room " + number + " (" + type.getName() + ")";
  }
}
