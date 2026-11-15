package edu.hoteluq.model;

import edu.hoteluq.security.ManagedEntity;
import edu.hoteluq.security.Action;

import edu.hoteluq.validation.DomainValidation;

public class AdditionalService extends ManagedEntity {
  private final java.util.UUID id = java.util.UUID.randomUUID();
  private String name;
  private double price;
  private boolean available = true;

  public AdditionalService(String name, double price) {
    this.name = DomainValidation.text(name, "Service name");
    this.price = DomainValidation.nonNegative(price, "Service price");
  }

  public java.util.UUID getId() { return readProtected(() -> id); }

  public String getName() {
    return readProtected(() -> name);
  }

  public double getPrice() {
    return readProtected(() -> price);
  }

  public boolean isAvailable() {
    return readProtected(() -> available);
  }

  public void setName(String name) {
    String valid = DomainValidation.text(name, "Service name");
    change(Action.NAME, valid, () -> this.name = valid);
  }

  public void updateDetails(String name, double price) {
    String validName = DomainValidation.text(name, "Service name");
    double validPrice = DomainValidation.nonNegative(price, "Service price");
    change(Action.NAME, validName, () -> { this.name = validName; this.price = validPrice; });
  }

  public void setPrice(double price) {
    change(Action.UPDATE, price, () -> {
      this.price = DomainValidation.nonNegative(price, "Service price");
    });
  }

  public void setAvailable(boolean available) {
    change(Action.AVAILABILITY, available, () -> {
      this.available = available;
    });
  }

  @Override
  public String toString() {
    return readProtected(() -> name);
  }
}
