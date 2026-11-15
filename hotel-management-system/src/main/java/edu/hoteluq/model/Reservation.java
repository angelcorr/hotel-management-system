package edu.hoteluq.model;

import edu.hoteluq.security.ManagedEntity;
import edu.hoteluq.security.Action;

import edu.hoteluq.validation.DomainValidation;

import edu.hoteluq.enums.ReservationStatus;
import edu.hoteluq.exceptions.InvalidReservationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import edu.hoteluq.state.*;
import edu.hoteluq.strategy.RefundPolicy;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Reservation extends ManagedEntity {
  private final UUID id;
  private final Client client;
  private final Room room;
  private final LocalDate checkIn;
  private final LocalDate checkOut;
  private final int guests;
  private final List<AdditionalService> services;
  private final Promotion promotion;
  private final double subtotal;
  private final double discount;
  private final double total;
  private static final LocalTime CHECK_IN_TIME = LocalTime.of(15, 0);
  private ReservationState state = new PendingState();


  private Reservation(Builder builder) {
    this.id = builder.id;
    this.client = builder.client;
    this.room = builder.room;
    this.checkIn = builder.checkIn;
    this.checkOut = builder.checkOut;
    this.guests = builder.guests;
    this.services = List.copyOf(builder.services);
    this.promotion = builder.promotion;
    this.subtotal = builder.subtotal;
    this.discount = builder.discount;
    this.total = builder.total;
  }

  public long getNights() {
    return readProtected(() -> ChronoUnit.DAYS.between(checkIn, checkOut));
  }

  public boolean overlaps(LocalDate from, LocalDate to) {
    DomainValidation.period(from, to);
    // A cancelled reservation doesn't block the room
    if (getStatus() == ReservationStatus.CANCELLED) return false;
    return checkIn.isBefore(to) && from.isBefore(checkOut); // True when this reservation uses the room during part of the given period.
  }

  public UUID getId() {
    return readProtected(() -> id);
  }

  public Client getClient() {
    return readProtected(() -> client);
  }

  public Room getRoom() {
    return readProtected(() -> room);
  }

  public LocalDate getCheckIn() {
    return readProtected(() -> checkIn);
  }

  public LocalDate getCheckOut() {
    return readProtected(() -> checkOut);
  }

  public int getGuests() {
    return readProtected(() -> guests);
  }

  public List<AdditionalService> getServices() {
    return readProtected(() -> services);
  }

  public Promotion getPromotion() {
    return readProtected(() -> promotion);
  }

  public double getSubtotal() {
    return readProtected(() -> subtotal);
  }

  public double getDiscount() {
    return readProtected(() -> discount);
  }

  public double getTotal() {
    return readProtected(() -> total);
  }

  public ReservationStatus getStatus() {
    return readProtected(() -> state.getStatus());
  }

  public void setStatus(ReservationStatus status) {
    DomainValidation.required(status, "Reservation status");
    setState(switch (status) {
      case PENDING -> new PendingState();
      case CONFIRMED -> new ConfirmedState();
      case CANCELLED -> new CancelledState();
      case COMPLETED -> new CompletedState();
    });
  }


  public ReservationState getState() { return readProtected(() -> state); }

  public void setState(ReservationState state) {
    change(Action.RESERVATION_STATE, state,
        () -> this.state = DomainValidation.required(state, "Reservation state"));
  }

  public void pay() {
    change(Action.RESERVATION_STATE, null, () -> state.pay(this));
  }

  public void cancel(RefundPolicy policy) {
    change(Action.RESERVATION_STATE, null,
        () -> state.cancel(this, DomainValidation.required(policy, "Refund policy")));
  }

  public void complete() {
    change(Action.RESERVATION_STATE, null, () -> state.complete(this));
  }

  public LocalDateTime getCheckInDateTime() {
    return readProtected(() -> checkIn.atTime(CHECK_IN_TIME));
  }

  @Override
  public String toString() {
    return id + ": " + client.getName() + ", " + room + ", " + checkIn + " to " + checkOut + " (" + getNights() + " nights), total $" + total + " [" + getStatus() + "]";
  }

  public static class Builder {
    private UUID id;
    private Client client;
    private Room room;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int guests = 1;
    private List<AdditionalService> services  = new ArrayList<>();
    private Promotion promotion;
    private double subtotal;
    private double discount;
    private double total;

    public Builder client(Client client) {
      this.client = client;
      return this;
    }

    public Builder room(Room room) {
      this.room = room;
      return this;
    }

    public Builder checkIn(LocalDate checkIn) {
      this.checkIn = checkIn;
      return this;
    }

    public Builder checkOut(LocalDate checkOut) {
      this.checkOut = checkOut;
      return this;
    }

    public Builder guests(int guests) {
      this.guests = guests;
      return this;
    }

    public Builder additionalService(AdditionalService service) {
      this.services.add(DomainValidation.required(service, "Service"));
      return this;
    }

    public Builder promotion(Promotion promotion) {
      this.promotion = promotion;
      return this;
    }


    public Reservation build() {
      validate();
      this.id = UUID.randomUUID();
      calculatePrice();
      return new Reservation(this);
    }

    private void calculatePrice() {
      long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
      BigDecimal base = BigDecimal.valueOf(room.getPricePerNight()).multiply(BigDecimal.valueOf(nights));
      for (AdditionalService service : services) {
        base = base.add(BigDecimal.valueOf(service.getPrice()));
      }
      BigDecimal reduction = promotion == null ? BigDecimal.ZERO : base
          .multiply(BigDecimal.valueOf(promotion.getDiscountPercent())).movePointLeft(2);
      subtotal = base.doubleValue();
      discount = reduction.doubleValue();
      total = base.subtract(reduction).doubleValue();
      DomainValidation.nonNegative(subtotal, "Reservation subtotal");
      DomainValidation.nonNegative(discount, "Reservation discount");
      DomainValidation.nonNegative(total, "Reservation total");
    }

    private void validate() {
      if (client == null) throw new InvalidReservationException("The reservation needs a client");
      if (room == null) throw new InvalidReservationException("The reservation needs a room");
      if (checkIn == null || checkOut == null) {
        throw new InvalidReservationException("The reservation needs a check-in and check-out dates");
      }
      if (!checkOut.isAfter(checkIn)) {
        throw new InvalidReservationException("The check-out date must come after the check-in date");
      }
      if (!room.isActive()) {
        throw new InvalidReservationException(room + " is out of service");
      }
      if (guests < 1 || guests > room.getCapacity()) {
        throw new InvalidReservationException(
            room + " allows 1 to " + room.getCapacity() + " guests, not " + guests
        );
      }

      for (AdditionalService service : services) {
        if (!service.isAvailable()) {
          throw new InvalidReservationException("The service " + service.getName() + " is not available");
        }
      }
    }
  }
}
