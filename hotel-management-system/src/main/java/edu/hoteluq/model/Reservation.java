package edu.hoteluq.model;

import edu.hoteluq.enums.ReservationStatus;
import edu.hoteluq.exceptions.InvalidReservationException;
import edu.hoteluq.state.PendingState;
import edu.hoteluq.state.ReservationState;
import edu.hoteluq.strategy.RefundPolicy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Reservation {
  private static final LocalTime CHECK_IN_TIME = LocalTime.of(15,0);

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
    return ChronoUnit.DAYS.between(checkIn, checkOut);
  }

  public boolean overlaps(LocalDate from, LocalDate to) {
    // A canceled reservation doesn't block the room
    if (getStatus() == ReservationStatus.CANCELLED) return false;
    return checkIn.isBefore(to) && from.isBefore(checkOut); // True when this reservation uses the room during part of the given period.
  }

  public UUID getId() {
    return id;
  }

  public Client getClient() {
    return client;
  }

  public Room getRoom() {
    return room;
  }

  public LocalDate getCheckIn() {
    return checkIn;
  }

  public LocalDate getCheckOut() {
    return checkOut;
  }

  public int getGuests() {
    return guests;
  }

  public List<AdditionalService> getServices() {
    return services;
  }

  public Promotion getPromotion() {
    return promotion;
  }

  public double getSubtotal() {
    return subtotal;
  }

  public double getDiscount() {
    return discount;
  }

  public double getTotal() {
    return total;
  }

  public ReservationState getState() {
    return state;
  }

  public void setState(ReservationState state) {
    this.state = state;
  }

  public ReservationStatus getStatus() {
    return state.getStatus();
  }

  public void pay() {
    state.pay(this);
  }

  public void cancel(RefundPolicy policy) {
    state.cancel(this, policy);
  }

  public void complete() {
    state.complete(this);
  }

  public LocalDateTime getCheckInDateTime() {
    return checkIn.atTime(CHECK_IN_TIME);
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
      this.services.add(service);
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
      subtotal = nights * room.getPricePerNight();
      for (AdditionalService service : services) {
        subtotal += service.getPrice();
      }

      discount = 0;
      if (promotion != null) {
        discount = subtotal * promotion.getDiscountPercent() / 100;
      }

      total = subtotal - discount;
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
