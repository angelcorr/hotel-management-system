package edu.hoteluq.model;

public class AdditionalService {
  private final String name;
  private double price;
  private boolean available = true;

  public AdditionalService(String name, double price) {
    this.name = name;
    this.price = price;
  }

  public String getName() {
    return name;
  }

  public double getPrice() {
    return price;
  }

  public boolean isAvailable() {
    return available;
  }

  public void setPrice(double price) {
    this.price = price;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }

  @Override
  public String toString() {
    return name;
  }
}
