package edu.hoteluq.model;

import edu.hoteluq.enums.MovementType;
import edu.hoteluq.exceptions.HotelException;
import edu.hoteluq.exceptions.InactiveCardException;
import edu.hoteluq.exceptions.InsuficientBalanceException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VirtualCard {
  private final String number;
  private double balance;
  private boolean active = true;
  private final List<CardMovement> movements = new ArrayList<>();

  public VirtualCard(String number) {
    this.number = number;
  }

  // TODO: this must be only used by administrator
  public void charge(double amount, String description) {
    checkActive();
    checkPositive(amount);
    if (amount > balance) {
      throw new InsuficientBalanceException("Insufficient founds");
    }

    balance -= amount;
    register(MovementType.PAYMENT, amount, description);
  }


  public void recharge(double amount, String description) {
    checkActive();
    checkPositive(amount);
    balance += amount;
    register(MovementType.RECHARGE, amount, description);
  }

  public void refund(double amount, String description) {
    checkPositive(amount);
    balance += amount;
    register(MovementType.REFUND, amount, description);
  }

  public void register(MovementType type, double amount, String description) {
    movements.add(new CardMovement(type, amount, description, LocalDateTime.now()));
  }

  private void checkActive() {
    if (!active) throw new InactiveCardException("Card " + number + " is not active");
  }

  private void checkPositive(double amount) {
    if (amount <= 0) throw new HotelException("The amount must be greater than zero");
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public boolean isActive() {
    return active;
  }

  public double getBalance() {
    return balance;
  }

  public String getNumber() {
    return number;
  }

  public List<CardMovement> getMovements() {
    return Collections.unmodifiableList(movements);
  }
}
