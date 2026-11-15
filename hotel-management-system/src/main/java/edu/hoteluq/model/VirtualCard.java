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
  private java.time.Clock clock = edu.hoteluq.config.HotelTime.systemClock();
  private final String number;
  private BigDecimal balance = BigDecimal.ZERO;
  private boolean active = true;
  private final List<CardMovement> movements = new ArrayList<>();

  public VirtualCard(String number) {
    this.number = DomainValidation.text(number, "Card number");
  }

  public void charge(double amount, String description) { charge(edu.hoteluq.money.CopMoney.input(amount), "COP", description); }
  public void recharge(double amount, String description) { recharge(edu.hoteluq.money.CopMoney.input(amount), "COP", description); }
  public void refund(double amount, String description) { refund(edu.hoteluq.money.CopMoney.input(amount), "COP", description); }
  public void charge(BigDecimal amount, String currency, String description) { transfer(Action.CHARGE, amount, currency, description); }
  public void recharge(BigDecimal amount, String currency, String description) { transfer(Action.RECHARGE, amount, currency, description); }
  public void refund(BigDecimal amount, String currency, String description) { transfer(Action.REFUND, amount, currency, description); }
  private void transfer(Action action, BigDecimal input, String currency, String description) {
    change(action, null, () -> {
      edu.hoteluq.money.CopMoney.currency(currency);
      BigDecimal amount = edu.hoteluq.money.CopMoney.positive(input);
      if (action != Action.REFUND) checkActive();
      DomainValidation.text(description, "Description");
      BigDecimal updated = action == Action.CHARGE ? balance.subtract(amount) : balance.add(amount);
      if (updated.signum() < 0) throw new InsufficientBalanceException("Insufficient funds");
      edu.hoteluq.money.CopMoney.input(updated);
      var movement = new CardMovement(action == Action.CHARGE ? MovementType.PAYMENT : action == Action.REFUND ? MovementType.REFUND : MovementType.RECHARGE,
          amount, description, clock.instant(), null, null);
      movements.add(movement); balance = updated;
    });
  }
  /** Installed by the aggregate before guard attachment; subsequent external changes are denied. */
  public void configureClock(java.time.Clock clock) {
    change(Action.BOOKING_TRANSACTION, null, () -> this.clock = edu.hoteluq.config.HotelTime.inHotelZone(java.util.Objects.requireNonNull(clock)));
  }

  private record Checkpoint(BigDecimal balance, List<CardMovement> movements) {}

  public BigDecimal getBalanceAmount() { return readProtected(() -> balance); }
  public Object checkpoint() { return readProtected(() -> new Checkpoint(balance, List.copyOf(movements))); }
  public void restore(Object checkpoint) {
    change(Action.BOOKING_TRANSACTION, null, () -> {
      Checkpoint c = (Checkpoint) checkpoint;
      balance = c.balance(); movements.clear(); movements.addAll(c.movements());
    });
  }

  /** Aggregate-only transfer. The owner guard rejects calls outside a booking transaction. */
  public void bookingTransfer(boolean refund, BigDecimal amount, java.util.UUID reservationId,
      java.util.UUID paymentId, java.time.Instant at) {
    change(Action.BOOKING_TRANSACTION, null, () -> {
      edu.hoteluq.money.CopMoney.integral(amount);
      if (amount.signum() < 0) throw new edu.hoteluq.exceptions.HotelException("Negative transfer");
      if (!refund) checkActive(); // Inactive original cards still receive refunds (existing contract).
      BigDecimal updated = refund ? balance.add(amount) : balance.subtract(amount);
      if (updated.signum() < 0) throw new InsufficientBalanceException("Insufficient funds");
      edu.hoteluq.money.CopMoney.input(updated);
      if (amount.signum() > 0) {
        CardMovement movement = new CardMovement(refund ? MovementType.REFUND : MovementType.PAYMENT,
            amount, (refund ? "Refund " : "Payment ") + reservationId,
            at, reservationId, paymentId);
        movements.add(movement);
      }
      balance = updated;
    });
  }

  private void checkActive() {
    if (!active) throw new InactiveCardException("Card " + number + " is not active");
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
