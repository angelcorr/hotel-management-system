package edu.hoteluq.model;

import edu.hoteluq.security.ManagedEntity;
import edu.hoteluq.security.Action;

import edu.hoteluq.validation.DomainValidation;

import java.util.List;

public abstract class Room extends ManagedEntity {
  private final int number;
  private int floor;
  private final RoomType type;
  private volatile boolean active = true;

  protected Room(int number, int floor, RoomType type) {
    this.number = DomainValidation.positive(number, "Room number");
    this.floor = floor;
    this.type = DomainValidation.required(type, "Room type");
  }

  // Each room has different features depending on its type
  public abstract List<String> getFeatures();

  public boolean isActive() {
    return readProtected(() -> active) && type.isActive();
  }

  public void setActive(boolean active) {
    change(Action.AVAILABILITY, active, () -> {
      this.active = active;
    });
  }

  public void updateDetails(int floor, boolean active) {
    change(Action.FLOOR, floor, () -> { this.floor = floor; this.active = active; });
  }

  public void setFloor(int floor) {
    change(Action.FLOOR, floor, () -> this.floor = floor);
  }

  public int getNumber() {
    return readProtected(() -> number);
  }

  public RoomType getType() {
    return readProtected(() -> type);
  }

  public int getFloor() {
    return readProtected(() -> floor);
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
