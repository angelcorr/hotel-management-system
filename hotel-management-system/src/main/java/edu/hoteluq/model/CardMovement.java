package edu.hoteluq.model;

import edu.hoteluq.validation.DomainValidation;

import edu.hoteluq.enums.MovementType;

import java.time.LocalDateTime;

public class CardMovement {
  private final MovementType type;
  private final double amount;
  private final String description;
  private final LocalDateTime date;

  public CardMovement(MovementType type, double amount, String description, LocalDateTime date) {
    this.type = DomainValidation.required(type, "Movement type");
    this.amount = DomainValidation.positive(amount, "Movement amount");
    this.description = DomainValidation.text(description, "Description");
    this.date = DomainValidation.required(date, "Movement date");
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
