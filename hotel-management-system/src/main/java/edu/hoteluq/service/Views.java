package edu.hoteluq.service;

import edu.hoteluq.enums.*;
import edu.hoteluq.model.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Immutable application results. No entity references, password hashes, salts or bearer tokens. */
public final class Views {
  private Views() {}
  public record UserView(String id, String name, String email, Role role) {}
  public record RoomTypeView(RoomCategory category, String name, double pricePerNight,
                             int capacity, String description, boolean active, java.math.BigDecimal priceAmount) {
    public RoomTypeView(RoomCategory category,String name,double price,int capacity,String description,boolean active) {this(category,name,price,capacity,description,active,java.math.BigDecimal.valueOf(price));}
  }
  public record RoomView(int number, int floor, RoomCategory category, boolean active, List<String> features) {
    public RoomView { features = List.copyOf(features); }
  }
  public record ServiceView(UUID id, String name, double price, boolean available, java.math.BigDecimal priceAmount) {
    public ServiceView(UUID id,String name,double price,boolean available) {this(id,name,price,available,java.math.BigDecimal.valueOf(price));}
  }
  public record PromotionView(UUID id, String name, double discountPercent) {}
  public record CardView(String number, double balance, boolean active, List<CardMovement> movements, java.math.BigDecimal exactBalance) {
    public CardView { movements = List.copyOf(movements); }
    public CardView(String number, double balance, boolean active, List<CardMovement> movements) {
      this(number, balance, active, movements, java.math.BigDecimal.valueOf(balance));
    }
  }
  public record ReservationView(UUID id, String clientId, int roomNumber, LocalDate checkIn,
                                LocalDate checkOut, int guests, ReservationStatus status, double total) {}

  public static UserView user(User user) {
    return new UserView(user.getId(), user.getName(), user.getEmail(), user.getRole());
  }
  public static RoomTypeView type(RoomType type) {
    return new RoomTypeView(type.getCategory(), type.getName(), type.getPricePerNight(),
        type.getCapacity(), type.getDescription(), type.isActive(),type.getPricePerNightAmount());
  }
  public static RoomView room(Room room) {
    return new RoomView(room.getNumber(), room.getFloor(), room.getType().getCategory(), room.isActive(), room.getFeatures());
  }
  public static ServiceView service(AdditionalService service) {
    return new ServiceView(service.getId(), service.getName(), service.getPrice(), service.isAvailable(),service.getPriceAmount());
  }
  public static PromotionView promotion(Promotion promotion) {
    return new PromotionView(promotion.getId(), promotion.getName(), promotion.getDiscountPercent());
  }
  public static CardView card(VirtualCard card) {
    return new CardView(card.getNumber(), card.getBalance(), card.isActive(), card.getMovements(), card.getBalanceAmount());
  }
  public static ReservationView reservation(Reservation reservation) {
    return new ReservationView(reservation.getId(), reservation.getClient().getId(), reservation.getRoom().getNumber(),
        reservation.getCheckIn(), reservation.getCheckOut(), reservation.getGuests(), reservation.getStatus(), reservation.getTotal());
  }
}
