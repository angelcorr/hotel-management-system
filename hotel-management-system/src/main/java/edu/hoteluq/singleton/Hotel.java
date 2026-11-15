package edu.hoteluq.singleton;

import edu.hoteluq.enums.*;
import edu.hoteluq.exceptions.*;
import edu.hoteluq.model.*;
import edu.hoteluq.security.*;
import edu.hoteluq.service.Views;
import edu.hoteluq.validation.DomainValidation;

import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;

/** In-memory aggregate. Its monitor serializes authorization, uniqueness checks and writes. */
public class Hotel {
  private static class Holder { private static final Hotel INSTANCE = new Hotel(); }
  private final List<RoomType> roomTypes = new ArrayList<>();
  private final List<Room> rooms = new ArrayList<>();
  private final List<User> users = new ArrayList<>();
  private final List<AdditionalService> services = new ArrayList<>();
  private final List<Promotion> promotions = new ArrayList<>();
  private final List<Reservation> reservations = new ArrayList<>();
  private final Map<String, SessionState> sessions = new HashMap<>();
  private final SecureRandom random = new SecureRandom();
  private final Clock clock;
  private Duration sessionLifetime;
  private final ThreadLocal<Frame> context = new ThreadLocal<>();
  private final ThreadLocal<Integer> internalDepth = ThreadLocal.withInitial(() -> 0);
  private final Client dummy = new Client(UUID.randomUUID().toString(), "Authentication", "authentication@example.invalid", UUID.randomUUID().toString());
  private boolean initialized;

  private record SessionState(User user, long credentialVersion, Instant expiresAt) {}
  private record Frame(String token, Frame previous) {}

  private Hotel() { this(Clock.systemUTC(), Duration.ofMinutes(30)); }

  // Package-private injection for deterministic integration tests; public access remains Singleton.
  Hotel(Clock clock, Duration sessionLifetime) {
    this.clock = DomainValidation.required(clock, "Clock");
    this.sessionLifetime = validLifetime(sessionLifetime);
  }

  public static Hotel getInstance() { return Holder.INSTANCE; }

  private static Duration validLifetime(Duration lifetime) {
    if (lifetime == null || lifetime.isNegative() || lifetime.isZero()) throw new HotelException("Session lifetime must be positive");
    return lifetime;
  }

  /** Trusted local startup only, before accepting users; never expose this as a registration endpoint. */
  public synchronized void initializeAdministrator(String id, String name, String email, String password) {
    initializeAdministrator(id, name, email, password, sessionLifetime);
  }

  public synchronized void initializeAdministrator(String id, String name, String email, String password, Duration lifetime) {
    if (initialized || !users.isEmpty()) throw new AuthorizationException();
    Duration valid = validLifetime(lifetime);
    Administrator administrator = new Administrator(id, name, email, password);
    internal(() -> { insertUser(administrator); return null; });
    sessionLifetime = valid;
    initialized = true;
  }

  public synchronized Views.UserView registerClient(String id, String name, String email, String password) {
    if (!initialized) throw new HotelException("Trusted administrator initialization is required first");
    Client client = new Client(id, name, email, password);
    return internal(() -> { insertUser(client); return Views.user(client); });
  }

  public synchronized Session login(String email, String password) {
    String normalized;
    try { normalized = DomainValidation.email(email); }
    catch (HotelException ex) {
      dummy.checkPassword(password);
      throw new AuthenticationException("Invalid credentials");
    }
    User user = internal(() -> rawFindEmail(normalized));
    boolean valid = internal(() -> (user == null ? dummy : user).checkPassword(password));
    if (!valid || user == null) throw new AuthenticationException("Invalid credentials");
    sessions.values().removeIf(session -> !clock.instant().isBefore(session.expiresAt()));
    byte[] bytes = new byte[32];
    String token;
    do {
      random.nextBytes(bytes);
      token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    } while (sessions.containsKey(token));
    Instant expires = clock.instant().plus(sessionLifetime);
    sessions.put(token, new SessionState(user, user.getCredentialVersion(), expires));
    return new Session(token, expires);
  }

  public synchronized void logout(String token) {
    resolve(token);
    sessions.remove(token);
  }

  private User resolve(String token) {
    SessionState session = token == null ? null : sessions.get(token);
    if (session == null) throw new AuthenticationException("Invalid or expired session");
    if (!clock.instant().isBefore(session.expiresAt()) || !users.contains(session.user())
        || session.user().getCredentialVersion() != session.credentialVersion()) {
      sessions.remove(token);
      throw new AuthenticationException("Invalid or expired session");
    }
    return session.user();
  }

  /** Scope is thread-bound, cannot be inherited by workers, and rechecks validity on every operation. */
  public synchronized SessionScope openSession(String token) {
    resolve(token);
    Frame frame = new Frame(token, context.get());
    context.set(frame);
    return new SessionScope(frame);
  }

  public final class SessionScope implements AutoCloseable {
    private final Frame frame;
    private final Thread owner = Thread.currentThread();
    private boolean closed;
    private SessionScope(Frame frame) { this.frame = frame; }
    @Override public void close() {
      if (Thread.currentThread() != owner) throw new IllegalStateException("Session scope belongs to another thread");
      if (closed) return;
      if (context.get() != frame) throw new IllegalStateException("Close session scopes in reverse order");
      if (frame.previous() == null) context.remove(); else context.set(frame.previous());
      closed = true;
    }
  }

  public synchronized <T> T withSession(String token, Supplier<T> operation) {
    try (SessionScope ignored = openSession(token)) { return operation.get(); }
  }

  private <T> T internal(Supplier<T> operation) {
    int previous = internalDepth.get();
    internalDepth.set(previous + 1);
    try { return operation.get(); }
    finally { if (previous == 0) internalDepth.remove(); else internalDepth.set(previous); }
  }

  public synchronized User currentUser() { return actor(); }

  private User actor() {
    Frame frame = context.get();
    return resolve(frame == null ? null : frame.token());
  }

  private void requireAdmin() {
    if (internalDepth.get() == 0 && actor().getRole() != Role.ADMINISTRATOR) throw new AuthorizationException();
  }

  private void requireOwner(User owner) {
    if (internalDepth.get() != 0) return;
    User actor = actor();
    if (actor != owner && actor.getRole() != Role.ADMINISTRATOR) throw new AuthorizationException();
  }

  private User rawFindEmail(String email) {
    return users.stream().filter(user -> user.getEmail().equalsIgnoreCase(email)).findFirst().orElse(null);
  }

  private void uniqueEmail(User except, String email) {
    User existing = internal(() -> rawFindEmail(DomainValidation.email(email)));
    if (existing != null && existing != except) throw new HotelException("Email is already registered");
  }

  private static boolean sameName(String a, String b) { return a.strip().equalsIgnoreCase(b.strip()); }

  private void uniqueName(Object except, String name) {
    String valid = DomainValidation.text(name, "Name");
    boolean duplicate;
    if (except instanceof RoomType) duplicate = roomTypes.stream().anyMatch(type -> type != except && sameName(type.getName(), valid));
    else if (except instanceof AdditionalService) duplicate = services.stream().anyMatch(service -> service != except && sameName(service.getName(), valid));
    else duplicate = promotions.stream().anyMatch(promotion -> promotion != except && sameName(promotion.getName(), valid));
    if (duplicate) throw new HotelException("Name is already registered in this catalog");
  }

  private boolean referenced(Object subject) {
    return internal(() -> reservations.stream().anyMatch(reservation ->
        reservation.getClient() == subject || reservation.getRoom() == subject
        || reservation.getRoom().getType() == subject || reservation.getPromotion() == subject
        || reservation.getServices().contains(subject)));
  }

  private boolean registered(ManagedEntity subject, User owner) {
    if (subject instanceof VirtualCard) return users.contains(owner);
    return users.contains(subject) || rooms.contains(subject) || roomTypes.contains(subject)
        || services.contains(subject) || promotions.contains(subject) || reservations.contains(subject);
  }

  private void ensureDetached(ManagedEntity subject) {
    if (subject.isManaged()) throw new HotelException("Entity is already managed");
  }

  private void bind(ManagedEntity subject, User owner) {
    subject.attachGuard(new EntityGuard() {
      @Override public void mutate(Action action, Object candidate, Runnable mutation) {
        synchronized (Hotel.this) {
          if (!registered(subject, owner)) throw new HotelException("Entity is no longer registered");
          if (subject instanceof User) {
            requireOwner(owner);
            if (action == Action.EMAIL) uniqueEmail(owner, (String) candidate);
          } else if (subject instanceof VirtualCard) {
            if (action == Action.CHARGE) requireOwner(owner); else requireAdmin();
          } else {
            requireAdmin();
            if (action != Action.AVAILABILITY && !(subject instanceof Reservation) && referenced(subject)) {
              throw new HotelException("Referenced catalog details cannot be changed before historical snapshots are supported");
            }
            if (action == Action.NAME) uniqueName(subject, (String) candidate);
          }
          mutation.run();
          if (subject instanceof User && action == Action.PASSWORD) sessions.values().removeIf(session -> session.user() == owner);
        }
      }
      @Override public <T> T read(Supplier<T> query) {
        synchronized (Hotel.this) {
          if (owner != null) {
            requireOwner(owner);
            if (!registered(subject, owner)) throw new HotelException("Entity is no longer registered");
          }
          return query.get();
        }
      }
    });
  }

  private void insertUser(User user) {
    DomainValidation.required(user, "User");
    if (user.getClass() != Client.class && user.getClass() != Administrator.class) throw new AuthorizationException();
    ensureDetached(user);
    if (users.stream().anyMatch(existing -> existing.getId().equals(user.getId()))) throw new HotelException("Id is already registered");
    uniqueEmail(null, user.getEmail());
    VirtualCard card = user instanceof Client client ? client.getCard() : null;
    if (card != null) ensureDetached(card);
    bind(user, user);
    if (card != null) bind(card, user);
    users.add(user);
  }

  public synchronized void addUser(User user) {
    requireAdmin();
    DomainValidation.required(user, "User");
    // Administrator provisioning is deliberately not part of client CRUD.
    if (!(user instanceof Client)) throw new AuthorizationException();
    internal(() -> { insertUser(user); return null; });
  }

  public synchronized User findUserByEmail(String email) {
    User user = internal(() -> rawFindEmail(DomainValidation.email(email)));
    requireOwner(user);
    return user;
  }

  public synchronized User findUser(String id) {
    String valid = DomainValidation.text(id, "User id");
    User user = internal(() -> users.stream().filter(value -> value.getId().equals(valid)).findFirst().orElse(null));
    requireOwner(user);
    if (user == null) throw new HotelException("User not found");
    return user;
  }

  public synchronized List<Client> getClients() {
    requireAdmin();
    return users.stream().filter(Client.class::isInstance).map(Client.class::cast).toList();
  }

  public synchronized void updateUser(String id, String name, String email) {
    User user = findUser(id);
    requireOwner(user);
    String validName = DomainValidation.text(name, "Name");
    String validEmail = DomainValidation.email(email);
    uniqueEmail(user, validEmail);
    user.updateProfile(validName, validEmail);
  }

  public synchronized void removeClient(String id) {
    requireAdmin();
    User user = findUser(id);
    if (!(user instanceof Client client)) throw new AuthorizationException();
    if (referenced(user) || client.getCard().getBalance() != 0 || !client.getCard().getMovements().isEmpty()) {
      throw new HotelException("Client has reservation or financial records");
    }
    users.remove(user);
    sessions.values().removeIf(session -> session.user() == user);
  }

  public synchronized void addRoomType(RoomType type) {
    requireAdmin();
    DomainValidation.required(type, "Room type");
    ensureDetached(type);
    if (roomTypes.stream().anyMatch(existing -> existing.getCategory() == type.getCategory())) throw new HotelException("Room category is already registered");
    uniqueName(type, type.getName());
    bind(type, null);
    roomTypes.add(type);
  }

  public synchronized RoomType getRoomType(RoomCategory category) {
    DomainValidation.required(category, "Category");
    return roomTypes.stream().filter(type -> type.getCategory() == category).findFirst()
        .orElseThrow(() -> new HotelException("Room type not found"));
  }

  public synchronized List<RoomType> getRoomTypes() { return List.copyOf(roomTypes); }

  public synchronized void removeRoomType(RoomCategory category) {
    requireAdmin();
    RoomType type = getRoomType(category);
    if (rooms.stream().anyMatch(room -> room.getType() == type) || referenced(type)) throw new HotelException("Room type is referenced");
    roomTypes.remove(type);
  }

  public synchronized void addRoom(Room room) {
    requireAdmin();
    DomainValidation.required(room, "Room");
    ensureDetached(room);
    if (!roomTypes.contains(room.getType())) throw new HotelException("Room type is not registered");
    if (findRoom(room.getNumber()) != null) throw new DuplicateRoomException("Room number is already registered");
    bind(room, null);
    rooms.add(room);
  }

  public synchronized Room findRoom(int number) {
    return rooms.stream().filter(room -> room.getNumber() == number).findFirst().orElse(null);
  }

  public synchronized List<Room> getRooms() { return List.copyOf(rooms); }

  public synchronized void removeRoom(int number) {
    requireAdmin();
    Room room = findRoom(number);
    if (room == null) throw new HotelException("Room not found");
    if (referenced(room)) throw new HotelException("Room has reservations");
    rooms.remove(room);
  }

  public synchronized void addService(AdditionalService service) {
    requireAdmin();
    DomainValidation.required(service, "Service");
    ensureDetached(service);
    uniqueName(service, service.getName());
    bind(service, null);
    services.add(service);
  }

  public synchronized AdditionalService findService(UUID id) {
    return services.stream().filter(service -> service.getId().equals(id)).findFirst().orElseThrow(() -> new HotelException("Service not found"));
  }

  public synchronized List<AdditionalService> getServices() { return List.copyOf(services); }

  public synchronized void removeService(UUID id) {
    requireAdmin();
    AdditionalService service = findService(id);
    if (referenced(service)) throw new HotelException("Service has reservations");
    services.remove(service);
  }

  public synchronized void addPromotion(Promotion promotion) {
    requireAdmin();
    DomainValidation.required(promotion, "Promotion");
    ensureDetached(promotion);
    uniqueName(promotion, promotion.getName());
    bind(promotion, null);
    promotions.add(promotion);
  }

  public synchronized Promotion findPromotion(UUID id) {
    return promotions.stream().filter(promotion -> promotion.getId().equals(id)).findFirst().orElseThrow(() -> new HotelException("Promotion not found"));
  }

  public synchronized List<Promotion> getPromotions() { return List.copyOf(promotions); }

  public synchronized void removePromotion(UUID id) {
    requireAdmin();
    Promotion promotion = findPromotion(id);
    if (referenced(promotion)) throw new HotelException("Promotion has reservations");
    promotions.remove(promotion);
  }

  public synchronized boolean isAvailable(Room room, LocalDate from, LocalDate to) {
    DomainValidation.period(from, to);
    DomainValidation.required(room, "Room");
    if (!rooms.contains(room)) throw new HotelException("Room is not registered");
    return internal(() -> room.isActive() && reservations.stream().noneMatch(reservation ->
        reservation.getRoom() == room && reservation.overlaps(from, to)));
  }

  public synchronized List<Room> getAvailableRooms(LocalDate from, LocalDate to) {
    DomainValidation.period(from, to);
    return rooms.stream().filter(room -> isAvailable(room, from, to)).toList();
  }

  public synchronized void addReservation(Reservation reservation) {
    DomainValidation.required(reservation, "Reservation");
    synchronized (reservation) {
      requireOwner(reservation.getClient());
      ensureDetached(reservation);
      if (reservation.getStatus() != ReservationStatus.PENDING) {
        throw new InvalidReservationException("New reservations must start pending; registration does not confirm payment");
      }
      if (!users.contains(reservation.getClient())) throw new HotelException("Client is not registered");
      if (reservation.getGuests() > reservation.getRoom().getCapacity()) throw new HotelException("Room capacity changed");
      for (AdditionalService service : reservation.getServices()) {
        if (!services.contains(service) || !service.isAvailable()) throw new HotelException("Service is not registered or unavailable");
      }
      if (reservation.getPromotion() != null && !promotions.contains(reservation.getPromotion())) throw new HotelException("Promotion is not registered");
      if (!isAvailable(reservation.getRoom(), reservation.getCheckIn(), reservation.getCheckOut())) throw new RoomNotAvailableException("Room is not available for this period");
      bind(reservation, reservation.getClient());
      reservations.add(reservation);
    }
  }

  public synchronized List<Reservation> getReservations() {
    User actor = actor();
    return internal(() -> reservations.stream().filter(reservation -> actor.getRole() == Role.ADMINISTRATOR || reservation.getClient() == actor).toList());
  }
}
