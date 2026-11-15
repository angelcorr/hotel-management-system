package edu.hoteluq.singleton;

import edu.hoteluq.enums.*;
import edu.hoteluq.model.*;
import edu.hoteluq.exceptions.*;
import edu.hoteluq.service.HotelService;
import org.junit.jupiter.api.BeforeEach;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

abstract class Phase2Fixture {
  protected Hotel hotel;
  protected HotelService service;
  protected MutableClock clock;
  protected String admin, owner, other, adminPassword, ownerPassword;
  protected static final LocalDate FROM = LocalDate.of(2027, 1, 10);

  static final class MutableClock extends Clock {
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-13T17:00:00Z"));
    void advance(Duration duration) { now.updateAndGet(instant -> instant.plus(duration)); }
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
    @Override public Instant instant() { return now.get(); }
  }

  @BeforeEach void initializePhase2Fixture() {
    clock = new MutableClock();
    hotel = new Hotel(clock, Duration.ofMinutes(5));
    service = new HotelService(hotel);
    adminPassword = UUID.randomUUID().toString();
    ownerPassword = UUID.randomUUID().toString();
    service.initializeAdministrator("admin", "Administrator", "admin@example.com", adminPassword, Duration.ofMinutes(5));
    admin = service.login("admin@example.com", adminPassword).token();
    service.register("client-1", "Ana", "ana@example.com", ownerPassword);
    service.register("client-2", "Luis", "luis@example.com", ownerPassword);
    owner = service.login("ana@example.com", ownerPassword).token();
    other = service.login("luis@example.com", ownerPassword).token();
    service.createRoomType(admin, RoomCategory.STANDARD, "Standard", 100, 2, "Description");
  }

  protected Client rawClient(String id) {
    return hotel.withSession(admin, () -> (Client) hotel.findUser(id));
  }
  protected VirtualCard rawCard(String id) {
    return hotel.withSession(admin, () -> ((Client) hotel.findUser(id)).getCard());
  }
  protected void room(int number) { service.createRoom(admin, RoomCategory.STANDARD, number, 1); }

  protected Reservation reserve(String id, int number, UUID serviceId, UUID promotionId) {
    return hotel.withSession(admin, () -> {
      var builder = new Reservation.Builder().client((Client) hotel.findUser(id)).room(hotel.findRoom(number))
          .checkIn(FROM).checkOut(FROM.plusDays(2));
      if (serviceId != null) builder.additionalService(hotel.findService(serviceId));
      if (promotionId != null) builder.promotion(hotel.findPromotion(promotionId));
      Reservation reservation = builder.build();
      hotel.addReservation(reservation);
      return reservation;
    });
  }

  protected int race(Runnable left, Runnable right) throws Exception {
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      Future<Boolean> first = executor.submit(() -> attempt(left, ready, start));
      Future<Boolean> second = executor.submit(() -> attempt(right, ready, start));
      try { assertTrue(ready.await(5, TimeUnit.SECONDS)); }
      finally { start.countDown(); }
      return (first.get(10, TimeUnit.SECONDS) ? 1 : 0) + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
    }
  }

  private boolean attempt(Runnable action, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
    ready.countDown();
    if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent test start timed out");
    try { action.run(); return true; }
    catch (AuthenticationException | AuthorizationException unexpected) { throw unexpected; }
    catch (HotelException expectedConflict) { return false; }
  }
}
