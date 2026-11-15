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
                             int capacity, String description, boolean active) {}
  public record RoomView(int number, int floor, RoomCategory category, boolean active, List<String> features) {
    public RoomView { features = List.copyOf(features); }
  }
  public record ServiceView(UUID id, String name, double price, boolean available) {}
  public record PromotionView(UUID id, String name, double discountPercent) {}
  public record CardView(String number, double balance, boolean active, List<CardMovement> movements) {
    public CardView { movements = List.copyOf(movements); }
  }
  public record ReservationView(UUID id, String clientId, int roomNumber, LocalDate checkIn,
                                LocalDate checkOut, int guests, ReservationStatus status, double total) {}

  public static UserView user(User user) {
    return new UserView(user.getId(), user.getName(), user.getEmail(), user.getRole());
  }
  public static RoomTypeView type(RoomType type) {
    return new RoomTypeView(type.getCategory(), type.getName(), type.getPricePerNight(),
        type.getCapacity(), type.getDescription(), type.isActive());
  }
  public static RoomView room(Room room) {
    return new RoomView(room.getNumber(), room.getFloor(), room.getType().getCategory(), room.isActive(), room.getFeatures());
  }
  public static ServiceView service(AdditionalService service) {
    return new ServiceView(service.getId(), service.getName(), service.getPrice(), service.isAvailable());
  }
  public static PromotionView promotion(Promotion promotion) {
    return new PromotionView(promotion.getId(), promotion.getName(), promotion.getDiscountPercent());
  }
  public static CardView card(VirtualCard card) {
    return new CardView(card.getNumber(), card.getBalance(), card.isActive(), card.getMovements());
  }
  public static ReservationView reservation(Reservation reservation) {
    return new ReservationView(reservation.getId(), reservation.getClient().getId(), reservation.getRoom().getNumber(),
        reservation.getCheckIn(), reservation.getCheckOut(), reservation.getGuests(), reservation.getStatus(), reservation.getTotal());
  }
}
