package edu.hoteluq.model;

import edu.hoteluq.enums.MovementType;
import edu.hoteluq.validation.DomainValidation;
import edu.hoteluq.config.HotelTime;
import edu.hoteluq.money.CopMoney;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public final class CardMovement {
  private final UUID id = UUID.randomUUID(), reservationId, paymentId;
  private final BigDecimal amount;
  private final MovementType type;
  private final String description;
  private final Instant at;
  public CardMovement(MovementType type, double amount, String description, LocalDateTime date) {
    this(type, CopMoney.input(amount), description, DomainValidation.required(date, "Date").atZone(HotelTime.ZONE).toInstant(), null, null);
  }
  public CardMovement(MovementType type, BigDecimal amount, String description, LocalDateTime date, UUID reservationId, UUID paymentId) {
    this(type, amount, description, DomainValidation.required(date, "Date").atZone(HotelTime.ZONE).toInstant(), reservationId, paymentId);
  }
  public CardMovement(MovementType type, BigDecimal amount, String description, Instant at, UUID reservationId, UUID paymentId) {
    this.type = DomainValidation.required(type, "Movement type");
    this.amount = CopMoney.positive(amount);
    this.description = DomainValidation.text(description, "Description");
    this.at = DomainValidation.required(at, "Movement instant");
    this.reservationId = reservationId; this.paymentId = paymentId;
  }
  public UUID getId() { return id; }
  public UUID getReservationId() { return reservationId; }
  public UUID getPaymentId() { return paymentId; }
  public BigDecimal getExactAmount() { return amount; }
  public String getCurrency() { return CopMoney.CURRENCY; }
  public MovementType getType() { return type; }
  public double getAmount() { return amount.doubleValue(); }
  public String getDescription() { return description; }
  public LocalDateTime getDate() { return HotelTime.local(at); }
  public Instant getInstant() { return at; }
  @Override public String toString() { return at + " " + type + " COP " + amount + " - " + description; }
}
