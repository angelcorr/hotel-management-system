package edu.hoteluq.model;

import edu.hoteluq.enums.MovementType;

import java.time.LocalDateTime;

public class CardMovement {
  private final MovementType type;
  private final double amount;
  private final String description;
  private final LocalDateTime date;

  public CardMovement(MovementType type, double amount, String description, LocalDateTime date) {
    this.type = type;
    this.amount = amount;
    this.description = description;
    this.date = date;
  }

  public MovementType getType() {
    return type;
  }

  public double getAmount() {
    return amount;
  }

  public String getDescription() {
    return description;
  }

  public LocalDateTime getDate() {
    return date;
  }

  @Override
  public String toString() {
    return date.toLocalDate() + " " + type + " $" + amount + " - " + description;
  }
}
