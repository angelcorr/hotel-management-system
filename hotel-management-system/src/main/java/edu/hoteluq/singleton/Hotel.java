package edu.hoteluq.singleton;

import edu.hoteluq.enums.*;
import edu.hoteluq.loyalty.*;
import edu.hoteluq.promotion.*;
import edu.hoteluq.exceptions.*;
import edu.hoteluq.model.*;
import edu.hoteluq.security.*;
import edu.hoteluq.service.Views;
import edu.hoteluq.service.BookingRequest;
import edu.hoteluq.service.BookingRules;
import edu.hoteluq.model.BookingRecords.*;
import edu.hoteluq.strategy.*;
import java.math.BigDecimal;
import edu.hoteluq.money.CopMoney;
import edu.hoteluq.config.HotelTime;
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

  private final Map<UUID, ServicePurchase> servicePurchases =
          new LinkedHashMap<>();

  private boolean servicePurchaseTransaction;

  public record ServicePurchase(
          UUID id,
          String clientId,
          UUID serviceId,
          String serviceName,
          BigDecimal amount,
          UUID movementId,
          Instant purchasedAt
  ) {}

  private final Map<UUID, RechargeRequest> rechargeRequests = new LinkedHashMap<>();
  public enum RechargeStatus { PENDING, APPROVED, REJECTED }
  public record RechargeRequest(UUID id, String clientId, BigDecimal amount, String reason,
                                RechargeStatus status, Instant requestedAt, Instant resolvedAt) {}

  private final PointsLedger points = new PointsLedger();
  private final PromotionProgram promotionProgram = new PromotionProgram();
  private boolean promotionMutation;
  private final LoyaltyCatalog loyaltyCatalog = new LoyaltyCatalog(this::findService);
  private final Map<String, SessionState> sessions = new HashMap<>();
  private final SecureRandom random = new SecureRandom();
  private final Clock clock;
  private Duration sessionLifetime;
  private final ThreadLocal<Frame> context = new ThreadLocal<>();
  private final ThreadLocal<Integer> internalDepth = ThreadLocal.withInitial(() -> 0);
  private final Client dummy = new Client(UUID.randomUUID().toString(), "Authentication", "authentication@example.invalid", UUID.randomUUID().toString());
  private boolean initialized;
  private BookingRules bookingRules;
  private boolean bookingTransaction;
  private CancellationSchedule cancellationSchedule = CancellationSchedule.official();
  private final Map<String, CancellationSchedule> cancellationPolicies = new HashMap<>();
  private final List<PolicyChange> policyChanges = new ArrayList<>();
  private final Map<String, Creation> creationKeys = new HashMap<>();
  private record Creation(BookingRequest request, Reservation reservation) {}
  // Package-private injection only for deterministic failure tests. Never supplied by request callers.
  enum Stage { AFTER_TRANSFER, AFTER_STATE, AFTER_RECEIPT }
  java.util.function.Consumer<Stage> transactionProbe = stage -> {};


  private record SessionState(User user, long credentialVersion, Instant expiresAt) {}
  private record Frame(String token, Frame previous) {}

  private Hotel() { this(HotelTime.systemClock(), Duration.ofMinutes(30)); }

  // Package-private injection for deterministic integration tests; public access remains Singleton.
  Hotel(Clock clock, Duration sessionLifetime) {
    this.clock = HotelTime.inHotelZone(DomainValidation.required(clock, "Clock"));
    this.sessionLifetime = validLifetime(sessionLifetime);
    cancellationPolicies.put(cancellationSchedule.id() + "/" + cancellationSchedule.version(), cancellationSchedule);
  }

  public synchronized boolean isInitialized() { return initialized; }

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
    return internal(() -> {
      boolean reservationReference =
              reservations.stream().anyMatch(reservation ->
                      reservation.getClient() == subject
                              || reservation.getRoom() == subject
                              || reservation.getRoom().getType() == subject
                              || reservation.getPromotion() == subject
                              || reservation.getServices().contains(subject)
              );

      boolean purchaseReference =
              subject instanceof AdditionalService service
                      && servicePurchases.values().stream().anyMatch(
                      purchase ->
                              purchase.serviceId().equals(service.getId())
              );

      return reservationReference || purchaseReference;
    });
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
          if (action == Action.BOOKING_TRANSACTION) {
            if (!bookingTransaction) { requireAdmin(); throw new AuthorizationException(); }
          } else if (subject instanceof User) {
            requireOwner(owner);
            if (action == Action.EMAIL) uniqueEmail(owner, (String) candidate);
          } else if (subject instanceof VirtualCard) {

            if (action == Action.SERVICE_PURCHASE) {
              if (!servicePurchaseTransaction) {
                throw new AuthorizationException();
              }

              if (actor() != owner || !(owner instanceof Client)) {
                throw new AuthorizationException();
              }

              mutation.run();
              return;
            }

            if (action == Action.CHARGE || action == Action.REFUND) {
              if (action == Action.CHARGE) requireOwner(owner); else requireAdmin();
              throw new InvalidReservationException("Use a reservation-linked payment or cancellation");
            }
            requireAdmin();
          } else {
            requireAdmin();
            if (subject instanceof Reservation && !bookingTransaction) {
              throw new InvalidReservationException("Use the reservation service for state transitions");
            }
            if(subject instanceof Promotion p && promotionProgram.configured(p.getId()) && !promotionMutation)
              throw new HotelException("Use the versioned promotion service");
            if (action != Action.AVAILABILITY && !(subject instanceof Reservation) && !(subject instanceof Promotion && promotionMutation) && referenced(subject)) {
              throw new HotelException("Referenced catalog details are protected");
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
    if (card != null) { ensureDetached(card); card.configureClock(clock); }
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
    if (referenced(user) || rechargeRequests.values().stream().anyMatch(r -> r.clientId().equals(id))
        || client.getCard().getBalance() != 0 || !client.getCard().getMovements().isEmpty()) {
      throw new HotelException("Client has reservation or financial records");
    }
    users.remove(user);
    sessions.values().removeIf(session -> session.user() == user);
  }

  /** Client may request a top-up, but cannot credit their own card. The key makes retries safe. */
  public synchronized RechargeRequest requestRecharge(UUID key, BigDecimal amount, String reason) {
    User requester = actor();
    if (!(requester instanceof Client)) throw new AuthorizationException();
    UUID validKey = DomainValidation.required(key, "Request id");
    BigDecimal value = CopMoney.positive(amount);
    String detail = DomainValidation.text(reason, "Reason");
    RechargeRequest existing = rechargeRequests.get(validKey);
    if (existing != null) {
      if (!existing.clientId().equals(requester.getId()) || existing.amount().compareTo(value) != 0
          || !existing.reason().equals(detail)) throw new HotelException("Conflicting recharge request key");
      return existing;
    }
    RechargeRequest request = new RechargeRequest(validKey, requester.getId(), value, detail,
        RechargeStatus.PENDING, clock.instant(), null);
    rechargeRequests.put(validKey, request);
    return request;
  }

  /** Administrators see all requests; a client only sees their own. */
  public synchronized List<RechargeRequest> rechargeRequests() {
    User requester = actor();
    return rechargeRequests.values().stream()
        .filter(r -> requester.getRole() == Role.ADMINISTRATOR || r.clientId().equals(requester.getId()))
        .toList();
  }

  /** Approval credits exactly once, including when clients or admins retry the same request. */
  public synchronized RechargeRequest resolveRecharge(UUID id, boolean approve) {
    requireAdmin();
    RechargeRequest existing = rechargeRequests.get(DomainValidation.required(id, "Request id"));
    if (existing == null) throw new HotelException("Recharge request not found");
    RechargeStatus target = approve ? RechargeStatus.APPROVED : RechargeStatus.REJECTED;
    if (existing.status() != RechargeStatus.PENDING) {
      if (existing.status() == target) return existing;
      throw new HotelException("Recharge request already resolved");
    }
    if (approve) {
      Client client = (Client) findUser(existing.clientId());
      // Only mark APPROVED after the card has actually been credited.
      client.getCard().recharge(existing.amount(), CopMoney.CURRENCY,
          "Approved request " + id + ": " + existing.reason());
    }
    RechargeRequest resolved = new RechargeRequest(existing.id(), existing.clientId(), existing.amount(),
        existing.reason(), target, existing.requestedAt(), clock.instant());
    rechargeRequests.put(id, resolved);
    return resolved;
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


  /**
   * Compra autónoma de un servicio, sin reserva.
   * El UUID permite reintentos sin duplicar el cargo.
   */
  public synchronized ServicePurchase purchaseService(
          UUID operationId,
          UUID serviceId
  ) {
    User requester = actor();

    if (!(requester instanceof Client client)) {
      throw new AuthorizationException();
    }

    UUID key = DomainValidation.required(
            operationId, "Purchase operation id"
    );

    UUID selectedId = DomainValidation.required(
            serviceId, "Service id"
    );

    ServicePurchase previous = servicePurchases.get(key);

    if (previous != null) {
      if (!previous.clientId().equals(client.getId())
              || !previous.serviceId().equals(selectedId)) {
        throw new HotelException(
                "Conflicting service purchase operation"
        );
      }

      return previous;
    }

    AdditionalService selected = findService(selectedId);

    if (!selected.isAvailable()) {
      throw new HotelException(
              "Selected service is unavailable"
      );
    }

    BigDecimal total = CopMoney.positive(
            selected.getPriceAmount()
    );

    VirtualCard card = client.getCard();

    if (servicePurchaseTransaction) {
      throw new HotelException(
              "Nested service purchase is not allowed"
      );
    }

    servicePurchaseTransaction = true;

    try {
      card.purchaseService(
              total,
              "Service purchase " + key
                      + " - " + selected.getName()
      );
    } finally {
      servicePurchaseTransaction = false;
    }

    UUID movementId = card.getMovements()
            .getLast()
            .getId();

    ServicePurchase purchase = new ServicePurchase(
            key,
            client.getId(),
            selectedId,
            selected.getName(),
            total,
            movementId,
            clock.instant()
    );

    servicePurchases.put(key, purchase);

    return purchase;
  }

  /**
   * El administrador consulta todas las compras.
   * Cada cliente consulta solamente las suyas.
   */
  public synchronized List<ServicePurchase> servicePurchases() {
    User requester = actor();

    return servicePurchases.values().stream()
            .filter(purchase ->
                    requester.getRole() == Role.ADMINISTRATOR
                            || purchase.clientId().equals(requester.getId())
            )
            .toList();
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
    if (referenced(service) || loyaltyCatalog.references(id) || promotionProgram.referencesService(id)) throw new HotelException("Service has reservations or loyalty references");
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
    if (referenced(promotion) || promotionProgram.configured(id)) throw new HotelException("Promotion has reservations or version history; deactivate instead");
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
      if (bookingRules != null && !clock.instant().isBefore(reservation.getCheckIn().atTime(bookingRules.checkInTime())
          .atZone(bookingRules.zone()).toInstant())) throw new InvalidReservationException("Check-in has already started");
      ensureDetached(reservation);
      if (reservation.getState().getClass() != edu.hoteluq.state.PendingState.class) {
        throw new InvalidReservationException("New reservations must start pending; registration does not confirm payment");
      }
      if (!users.contains(reservation.getClient())) throw new HotelException("Client is not registered");
      if (reservation.getGuests() > reservation.getRoom().getCapacity()) throw new HotelException("Room capacity changed");
      for (AdditionalService service : reservation.getServices()) {
        if (!services.contains(service) || !service.isAvailable()) throw new HotelException("Service is not registered or unavailable");
      }
      if (reservation.getPromotion() != null && !promotions.contains(reservation.getPromotion())) throw new HotelException("Promotion is not registered");
      validatePackageSelection(reservation);
      if(reservation.getPromotionDefinition()!=null || (reservation.getPromotion()!=null && promotionProgram.configured(reservation.getPromotion().getId())))
        validatePromotion(reservation,clock.instant());
      if (!isAvailable(reservation.getRoom(), reservation.getCheckIn(), reservation.getCheckOut())) throw new RoomNotAvailableException("Room is not available for this period");
      bind(reservation, reservation.getClient());
      reservations.add(reservation);
      try {
        bookingAtomic(reservation, () -> {
          reservation.recordSchedule(scheduleFor(reservation));
          audit(reservation, "CREATED", actorId(), "Reservation created", null);
          transactionProbe.accept(Stage.AFTER_RECEIPT);
          return null;
        });
      } catch (RuntimeException | Error failure) {
        reservations.remove(reservation); // failed creation must not retain an orphan availability block
        throw failure;
      }
    }
  }

  private Schedule scheduleFor(Reservation r) {
    if (bookingRules == null) return null;
    return new Schedule(HotelTime.instant(r.getCheckIn(), rules().checkInTime()),
        rules().completionTime() == null ? null : HotelTime.instant(r.getCheckOut(), rules().completionTime()), HotelTime.ZONE);
  }
  private String actorId() { return actor().getId(); }
  private void audit(Reservation r, String type, String who, String reason, Quote before) {
    audit(r, type, who, reason, before, UUID.randomUUID());
  }
  private void audit(Reservation r, String type, String who, String reason, Quote before, UUID operation) {
    audit(r, type, who, reason, before, operation, clock.instant());
  }
  private void audit(Reservation r, String type, String who, String reason, Quote before, UUID operation, Instant at) {
    r.audit(new AuditEvent(operation, type, who, at, r.getVersion(), reason, before, r.getQuote()));
  }
  public synchronized void configureCancellation(CancellationSchedule policy) {
    requireAdmin();
    DomainValidation.required(policy, "Cancellation schedule");
    policy.requireOfficial(); // Validate before changing active policy, registry or activation history.
    String key = policy.id() + "/" + policy.version();
    CancellationSchedule previous = cancellationPolicies.get(key);
    if (previous != null && !previous.equals(policy)) throw new HotelException("Policy version cannot be rewritten");
    if (previous == null && cancellationPolicies.values().stream().anyMatch(p -> p.id().equals(policy.id()) && p.version() >= policy.version())) throw new HotelException("New policy version must increase");
    if (policy.equals(cancellationSchedule)) return;
    if (previous != null) throw new HotelException("Cannot reactivate an older policy version");
    var event = new PolicyChange(UUID.randomUUID(), actorId(), clock.instant(), policy);
    cancellationPolicies.put(key, policy); cancellationSchedule = policy; policyChanges.add(event);
  }
  public synchronized List<PolicyChange> cancellationPolicyHistory() { requireAdmin(); return List.copyOf(policyChanges); }

  /** Configure once from trusted/admin startup. Existing bookings prevent policy replacement. */
  public synchronized void configureBookingRules(BookingRules rules) {
    requireAdmin();
    if (bookingRules != null || !reservations.isEmpty()) throw new HotelException("Booking rules already in use");
    bookingRules = DomainValidation.required(rules, "Booking rules");
  }

  /** Complete a previously absent timing decision without changing any contractual dates or hours. */
  public synchronized void configureCheckoutWindow(BookingRules.CheckoutWindow window) {
    requireAdmin();
    BookingRules existing = rules();
    DomainValidation.required(window, "Explicit checkout window");
    if (existing.checkoutWindow() != null || existing.completionTime() == null) throw new HotelException("Checkout window already configured or contractual checkout hour missing");
    bookingRules = new BookingRules(existing.checkInTime(), existing.completionTime(), window);
  }

  private BookingRules rules() {
    if (bookingRules == null) throw new HotelException("Explicit hotel booking rules must be configured first");
    return bookingRules;
  }

  public synchronized Reservation findReservation(UUID id) {
    actor();
    Reservation reservation = internal(() -> reservations.stream().filter(r -> r.getId().equals(id))
        .findFirst().orElseThrow(() -> new InvalidReservationException("Reservation not found")));
    User owner = internal(reservation::getClient);
    requireOwner(owner);
    return reservation;
  }

  private void requireRegistered(Reservation reservation) {
    actor();
    if (reservation == null || !reservations.contains(reservation)) {
      throw new InvalidReservationException("Reservation is not registered in this hotel");
    }
    requireOwner(internal(reservation::getClient));
  }

  private Reservation draft(Client client, BookingRequest request) {
    rules();
    DomainValidation.required(request, "Booking request");
    if (!clock.instant().isBefore(request.checkIn().atTime(rules().checkInTime()).atZone(rules().zone()).toInstant())) {
      throw new InvalidReservationException("Check-in has already started");
    }
    Room room = findRoom(request.roomNumber());
    if (room == null) throw new InvalidReservationException("Room not found");
    var builder = new Reservation.Builder().client(client).room(room).checkIn(request.checkIn())
        .checkOut(request.checkOut()).guests(request.guests());
    LinkedHashSet<AdditionalService> selected=new LinkedHashSet<>();
    request.serviceIds().forEach(id -> selected.add(findService(id)));
    for(UUID id:request.packageIds()) for(AdditionalService service:loyaltyCatalog.packageServices(id))
      if(!selected.add(service)) throw new InvalidReservationException("Duplicate service across selections/packages");
    selected.forEach(builder::additionalService); builder.packages(request.packageIds());
    if(request.promotionId()!=null) builder.promotion(findPromotion(request.promotionId())).promotionDefinition(promotionProgram.get(request.promotionId()));
    Reservation result=builder.build();
    if(result.getPromotion()!=null) validatePromotion(result,clock.instant());
    return result;
  }

  public synchronized BookingView createReservation(String clientId, String key, BookingRequest request) {
    User user = findUser(clientId);
    if (!(user instanceof Client client)) throw new InvalidReservationException("Client required");
    String normalized = DomainValidation.text(key, "Idempotency key");
    String actualId = client.getId();
    String scopedKey = actualId.length() + ":" + actualId + ":" + normalized;
    DomainValidation.required(request, "Booking request");
    Creation prior = creationKeys.get(scopedKey);
    if (prior != null) {
      if (!prior.request().equals(request)) throw new InvalidReservationException("Idempotency key reused with different data");
      return prior.reservation().view();
    }
    Reservation reservation = draft(client, request);
    addReservation(reservation);
    creationKeys.put(scopedKey, new Creation(request, reservation));
    return reservation.view();
  }

  private boolean availableExcept(Reservation except, Room room, LocalDate from, LocalDate to) {
    return internal(() -> room.isActive() && reservations.stream().noneMatch(r -> r != except
        && r.getRoom() == room && r.overlaps(from, to)));
  }

  /** All mutations and rollback occur while the Hotel monitor excludes readers and competing writers. */
  private <T> T bookingAtomic(Reservation reservation, Supplier<T> operation) {
    if (bookingTransaction) throw new IllegalStateException("Nested booking transaction");
    return internal(() -> {
      VirtualCard card = reservation.getClient().getCard();
      Object reservationBefore = reservation.checkpoint();
      Object cardBefore = card.checkpoint();
      Object pointsBefore = points.checkpoint();
      bookingTransaction = true;
      try { return operation.get(); }
      catch (RuntimeException | Error failure) {
        // Restore exact pre-state, not an extra refund: failed operations leave no financial movement.
        card.restore(cardBefore);
        points.restore(pointsBefore);
        reservation.restore(reservationBefore);
        throw failure;
      } finally { bookingTransaction = false; }
    });
  }

  public synchronized BookingView modifyReservation(UUID id, long expectedVersion, BookingRequest request) {
    Reservation reservation = findReservation(id);
    if (reservation.getStatus() != ReservationStatus.PENDING || reservation.getVersion() != expectedVersion) {
      throw new InvalidReservationException("Only the current pending version can be modified");
    }
    Reservation replacement = draft(reservation.getClient(), request);
    if (!availableExcept(reservation, replacement.getRoom(), replacement.getCheckIn(), replacement.getCheckOut())) {
      throw new RoomNotAvailableException("Room unavailable");
    }
    String who = actorId();
    Quote before = reservation.getQuote();
    return bookingAtomic(reservation, () -> {
      reservation.replacePending(replacement); reservation.recordSchedule(scheduleFor(reservation));
      transactionProbe.accept(Stage.AFTER_STATE);
      audit(reservation, "PENDING_MODIFIED", who, "Pending conditions changed", before);
      transactionProbe.accept(Stage.AFTER_RECEIPT);
      return reservation.view();
    });
  }

  public synchronized BookingView confirmReservation(UUID id,long expectedVersion) {
    Reservation r=findReservation(id);
    if(r.getStatus()==ReservationStatus.PENDING && r.getVersion()!=expectedVersion) throw new InvalidReservationException("Stale reservation version");
    return confirmReservation(r);
  }
  public synchronized BookingRules bookingConfiguration() { actor(); return bookingRules; }
  public synchronized Quote previewPendingModification(UUID id,long version,BookingRequest request) {
    Reservation r=findReservation(id);
    if(r.getStatus()!=ReservationStatus.PENDING || r.getVersion()!=version) throw new InvalidReservationException("Stale reservation version");
    Reservation draft=draft(r.getClient(),request);
    if(!availableExcept(r,draft.getRoom(),draft.getCheckIn(),draft.getCheckOut())) throw new RoomNotAvailableException("Room unavailable");
    return draft.getQuote();
  }
  public synchronized Quote previewBooking(String clientId,BookingRequest request) {
    User user=findUser(clientId);
    if(!(user instanceof Client client)) throw new InvalidReservationException("Client required");
    Reservation draft=draft(client,request);
    if(!availableExcept(null,draft.getRoom(),draft.getCheckIn(),draft.getCheckOut())) throw new RoomNotAvailableException("Room unavailable");
    return draft.getQuote();
  }
  public record PromotionOffer(PromotionDefinition promotion,boolean eligible,String message,Quote quote) {}
  public synchronized List<PromotionOffer> promotionOffers(String clientId,BookingRequest request) {return promotionOffers(clientId,request,null,0);}
  public synchronized List<PromotionOffer> promotionOffers(String clientId,BookingRequest request,UUID editing,long version) {
    if(editing!=null && !findReservation(editing).getClient().getId().equals(clientId)) throw new AuthorizationException();
    pointsClient(clientId); List<PromotionOffer> result=new ArrayList<>();
    for(var definition:promotionProgram.list()) {
      var selected=new BookingRequest(request.roomNumber(),request.checkIn(),request.checkOut(),request.guests(),request.serviceIds(),definition.id(),request.packageIds());
      try { result.add(new PromotionOffer(definition,true,"Elegible",editing==null?previewBooking(clientId,selected):previewPendingModification(editing,version,selected))); }
      catch(HotelException ex) { result.add(new PromotionOffer(definition,false,ex.getMessage(),null)); }
    }
    return List.copyOf(result);
  }
  public synchronized Quote previewPointRedemption(UUID id,long version,UUID benefitId) {
    Reservation r=findReservation(id);
    if(r.getStatus()!=ReservationStatus.PENDING || r.getVersion()!=version) throw new InvalidReservationException("Stale reservation version");
    if(points.policy()==null) throw new HotelException("Loyalty and refund configuration required");
    Benefit benefit=loyaltyCatalog.benefit(benefitId);
    if(points.account(r.getClient().getId(),clock.instant()).available()<benefit.cost()) throw new HotelException("Insufficient non-expired points");
    Quote quote=r.previewPointsBenefit(loyaltyCatalog.services(benefit),benefit.kind()==Benefit.Kind.RESERVATION_DISCOUNT?benefit.discount():BigDecimal.ZERO,
        benefit.kind()==Benefit.Kind.PACKAGE?List.of(benefit.targetId()):List.of());
    if(r.getPromotion()!=null) {
      var context=promotionContext(r); Set<UUID> packages=new HashSet<>(r.getPackageIds());
      if(benefit.kind()==Benefit.Kind.PACKAGE) packages.add(benefit.targetId());
      promotionProgram.validate(r.getPromotionDefinition(),new PromotionCondition.Context(r.getCheckIn(),r.getCheckOut(),context.roomType(),
          quote.services().stream().map(ServiceLine::id).collect(java.util.stream.Collectors.toSet()),packages,context.completedReservations()),clock.instant());
    }
    return quote;
  }

  public synchronized BookingView confirmReservation(Reservation reservation) {
    return confirmReservation(reservation, null, -1, null);
  }
  private BookingView confirmReservation(Reservation reservation, Benefit benefit, long version, String key) {
    requireRegistered(reservation);
    if (reservation.getStatus() == ReservationStatus.CONFIRMED && reservation.getPayment() != null) {
      if (benefit != null) throw new InvalidReservationException("A paid reservation cannot receive a new redemption");
      return reservation.view(); // natural idempotency key: registered reservation UUID
    }
    if (reservation.getStatus() != ReservationStatus.PENDING) throw new InvalidReservationException("Reservation cannot be confirmed");
    rules();
    Instant confirmedAt = clock.instant();
    if (!confirmedAt.isBefore(reservation.getSchedule().checkIn())) {
      throw new InvalidReservationException("Check-in has already started");
    }
    validatePackageSelection(reservation);
    if(reservation.getPromotion()!=null) validatePromotion(reservation,confirmedAt);
    if (!availableExcept(reservation, reservation.getRoom(), reservation.getCheckIn(), reservation.getCheckOut())) {
      throw new RoomNotAvailableException("Room unavailable");
    }
    if (reservation.getGuests() > reservation.getRoom().getCapacity()) throw new InvalidReservationException("Capacity exceeded");
    for (AdditionalService service : reservation.getServices()) {
      if (!services.contains(service) || !service.isAvailable()) throw new InvalidReservationException("Service unavailable");
    }
    if (benefit != null && reservation.getVersion() != version) throw new InvalidReservationException("Stale reservation version");
    CopMoney.positive(reservation.getTotalAmount());
    CopMoney.integral(reservation.getTotalAmount());
    String who = actorId();
    return bookingAtomic(reservation, () -> {
      VirtualCard card = reservation.getClient().getCard();
      if (benefit != null) {
        var included = loyaltyCatalog.services(benefit);
        for (AdditionalService service : included) if (!services.contains(service) || !service.isAvailable()) throw new InvalidReservationException("Benefit service unavailable");
        Quote before = reservation.getQuote();
        reservation.applyPointsBenefit(included, benefit.kind() == Benefit.Kind.RESERVATION_DISCOUNT ? benefit.discount() : BigDecimal.ZERO,
            benefit.kind()==Benefit.Kind.PACKAGE?List.of(benefit.targetId()):List.of());
        if(reservation.getPromotion()!=null) validatePromotion(reservation,confirmedAt);
        var redemption = points.redeem(reservation.getClient().getId(),reservation.getId(),version,key,benefit,confirmedAt);
        audit(reservation,"POINTS_BENEFIT",who,"Redemption " + redemption.id(),before,redemption.id(),confirmedAt);
      }
      BigDecimal amount = CopMoney.positive(reservation.getTotalAmount());
      Payment payment = new Payment(UUID.randomUUID(), reservation.getId(), card.getNumber(), amount, confirmedAt);
      card.bookingTransfer(false, amount, reservation.getId(), payment.id(), payment.at());
      transactionProbe.accept(Stage.AFTER_TRANSFER);
      reservation.pay();
      transactionProbe.accept(Stage.AFTER_STATE);
      reservation.recordPayment(payment);
      if(reservation.getPromotionDefinition()==null || reservation.getPromotionDefinition().settings().pointsAccrual()==PromotionDefinition.PointsAccrual.EARN_ON_NET_PAYMENT)
        points.paid(reservation.getClient().getId(), payment);
      audit(reservation, "CONFIRMED", who, "COP payment approved", null, payment.id(), payment.at());
      transactionProbe.accept(Stage.AFTER_RECEIPT);
      return reservation.view();
    });
  }

  public synchronized BookingView cancelReservation(Reservation reservation, boolean byHotel) {
    return cancelReservation(reservation,byHotel,clock.instant());
  }
  private BookingView cancelReservation(Reservation reservation,boolean byHotel,Instant cancelledAt) {
    requireRegistered(reservation);
    if (byHotel) requireAdmin();
    else if (actor() != internal(reservation::getClient)) throw new AuthorizationException();
    if (reservation.getStatus() == ReservationStatus.CANCELLED && reservation.getRefund() != null) return reservation.view();
    if (reservation.getStatus() != ReservationStatus.PENDING && reservation.getStatus() != ReservationStatus.CONFIRMED) {
      throw new InvalidReservationException("Reservation cannot be cancelled");
    }
    rules();
    CancellationCalculation calculation=cancellationCalculation(reservation,byHotel,cancelledAt);
    Duration left=calculation.left(); CancellationSchedule selectedSchedule=calculation.schedule();
    RefundPolicy refundPolicy=calculation.policy(); BigDecimal percent=calculation.percent();
    Payment payment=calculation.payment(); BigDecimal amount=calculation.amount();
    String who = actorId();
    return bookingAtomic(reservation, () -> {
      VirtualCard card = reservation.getClient().getCard();
      if (payment != null && !payment.cardNumber().equals(card.getNumber())) throw new InvalidReservationException("Original card mismatch");
      Refund refund = new Refund(UUID.randomUUID(), reservation.getId(), payment == null ? null : payment.id(),
          card.getNumber(), amount, refundPolicy.getName(), byHotel, cancelledAt, CopMoney.CURRENCY,
          byHotel ? "HOTEL_FULL_REFUND" : selectedSchedule.id(), byHotel ? 1 : selectedSchedule.version(), percent, left, selectedSchedule);
      if (payment != null) card.bookingTransfer(true, amount, reservation.getId(), payment.id(), refund.at());
      transactionProbe.accept(Stage.AFTER_TRANSFER);
      reservation.cancel(refundPolicy);
      transactionProbe.accept(Stage.AFTER_STATE);
      reservation.recordRefund(refund);
      points.refunded(reservation.getClient().getId(), refund, payment);
      audit(reservation, "CANCELLED", who, refundPolicy.getName(), null, refund.id(), refund.at());
      transactionProbe.accept(Stage.AFTER_RECEIPT);
      return reservation.view();
    });
  }

  private record CancellationCalculation(Duration left,CancellationSchedule schedule,RefundPolicy policy,BigDecimal percent,Payment payment,BigDecimal amount) {}
  private CancellationCalculation cancellationCalculation(Reservation reservation,boolean byHotel,Instant cancelledAt) {
    Duration left = Duration.between(cancelledAt, reservation.getSchedule().checkIn());
    CancellationSchedule selectedSchedule = byHotel ? null : cancellationSchedule;
    if (!byHotel) selectedSchedule.requireOfficial();
    RefundPolicy refundPolicy = byHotel ? new FullRefundPolicy() : selectedSchedule.select(left);
    BigDecimal percent = byHotel ? new BigDecimal("100") : ((CancellationSchedule.ConfiguredRefundPolicy) refundPolicy).percent();
    Payment payment = reservation.getPayment();
    BigDecimal paid = payment == null ? BigDecimal.ZERO : payment.amount();
    BigDecimal previouslyReturned = reservation.getRefund() == null ? BigDecimal.ZERO : reservation.getRefund().amount();
    BigDecimal eligible = paid.subtract(previouslyReturned);
    if (eligible.signum() < 0) throw new InvalidReservationException("Invalid historical refund balance");
    BigDecimal amount = CopMoney.rounded(refundPolicy.calculateRefund(eligible));
    if (amount.signum() < 0 || amount.compareTo(eligible) > 0) throw new InvalidReservationException("Refund exceeds original payment");
    return new CancellationCalculation(left,selectedSchedule,refundPolicy,percent,payment,amount);
  }
  public record CancellationQuote(UUID reservationId,long version,boolean byHotel,Instant at,BigDecimal percent,BigDecimal amount) {}
  private CancellationQuote cancellationQuote(Reservation r,boolean byHotel,Instant at) {
    requireRegistered(r); if(byHotel) requireAdmin(); else if(actor()!=internal(r::getClient)) throw new AuthorizationException();
    if(r.getStatus()==ReservationStatus.CANCELLED && r.getRefund()!=null) return new CancellationQuote(r.getId(),r.getVersion(),byHotel,at,r.getRefund().percent(),r.getRefund().amount());
    if(r.getStatus()!=ReservationStatus.PENDING && r.getStatus()!=ReservationStatus.CONFIRMED) throw new InvalidReservationException("Reservation cannot be cancelled");
    rules(); var c=cancellationCalculation(r,byHotel,at);return new CancellationQuote(r.getId(),r.getVersion(),byHotel,at,c.percent(),c.amount());
  }
  public synchronized CancellationQuote previewCancellation(UUID id,boolean byHotel) { return cancellationQuote(findReservation(id),byHotel,clock.instant()); }
  public synchronized BookingView cancelQuoted(CancellationQuote accepted) {
    DomainValidation.required(accepted,"Cancellation quote"); Reservation r=findReservation(accepted.reservationId()); Instant at=clock.instant();
    var current=cancellationQuote(r,accepted.byHotel(),at);
    if(r.getStatus()!=ReservationStatus.CANCELLED && (current.version()!=accepted.version() || current.amount().compareTo(accepted.amount())!=0 || current.percent().compareTo(accepted.percent())!=0))
      throw new InvalidReservationException("Cancellation quote changed; review the new refund");
    return cancelReservation(r,accepted.byHotel(),at);
  }

  private PromotionCondition.Context promotionContext(Reservation r) {
    long completed=internal(()->reservations.stream().filter(old->old!=r && old.getClient()==r.getClient()
        && old.getStatus()==ReservationStatus.COMPLETED && old.getPayment()!=null && old.getRefund()==null).count());
    return new PromotionCondition.Context(r.getCheckIn(),r.getCheckOut(),r.getRoom().getType().getCategory(),
        r.getServices().stream().map(AdditionalService::getId).collect(java.util.stream.Collectors.toSet()),Set.copyOf(r.getPackageIds()),completed);
  }
  private void validatePromotion(Reservation r,Instant now) {
    if(r.getPromotion()==null || !promotions.contains(r.getPromotion())) throw new HotelException("Registered promotion required");
    var definition=r.getPromotionDefinition();
    if(definition==null || !definition.id().equals(r.getPromotion().getId())) throw new InvalidReservationException("Promotion eligibility not configured");
    promotionProgram.validate(definition,promotionContext(r),now);
  }
  private void validatePackageSelection(Reservation r) {
    for(UUID id:r.getPackageIds()) for(AdditionalService service:loyaltyCatalog.packageServices(id))
      if(!r.getServices().contains(service) || !service.isAvailable()) throw new HotelException("Package services missing/unavailable");
  }
  private void validatePromotionReference(PromotionCondition condition) {
    if(condition instanceof PromotionCondition.Services selected) selected.ids().forEach(this::findService);
    if(condition instanceof PromotionCondition.Packages selected) selected.ids().forEach(loyaltyCatalog::packageServices);
  }
  public synchronized PromotionDefinition createConditionalPromotion(PromotionDefinition.Settings settings) {
    requireAdmin(); DomainValidation.required(settings,"Promotion settings");
    Promotion promotion=new Promotion(settings.name(),settings.percent());
    var prepared=promotionProgram.prepare(promotion.getId(),0,settings,this::validatePromotionReference);
    addPromotion(promotion); promotionProgram.publish(prepared,actorId(),clock.instant()); return prepared;
  }
  public synchronized PromotionDefinition configurePromotion(UUID id,long expectedVersion,PromotionDefinition.Settings settings) {
    requireAdmin(); Promotion promotion=findPromotion(id);
    var prepared=promotionProgram.prepare(id,expectedVersion,settings,this::validatePromotionReference);
    uniqueName(promotion,settings.name());
    promotionMutation=true;
    try { promotion.updateDetails(settings.name(),settings.percent()); }
    finally { promotionMutation=false; }
    promotionProgram.publish(prepared,actorId(),clock.instant()); return prepared;
  }
  public synchronized PromotionDefinition setPromotionActive(UUID id,long expectedVersion,boolean active) {
    requireAdmin(); return configurePromotion(id,expectedVersion,promotionProgram.get(id).settings().withActive(active));
  }
  public synchronized List<PromotionDefinition> conditionalPromotions() { actor(); return promotionProgram.list(); }
  public synchronized List<PromotionProgram.Change> promotionHistory() { requireAdmin(); return promotionProgram.history(); }

  /** One monitor/instant for every report family; generation never observes half a payment/refund. */
  public synchronized edu.hoteluq.report.ReportSnapshot reportSnapshot() {
    requireAdmin(); Instant at=clock.instant();
    return internal(()->{
      var roomData=rooms.stream().map(r->new edu.hoteluq.report.ReportSnapshot.Room(r.getNumber(),r.getType().getCategory(),r.isActive() && r.getType().isActive())).toList();
      var bookingData=reservations.stream().map(r->{
        var q=r.getQuote();var p=r.getPayment();var f=r.getRefund();var redemption=points.redemption(r.getId());Set<UUID> waived=new HashSet<>();
        if(redemption!=null && redemption.benefit().kind()!=Benefit.Kind.RESERVATION_DISCOUNT) loyaltyCatalog.services(redemption.benefit()).forEach(v->waived.add(v.getId()));
        Instant created=r.view().history().stream().filter(e->e.type().equals("CREATED")).map(edu.hoteluq.model.BookingRecords.AuditEvent::at).findFirst().orElse(null);
        return new edu.hoteluq.report.ReportSnapshot.Booking(r.getId(),r.getClient().getId(),q.roomNumber(),q.category(),q.typeName(),r.getStatus(),q.checkIn(),q.checkOut(),created,
            q.nightlyPrice().multiply(BigDecimal.valueOf(r.getNights())),q.services().stream().map(v->new edu.hoteluq.report.ReportSnapshot.Service(v.id(),v.name(),v.price())).toList(),waived,
            p==null?null:new edu.hoteluq.report.ReportSnapshot.Payment(p.id(),p.amount(),p.at()),
            f==null?null:new edu.hoteluq.report.ReportSnapshot.Refund(f.id(),f.paymentId(),f.amount(),f.percent(),f.byHotel(),f.at()));
      }).toList();
      var rechargeData=new ArrayList<edu.hoteluq.report.ReportSnapshot.Recharge>();var accounts=new ArrayList<PointsLedger.Account>();
      for(User user:users)if(user instanceof Client client){
        for(var movement:client.getCard().getMovements())if(movement.getType()==edu.hoteluq.enums.MovementType.RECHARGE)
          rechargeData.add(new edu.hoteluq.report.ReportSnapshot.Recharge(movement.getId(),client.getId(),movement.getExactAmount(),movement.getInstant()));
        accounts.add(points.snapshotAccount(client.getId(),at));
      }
      var purchaseData = servicePurchases.values().stream()
              .map(p -> new edu.hoteluq.report.ReportSnapshot.ServicePurchase(
                      p.id(),
                      p.clientId(),
                      p.serviceId(),
                      p.serviceName(),
                      p.amount(),
                      p.movementId(),
                      p.purchasedAt()
              ))
              .toList();

      return new edu.hoteluq.report.ReportSnapshot(
              at,
              roomData,
              bookingData,
              rechargeData,
              accounts,
              purchaseData
      );
    });
  }
  public synchronized void authorizeReports() { requireAdmin(); }

  // Loyalty endpoints share the aggregate monitor, session checks and financial transaction boundary.
  public synchronized void configureLoyalty(LoyaltyPolicy policy) {
    requireAdmin(); points.configure(policy, actorId(), clock.instant());
  }
  public synchronized LoyaltyPolicy loyaltyPolicy() { actor(); return points.policy(); }
  public synchronized void configureBenefit(Benefit benefit) { requireAdmin(); loyaltyCatalog.configure(benefit); }
  public synchronized List<Benefit> loyaltyBenefits() { actor(); return loyaltyCatalog.benefits(); }
  public synchronized LoyaltyCatalog.PackageView createServicePackage(String name, String description, List<UUID> serviceIds, List<UUID> childIds) {
    requireAdmin(); return loyaltyCatalog.create(name,description,serviceIds,childIds);
  }
  public synchronized List<LoyaltyCatalog.PackageView> servicePackages() { actor(); return loyaltyCatalog.packages(); }
  private Client pointsClient(String id) {
    User user=findUser(id); if (!(user instanceof Client client)) throw new HotelException("Client required"); return client;
  }
  public synchronized PointsLedger.Account pointsAccount(String clientId) {
    Client client=pointsClient(clientId); return points.account(client.getId(),clock.instant());
  }
  public synchronized List<PointsLedger.Deferred> deferredPoints(String clientId) {
    Client client=pointsClient(clientId); return points.deferred(client.getId());
  }
  public synchronized void settleDeferredPoints(String clientId, UUID paymentId) {
    requireAdmin(); Client client=pointsClient(clientId);
    if (points.deferred(client.getId()).stream().noneMatch(d -> d.payment().id().equals(paymentId))) {
      if (points.account(client.getId(),clock.instant()).lots().stream().anyMatch(l -> l.paymentId().equals(paymentId))) return;
      throw new HotelException("Payment does not belong to client");
    }
    Object before=points.checkpoint();
    try { points.settle(paymentId,clock.instant()); }
    catch (RuntimeException | Error failure) { points.restore(before); throw failure; }
  }
  public synchronized BookingView redeemPoints(UUID reservationId, long version, String key, UUID benefitId) {
    Reservation reservation=findReservation(reservationId);
    var previous=points.redemption(reservationId);
    if (previous!=null) {
      if (!previous.key().equals(key) || !previous.benefit().id().equals(benefitId) || previous.expectedVersion()!=version)
        throw new HotelException("Conflicting redemption retry");
      return reservation.view();
    }
    return confirmReservation(reservation,loyaltyCatalog.benefit(benefitId),version,key);
  }

  /** Compatibility alias: only an explicit call can check out, never a scheduled-date timer. */
  public synchronized BookingView completeReservation(UUID id) { return checkoutReservation(id, "legacy-checkout:" + id); }

  public synchronized BookingView checkoutReservation(UUID id, String key) {
    requireAdmin();
    Reservation reservation = findReservation(id);
    String validKey = DomainValidation.text(key, "Check-out idempotency key");
    if (reservation.getCheckout() != null) {
      if (reservation.getCheckout().idempotencyKey().equals(validKey)) return reservation.view();
      throw new InvalidReservationException("Check-out already recorded with another operation key");
    }
    if (reservation.getStatus() != ReservationStatus.CONFIRMED || reservation.getPayment() == null) throw new InvalidReservationException("Only paid confirmed reservations can check out");
    BookingRules policy = rules();
    Instant scheduled = reservation.getSchedule().checkOut(), actual = clock.instant();
    if (scheduled == null || policy.checkoutWindow() == null) throw new InvalidReservationException("Explicit check-out time and early/late policy required");
    if (!policy.checkoutWindow().allows(scheduled, actual)) throw new InvalidReservationException("Outside authorized check-out window");
    String who = actorId();
    return bookingAtomic(reservation, () -> {
      Checkout checkout = new Checkout(UUID.randomUUID(), validKey, who, actual, scheduled, HotelTime.ZONE.getId(), policy.checkoutWindow());
      reservation.recordCheckout(checkout);
      transactionProbe.accept(Stage.AFTER_STATE);
      audit(reservation, "CHECKED_OUT", who, "Explicit check-out", null, checkout.operationId(), checkout.at());
      transactionProbe.accept(Stage.AFTER_RECEIPT);
      return reservation.view();
    });
  }

  /** Read-only economic preparation with audit. Application remains blocked pending a settlement policy. */
  public synchronized ModificationProposal prepareConfirmedModification(UUID id, long version, String reason, BookingRequest request) {
    requireAdmin();
    String validReason = DomainValidation.text(reason, "Modification reason");
    Reservation reservation = findReservation(id);
    if (reservation.getStatus() != ReservationStatus.CONFIRMED || reservation.getVersion() != version) throw new InvalidReservationException("Current confirmed reservation required");
    Reservation proposed = draft(reservation.getClient(), request);
    if (!availableExcept(reservation, proposed.getRoom(), proposed.getCheckIn(), proposed.getCheckOut())) throw new RoomNotAvailableException("Room unavailable");
    ModificationProposal proposal = new ModificationProposal(UUID.randomUUID(), id, version, actorId(), clock.instant(), validReason,
        reservation.getQuote(), proposed.getQuote(), proposed.getTotalAmount().subtract(reservation.getPayment().amount()), "Economic settlement rules are undefined; application is blocked");
    return bookingAtomic(reservation, () -> {
      reservation.audit(new AuditEvent(proposal.operationId(), "CONFIRMED_CHANGE_PROPOSED", proposal.actorId(), proposal.at(),
          version, validReason + "; " + proposal.blockedReason(), proposal.before(), proposal.proposed()));
      return proposal;
    });
  }
  public synchronized void applyConfirmedModification(UUID id, long version, String reason, BookingRequest request) {
    requireAdmin();
    DomainValidation.text(reason, "Modification reason");
    Reservation r = findReservation(id);
    if (r.getStatus() != ReservationStatus.CONFIRMED || r.getVersion() != version) throw new InvalidReservationException("Current confirmed reservation required");
    throw new InvalidReservationException("Confirmed modification settlement policy is undefined; no changes applied");
  }

  public synchronized List<Reservation> getReservations() {
    User actor = actor();
    return internal(() -> reservations.stream().filter(reservation -> actor.getRole() == Role.ADMINISTRATOR || reservation.getClient() == actor).toList());
  }
}
