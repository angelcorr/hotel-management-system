package edu.hoteluq.model;

import edu.hoteluq.security.ManagedEntity;
import edu.hoteluq.security.Action;

import edu.hoteluq.validation.DomainValidation;

import edu.hoteluq.enums.MovementType;
import edu.hoteluq.exceptions.InactiveCardException;
import edu.hoteluq.exceptions.InsufficientBalanceException;
import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VirtualCard extends ManagedEntity {
  private final String number;
  private BigDecimal balance = BigDecimal.ZERO;
  private boolean active = true;
  private final List<CardMovement> movements = new ArrayList<>();

  public VirtualCard(String number) {
    this.number = DomainValidation.text(number, "Card number");
  }

  public void charge(double amount, String description) {
    change(Action.CHARGE, null, () -> {
      checkActive();
      checkPositive(amount);
      DomainValidation.text(description, "Description");
      if (BigDecimal.valueOf(amount).compareTo(balance) > 0) {
        throw new InsufficientBalanceException("Insufficient funds");
      }

      balance = balance.subtract(BigDecimal.valueOf(amount));
      register(MovementType.PAYMENT, amount, description);
    });
  }


  public void recharge(double amount, String description) {
    change(Action.RECHARGE, null, () -> {
      checkActive();
      checkPositive(amount);
      DomainValidation.text(description, "Description");
      BigDecimal updated = balance.add(BigDecimal.valueOf(amount));
      DomainValidation.nonNegative(updated.doubleValue(), "Resulting balance");
      balance = updated;
      register(MovementType.RECHARGE, amount, description);
    });
  }

  public void refund(double amount, String description) {
    change(Action.REFUND, null, () -> {
      checkPositive(amount);
      DomainValidation.text(description, "Description");
      BigDecimal updated = balance.add(BigDecimal.valueOf(amount));
      DomainValidation.nonNegative(updated.doubleValue(), "Resulting balance");
      balance = updated;
      register(MovementType.REFUND, amount, description);
    });
  }

  private void register(MovementType type, double amount, String description) {
    movements.add(new CardMovement(type, amount, description, LocalDateTime.now()));
  }

  private void checkActive() {
    if (!active) throw new InactiveCardException("Card " + number + " is not active");
  }

  private void checkPositive(double amount) {
    DomainValidation.positive(amount, "Amount");
  }

  public void setActive(boolean active) {
    change(Action.CARD_STATE, active, () -> this.active = active);
  }

  public boolean isActive() {
    return readProtected(() -> active);
  }

  public double getBalance() {
    return readProtected(() -> balance.doubleValue());
  }

  public String getNumber() {
    return readProtected(() -> number);
  }

  public List<CardMovement> getMovements() {
    return readProtected(() -> List.copyOf(movements));
  }
}
