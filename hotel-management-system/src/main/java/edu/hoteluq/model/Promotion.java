package edu.hoteluq.model;

public class Promotion {
  private final String name;
  private final double discountPercent;

  public Promotion(String name, double discountPercent) {
    this.name = name;
    this.discountPercent = discountPercent;
  }

  public String getName() {
    return name;
  }

  public double getDiscountPercent() {
    return discountPercent;
  }

  @Override
  public String toString() {
    return name + " (" + discountPercent + "%)";
  }
}
