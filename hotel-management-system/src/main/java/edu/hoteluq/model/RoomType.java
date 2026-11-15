package edu.hoteluq.model;

import edu.hoteluq.security.ManagedEntity;
import edu.hoteluq.security.Action;

import edu.hoteluq.validation.DomainValidation;

import edu.hoteluq.enums.RoomCategory;

public class RoomType extends ManagedEntity {
  private final RoomCategory category;
  private String name;
  private double pricePerNight;
  private int capacity;
  private String description;
  private volatile boolean active = true;

  public RoomType(RoomCategory category, double pricePerNight, String name, int capacity, String description) {
    this.category = DomainValidation.required(category, "Category");
    this.pricePerNight = DomainValidation.nonNegative(pricePerNight, "Nightly price");
    this.name = DomainValidation.text(name, "Name");
    this.capacity = DomainValidation.positive(capacity, "Capacity");
    this.description = DomainValidation.text(description, "Description");
  }

  public RoomCategory getCategory() {
    return readProtected(() -> category);
  }

  public String getName() {
    return readProtected(() -> name);
  }

  public double getPricePerNight() {
    return readProtected(() -> pricePerNight);
  }

  public int getCapacity() {
    return readProtected(() -> capacity);
  }

  public String getDescription() {
    return readProtected(() -> description);
  }

  public void setName(String name) {
    change(Action.NAME, name, () -> {
      this.name = DomainValidation.text(name, "Name");
    });
  }

  public void setPricePerNight(double pricePerNight) {
    change(Action.UPDATE, pricePerNight, () -> {
      this.pricePerNight = DomainValidation.nonNegative(pricePerNight, "Nightly price");
    });
  }

  public void setCapacity(int capacity) {
    change(Action.UPDATE, capacity, () -> {
      this.capacity = DomainValidation.positive(capacity, "Capacity");
    });
  }

  public void setDescription(String description) {
    change(Action.UPDATE, description, () -> {
      this.description = DomainValidation.text(description, "Description");
    });
  }

  public void updateDetails(String name, double price, int capacity, String description) {
    String validName = DomainValidation.text(name, "Name");
    double validPrice = DomainValidation.nonNegative(price, "Nightly price");
    int validCapacity = DomainValidation.positive(capacity, "Capacity");
    String validDescription = DomainValidation.text(description, "Description");
    change(Action.NAME, validName, () -> {
        this.name = validName;
        this.pricePerNight = validPrice;
        this.capacity = validCapacity;
        this.description = validDescription;
    });
  }

  public boolean isActive() { return readProtected(() -> active); }

  public void setActive(boolean active) {
    change(Action.AVAILABILITY, active, () -> this.active = active);
  }

  @Override
  public String toString() {
    return readProtected(() -> name);
  }
}
