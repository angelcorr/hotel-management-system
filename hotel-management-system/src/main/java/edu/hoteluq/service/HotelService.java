package edu.hoteluq.service;

import edu.hoteluq.enums.*;
import edu.hoteluq.exceptions.*;
import edu.hoteluq.factory.RoomCreatorRegistry;
import edu.hoteluq.model.*;
import edu.hoteluq.security.Session;
import edu.hoteluq.singleton.Hotel;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import static edu.hoteluq.service.Views.*;

/** Application facade for a local Java application; not a web controller. */
public final class HotelService {
  private final Hotel hotel;
  private final RoomCreatorRegistry creators = new RoomCreatorRegistry();
  public HotelService() { this(Hotel.getInstance()); }
  public HotelService(Hotel hotel) { this.hotel = Objects.requireNonNull(hotel); }

  /** Only trusted startup code calls this once, using operator-supplied credentials. */
  public void initializeAdministrator(String id, String name, String email, String password, Duration sessionLifetime) {
    hotel.initializeAdministrator(id, name, email, password, sessionLifetime);
  }
  public UserView register(String id, String name, String email, String password) {
    return hotel.registerClient(id, name, email, password);
  }
  public boolean initialized() { return hotel.isInitialized(); }
  public Session login(String email, String password) { return hotel.login(email, password); }
  public void logout(String token) { hotel.logout(token); }

  private <T> T authenticated(String token, Supplier<T> call) { return hotel.withSession(token, call); }
  private <T> T admin(String token, Supplier<T> call) {
    return authenticated(token, () -> {
      if (hotel.currentUser().getRole() != Role.ADMINISTRATOR) throw new AuthorizationException();
      return call.get();
    });
  }

  public UserView me(String token) { return authenticated(token, () -> user(hotel.currentUser())); }
  public UserView getUser(String token, String id) { return authenticated(token, () -> user(hotel.findUser(id))); }
  public List<UserView> clients(String token) { return admin(token, () -> hotel.getClients().stream().map(Views::user).toList()); }
  public UserView createClient(String token, String id, String name, String email, String password) {
    return admin(token, () -> {
      Client client = new Client(id, name, email, password);
      hotel.addUser(client);
      return user(client);
    });
  }
  public UserView updateUser(String token, String id, String name, String email) {
    return authenticated(token, () -> { hotel.updateUser(id, name, email); return user(hotel.findUser(id)); });
  }
  public void changePassword(String token, String password) {
    authenticated(token, () -> { hotel.currentUser().setPassword(password); return null; });
  }
  public void resetClientPassword(String token, String id, String password) {
    admin(token, () -> { client(id).setPassword(password); return null; });
  }
  public void deleteClient(String token, String id) { admin(token, () -> { hotel.removeClient(id); return null; }); }

  private Client client(String id) {
    User user = hotel.findUser(id);
    if (!(user instanceof Client client)) throw new HotelException("User is not a client");
    return client;
  }
  public Hotel.RechargeRequest requestRecharge(String token, UUID requestId, java.math.BigDecimal amount, String reason) {
    return authenticated(token, () -> hotel.requestRecharge(requestId, amount, reason));
  }


  public Hotel.ServicePurchase purchaseService(
          String token,
          UUID operationId,
          UUID serviceId
  ) {
    return authenticated(
            token,
            () -> hotel.purchaseService(operationId, serviceId)
    );
  }

  public List<Hotel.ServicePurchase> servicePurchases(
          String token
  ) {
    return authenticated(
            token,
            hotel::servicePurchases
    );
  }

  public List<Hotel.RechargeRequest> rechargeRequests(String token) {
    return authenticated(token, hotel::rechargeRequests);
  }
  public Hotel.RechargeRequest resolveRecharge(String token, UUID requestId, boolean approve) {
    return admin(token, () -> hotel.resolveRecharge(requestId, approve));
  }
  public CardView card(String token, String clientId) { return authenticated(token, () -> Views.card(client(clientId).getCard())); }
  public CardView recharge(String token, String clientId, double amount, String description) {
    return admin(token, () -> {
      VirtualCard card = client(clientId).getCard();
      card.recharge(amount, description);
      return Views.card(card);
    });
  }
  public CardView recharge(String token, String clientId, java.math.BigDecimal amount, String currency, String description) {
    return admin(token, () -> {
      VirtualCard card = client(clientId).getCard(); card.recharge(amount, currency, description); return Views.card(card);
    });
  }
  public RoomTypeView createRoomType(String token, RoomCategory category, String name, java.math.BigDecimal price,
      String currency, int capacity, String description) {
    return admin(token, () -> {
      edu.hoteluq.money.CopMoney.currency(currency);
      var type = new RoomType(category, price, name, capacity, description); hotel.addRoomType(type); return Views.type(type);
    });
  }
  public ServiceView createService(String token, String name, java.math.BigDecimal price, String currency) {
    return admin(token, () -> {
      edu.hoteluq.money.CopMoney.currency(currency);
      var service = new AdditionalService(name, price); hotel.addService(service); return Views.service(service);
    });
  }
  public void setCardActive(String token, String id, boolean active) {
    admin(token, () -> { client(id).getCard().setActive(active); return null; });
  }
  public List<ReservationView> history(String token, String clientId) {
    return authenticated(token, () -> {
      Client client = client(clientId);
      return hotel.getReservations().stream().filter(reservation -> reservation.getClient() == client).map(Views::reservation).toList();
    });
  }

  public List<RoomTypeView> roomTypes(String token) {
    return authenticated(token, () -> hotel.getRoomTypes().stream().map(Views::type).toList());
  }
  public RoomTypeView createRoomType(String token, RoomCategory category, String name, double price, int capacity, String description) {
    return admin(token, () -> {
      RoomType type = new RoomType(category, price, name, capacity, description);
      hotel.addRoomType(type);
      return type(type);
    });
  }
  public RoomTypeView updateRoomType(String token, RoomCategory category, String name, double price, int capacity, String description) {
    return admin(token, () -> {
      RoomType type = hotel.getRoomType(category);
      type.updateDetails(name, price, capacity, description);
      return type(type);
    });
  }
  public void setRoomTypeActive(String token, RoomCategory category, boolean active) {
    admin(token, () -> { hotel.getRoomType(category).setActive(active); return null; });
  }
  public RoomTypeView updateRoomType(String token,RoomCategory category,String name,java.math.BigDecimal price,String currency,int capacity,String description) {
    return admin(token,()->{edu.hoteluq.money.CopMoney.currency(currency);var type=hotel.getRoomType(category);type.updateDetails(name,price,capacity,description);return type(type);});
  }
  public ServiceView updateService(String token,UUID id,String name,java.math.BigDecimal price,String currency) {
    return admin(token,()->{edu.hoteluq.money.CopMoney.currency(currency);var value=hotel.findService(id);value.updateDetails(name,price);return service(value);});
  }
  public void deleteRoomType(String token, RoomCategory category) { admin(token, () -> { hotel.removeRoomType(category); return null; }); }

  public List<RoomView> rooms(String token) { return authenticated(token, () -> hotel.getRooms().stream().map(Views::room).toList()); }
  public List<RoomView> availableRooms(String token, LocalDate from, LocalDate to) {
    return authenticated(token, () -> hotel.getAvailableRooms(from, to).stream().map(Views::room).toList());
  }
  public RoomView createRoom(String token, RoomCategory category, int number, int floor) {
    return admin(token, () -> room(creators.get(category).create(hotel, number, floor)));
  }
  private Room roomByNumber(int number) {
    Room room = hotel.findRoom(number);
    if (room == null) throw new HotelException("Room not found");
    return room;
  }
  public RoomView updateRoom(String token, int number, int floor, boolean active) {
    return admin(token, () -> { Room room = roomByNumber(number); room.updateDetails(floor, active); return room(room); });
  }
  public void setRoomActive(String token, int number, boolean active) {
    admin(token, () -> { roomByNumber(number).setActive(active); return null; });
  }
  public void deleteRoom(String token, int number) { admin(token, () -> { hotel.removeRoom(number); return null; }); }

  public List<ServiceView> services(String token) { return authenticated(token, () -> hotel.getServices().stream().map(Views::service).toList()); }
  public ServiceView createService(String token, String name, double price) {
    return admin(token, () -> { AdditionalService service = new AdditionalService(name, price); hotel.addService(service); return service(service); });
  }
  public ServiceView updateService(String token, UUID id, String name, double price) {
    return admin(token, () -> { AdditionalService service = hotel.findService(id); service.updateDetails(name, price); return service(service); });
  }
  public void setServiceAvailable(String token, UUID id, boolean available) {
    admin(token, () -> { hotel.findService(id).setAvailable(available); return null; });
  }
  public void deleteService(String token, UUID id) { admin(token, () -> { hotel.removeService(id); return null; }); }

  public List<PromotionView> promotions(String token) { return authenticated(token, () -> hotel.getPromotions().stream().map(Views::promotion).toList()); }
  public PromotionView createPromotion(String token, String name, double percent) {
    return admin(token, () -> { Promotion promotion = new Promotion(name, percent); hotel.addPromotion(promotion); return promotion(promotion); });
  }
  public PromotionView updatePromotion(String token, UUID id, String name, double percent) {
    return admin(token, () -> { Promotion promotion = hotel.findPromotion(id); promotion.updateDetails(name, percent); return promotion(promotion); });
  }
  public void deletePromotion(String token, UUID id) { admin(token, () -> { hotel.removePromotion(id); return null; }); }
}
