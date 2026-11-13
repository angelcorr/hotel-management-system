package edu.hoteluq.singleton;

import edu.hoteluq.enums.RoomCategory;
import edu.hoteluq.exceptions.DuplicateRoomException;
import edu.hoteluq.exceptions.HotelException;
import edu.hoteluq.exceptions.RoomNotAvailableException;
import edu.hoteluq.model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hotel {
  private static Hotel instance;

  private final List<RoomType> roomTypes = new ArrayList<>();
  private final List<Room> rooms = new ArrayList<>();
  private final List<User> users = new ArrayList<>();
  private final List<AdditionalService> services = new ArrayList<>();
  private final List<Promotion> promotions = new ArrayList<>();
  private final List<Reservation> reservations = new ArrayList<>();

  private Hotel() {}

  public static Hotel getInstance() {
    if (instance == null) {
      instance = new Hotel();
    }

    return instance;
  }

  public void addRoomType(RoomType type) {
    roomTypes.add(type);
  }

  public RoomType getRoomType(RoomCategory category) {
    for (RoomType type : roomTypes) {
      if (type.getCategory() == category) return type;
    }

    throw new HotelException("There is no room type for " + category);
  }

  public List<RoomType> getRoomTypes() {
    return Collections.unmodifiableList(roomTypes);
  }

  public void addRoom(Room room) {
    // Early return when room already exists
    if (findRoom(room.getNumber()) != null) {
      throw new DuplicateRoomException("Room " + room.getNumber() + " already exists");
    }

    rooms.add(room);
  }

  public Room findRoom(int number) {
    for (Room room : rooms) {
      if (room.getNumber() == number) return room;
    }

    return null;
  }

  public List<Room> getRooms() {
    return Collections.unmodifiableList(rooms);
  }

  public void addUser(User user) {
    for (User existingUser : users) {
      if (existingUser.getId().equals(user.getId())) {
        throw new HotelException("Duplicated id: " + user.getId());
      }

      if (existingUser.getEmail().equalsIgnoreCase(user.getEmail())) {
        throw new HotelException("Duplicated email: " + user.getEmail());
      }
    }
    users.add(user);
  }

  public User findUserByEmail(String email) {
    for (User user : users) {
      if (user.getEmail().equalsIgnoreCase(email)) return user;
    }

    return null;
  }

  public List<Client> getClients() {
    List<Client> clients = new ArrayList<>();
    for (User user : users) {
      if (user instanceof Client client) clients.add(client);
    }

    return clients;
  }

  public void addService(AdditionalService service) {
    services.add(service);
  }

  public List<AdditionalService> getServices() {
    return Collections.unmodifiableList(services);
  }

  public void addPromotion(Promotion promotion) {
    promotions.add(promotion);
  }

  public List<Promotion> getPromotions() {
    return Collections.unmodifiableList(promotions);
  }

  public boolean isAvailable(Room room, LocalDate from, LocalDate to) {
    if (!room.isActive()) return false; // Early return when the room is not available

    for (Reservation reservation : reservations) {
      if (reservation.getRoom() == room && reservation.overlaps(from, to)) {
        return false;
      }
    }

    return true;
  }

  public List<Room> getAvailableRooms(LocalDate from, LocalDate to) {
    List<Room> available = new ArrayList<>();
    for (Room room : rooms) {
      if (isAvailable(room, from, to)) available.add(room);
    }

    return available;
  }

  public void addReservation(Reservation reservation) {
    if (!isAvailable(reservation.getRoom(), reservation.getCheckIn(), reservation.getCheckOut())) {
      throw new RoomNotAvailableException(reservation.getRoom() + " is not available from " + reservation.getCheckIn() + " to " + reservation.getCheckOut());
    }

    reservations.add(reservation);
  }

  public List<Reservation> getReservations() {
    return reservations;
  }
}
