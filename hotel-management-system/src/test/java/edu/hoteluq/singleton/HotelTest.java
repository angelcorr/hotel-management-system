package edu.hoteluq.singleton;

import edu.hoteluq.enums.*;
import edu.hoteluq.exceptions.*;
import edu.hoteluq.factory.RoomCreatorRegistry;
import edu.hoteluq.factory.RoomCreator;
import edu.hoteluq.model.*;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class HotelTest {
  private static final Hotel hotel = Hotel.getInstance();
  private static final AtomicInteger number = new AtomicInteger(100);
  private static final LocalDate from = LocalDate.of(2027, 1, 10);

  private static String administratorToken;
  private Hotel.SessionScope scope;

  @BeforeEach void authenticateFixture() { scope = hotel.openSession(administratorToken); }
  @AfterEach void closeFixture() { scope.close(); }

  @BeforeAll
  static void catalog() {
    String password = UUID.randomUUID().toString();
    hotel.initializeAdministrator("fixture-admin", "Fixture admin", "fixture-admin@example.com", password);
    administratorToken = hotel.login("fixture-admin@example.com", password).token();
    try (var scope = hotel.openSession(administratorToken)) {
    hotel.configureBookingRules(TestCommercialPolicies.rules());
    for (RoomCategory category : RoomCategory.values()) {
      hotel.addRoomType(new RoomType(category, 100, category.name(), 2, "Description"));
    }
    }
  }

  private Client client(boolean registered) {
    String id = UUID.randomUUID().toString();
    Client client = new Client(id, "Ana", id + "@example.com", "secret");
    if (registered) hotel.addUser(client);
    return client;
  }

  private Room room() {
    return new RoomCreatorRegistry().get(RoomCategory.STANDARD).create(number.incrementAndGet(), 1);
  }

  private Reservation reservation(Client client, Room room) {
    return new Reservation.Builder().client(client).room(room).checkIn(from).checkOut(from.plusDays(2)).build();
  }

  @Test
  void singletonAndFactoryRemainFunctionalForAllCategories() {
    assertSame(hotel, Hotel.getInstance());
    for (RoomCategory category : RoomCategory.values()) {
      Room room = new RoomCreatorRegistry().get(category).create(number.incrementAndGet(), 1);
      assertSame(room, hotel.findRoom(room.getNumber()));
      assertEquals(category, room.getType().getCategory());
      assertFalse(room.getFeatures().isEmpty());
    }
    assertThrows(HotelException.class, () -> new RoomCreatorRegistry().get(null));
  }

  @Test
  void templateMethodResolvesCategoryThenCreatesAndRegistersRoom() {
    java.util.List<String> steps = new java.util.ArrayList<>();
    RoomCreator creator = new RoomCreator() {
      @Override protected RoomCategory category() {
        steps.add("resolve-category");
        return RoomCategory.STANDARD;
      }
      @Override protected Room createRoom(int roomNumber, int floor, RoomType type) {
        steps.add("create-concrete-room");
        assertEquals(RoomCategory.STANDARD, type.getCategory());
        return new StandardRoom(roomNumber, floor, type);
      }
    };
    int roomNumber = number.incrementAndGet();
    Room room = creator.create(hotel, roomNumber, 1);
    assertEquals(java.util.List.of("resolve-category", "create-concrete-room"), steps);
    assertSame(room, hotel.findRoom(roomNumber),
        "The Template Method must register the product after the creation hook");
    assertThrows(DuplicateRoomException.class, () -> creator.create(hotel, roomNumber, 1),
        "The invariant must still be enforced by the aggregate");
  }

  @Test
  void roomCategoriesAndRoomNumbersCannotBeDuplicated() {
    int count = hotel.getRoomTypes().size();
    assertThrows(HotelException.class, () -> hotel.addRoomType(
        new RoomType(RoomCategory.STANDARD, 1, "Duplicate", 1, "Description")));
    assertEquals(count, hotel.getRoomTypes().size());
    Room room = room();
    assertThrows(DuplicateRoomException.class, () -> hotel.addRoom(new StandardRoom(room.getNumber(), 1, room.getType())));
    RoomType external = new RoomType(RoomCategory.STANDARD, 10, "External", 1, "Description");
    assertThrows(HotelException.class, () -> hotel.addRoom(new StandardRoom(number.incrementAndGet(), 1, external)));
    assertThrows(HotelException.class, () -> hotel.getRoomType(null));
  }

  @Test
  void duplicateUserIdAndNormalizedEmailAreRejected() {
    Client client = client(true);
    int count = hotel.getClients().size();
    assertThrows(HotelException.class, () -> hotel.addUser(new Client(client.getId(), "Other", "other@example.com", "secret")));
    assertThrows(HotelException.class, () -> hotel.addUser(new Client(UUID.randomUUID().toString(), "Other", " " + client.getEmail().toUpperCase() + " ", "secret")));
    assertEquals(count, hotel.getClients().size());
    assertSame(client, hotel.findUserByEmail(" " + client.getEmail().toUpperCase() + " "));
    assertNull(hotel.findUserByEmail("not-registered@example.com"));
  }

  @Test
  void nullCatalogEntriesAreRejectedWithoutMutatingLists() {
    int rooms = hotel.getRooms().size();
    int services = hotel.getServices().size();
    int promotions = hotel.getPromotions().size();
    int reservations = hotel.getReservations().size();
    assertThrows(HotelException.class, () -> hotel.addRoomType(null));
    assertThrows(HotelException.class, () -> hotel.addRoom(null));
    assertThrows(HotelException.class, () -> hotel.addUser(null));
    assertThrows(HotelException.class, () -> hotel.addService(null));
    assertThrows(HotelException.class, () -> hotel.addPromotion(null));
    assertThrows(HotelException.class, () -> hotel.addReservation(null));
    assertEquals(rooms, hotel.getRooms().size());
    assertEquals(services, hotel.getServices().size());
    assertEquals(promotions, hotel.getPromotions().size());
    assertEquals(reservations, hotel.getReservations().size());
  }

  @Test
  void availabilityRejectsInvalidIntervalsAndForeignRooms() {
    Room room = room();
    assertThrows(InvalidReservationException.class, () -> hotel.getAvailableRooms(from, from));
    assertThrows(InvalidReservationException.class, () -> hotel.getAvailableRooms(null, from));
    assertThrows(InvalidReservationException.class, () -> hotel.isAvailable(room, from.plusDays(1), from));
    assertThrows(HotelException.class, () -> hotel.isAvailable(null, from, from.plusDays(1)));
    assertThrows(HotelException.class, () -> hotel.isAvailable(new StandardRoom(number.incrementAndGet(), 1, room.getType()), from, from.plusDays(1)));
    room.setActive(false);
    assertFalse(hotel.getAvailableRooms(from, from.plusDays(1)).contains(room));
  }

  @Test
  void reservationsRequireRegisteredClientAndRoom() {
    Room room = room();
    assertThrows(HotelException.class, () -> hotel.addReservation(reservation(client(false), room)));
    Client client = client(true);
    Room external = new StandardRoom(number.incrementAndGet(), 1, room.getType());
    assertThrows(HotelException.class, () -> hotel.addReservation(reservation(client, external)));
    Reservation valid = reservation(client, room);
    hotel.addReservation(valid);
    assertThrows(HotelException.class, () -> hotel.addReservation(valid));
  }

  @Test
  void overlappingReservationsAreRejectedAndCancellationReleasesRoom() {
    Room room = room();
    Client client = client(true);
    Reservation first = reservation(client, room);
    hotel.addReservation(first);
    assertThrows(RoomNotAvailableException.class, () -> hotel.addReservation(reservation(client, room)));
    assertFalse(hotel.isAvailable(room, from.minusDays(1), from.plusDays(1)));
    assertTrue(hotel.isAvailable(room, from.plusDays(2), from.plusDays(3)));
    hotel.cancelReservation(first, true);
    assertTrue(hotel.isAvailable(room, from, from.plusDays(2)));
    hotel.addReservation(reservation(client, room));
  }

  @Test
  void catalogChangesBetweenBuildingAndRegisteringAreRevalidated() {
    Room room = room();
    Client client = client(true);
    AdditionalService spa = new AdditionalService("Spa", 10);
    Reservation withSpa = new Reservation.Builder().client(client).room(room).checkIn(from)
        .checkOut(from.plusDays(1)).additionalService(spa).build();
    spa.setAvailable(false);
    assertThrows(HotelException.class, () -> hotel.addReservation(withSpa));
    RoomType family = hotel.getRoomType(RoomCategory.FAMILY);
    Room familyRoom = new RoomCreatorRegistry().get(RoomCategory.FAMILY).create(number.incrementAndGet(), 1);
    Reservation twoGuests = new Reservation.Builder().client(client).room(familyRoom).checkIn(from)
        .checkOut(from.plusDays(1)).guests(2).build();
    try {
      family.setCapacity(1);
      assertThrows(HotelException.class, () -> hotel.addReservation(twoGuests));
    } finally {
      family.setCapacity(2);
    }
    Reservation active = reservation(client, room);
    room.setActive(false);
    assertThrows(RoomNotAvailableException.class, () -> hotel.addReservation(active));
  }

  @Test
  void collectionSnapshotsCannotBypassRegistration() {
    var rooms = hotel.getRooms();
    assertThrows(UnsupportedOperationException.class, () -> rooms.clear());
    assertThrows(UnsupportedOperationException.class, () -> hotel.getRoomTypes().clear());
    assertThrows(UnsupportedOperationException.class, () -> hotel.getServices().add(new AdditionalService("Fake", 0)));
    assertThrows(UnsupportedOperationException.class, () -> hotel.getPromotions().add(new Promotion("Fake", 0)));
    assertThrows(UnsupportedOperationException.class, () -> hotel.getReservations().clear());
    int size = rooms.size();
    room();
    assertEquals(size, rooms.size());
    assertEquals(size + 1, hotel.getRooms().size());
  }

  @Test
  void concurrentRegistrationOfOverlappingReservationsAcceptsOnlyOne() throws Exception {
    Room room = room();
    Client client = client(true);
    Reservation first = reservation(client, room);
    Reservation second = reservation(client, room);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    Callable<Boolean> attemptFirst = () -> attempt(first, ready, start);
    Callable<Boolean> attemptSecond = () -> attempt(second, ready, start);
    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      Future<Boolean> a = executor.submit(attemptFirst);
      Future<Boolean> b = executor.submit(attemptSecond);
      try {
        assertTrue(ready.await(5, TimeUnit.SECONDS));
      } finally {
        start.countDown();
      }
      assertNotEquals(a.get(5, TimeUnit.SECONDS), b.get(5, TimeUnit.SECONDS));
    }
  }

  private boolean attempt(Reservation reservation, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
    ready.countDown();
    if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting for registration");
    try (var scope = hotel.openSession(administratorToken)) {
      hotel.addReservation(reservation);
      return true;
    } catch (RoomNotAvailableException expected) {
      return false;
    }
  }
}
