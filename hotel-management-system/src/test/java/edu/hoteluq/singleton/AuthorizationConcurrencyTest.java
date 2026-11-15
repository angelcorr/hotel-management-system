package edu.hoteluq.singleton;

import edu.hoteluq.enums.*;
import edu.hoteluq.model.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import edu.hoteluq.exceptions.*;
import static org.junit.jupiter.api.Assertions.*;

class AuthorizationConcurrencyTest extends Phase2Fixture {
  @Test void concurrentRegistrationsCannotDuplicateNormalizedEmail() throws Exception {
    assertEquals(1, race(
        () -> service.register("new-1", "One", "same@example.com", "secret"),
        () -> service.register("new-2", "Two", " SAME@EXAMPLE.COM ", "secret")));
    assertEquals(3, service.clients(admin).size());
  }

  @Test void concurrentDirectEmailUpdatesUseOneAggregateLock() throws Exception {
    Client first = rawClient("client-1");
    Client second = rawClient("client-2");
    assertEquals(1, race(
        () -> hotel.withSession(owner, () -> { first.setEmail("same@example.com"); return null; }),
        () -> hotel.withSession(other, () -> { second.setEmail(" SAME@EXAMPLE.COM "); return null; })));
    assertEquals(1, service.clients(admin).stream().filter(view -> view.email().strip().equalsIgnoreCase("same@example.com")).count());
  }

  @Test void concurrentCatalogCreateRejectsDuplicateNames() throws Exception {
    assertEquals(1, race(
        () -> service.createService(admin, "Spa", 10),
        () -> service.createService(admin, " SPA ", 20)));
    assertEquals(1, service.services(admin).size());
  }

  @Test void concurrentCatalogRenamesCannotCreateDuplicateNames() throws Exception {
    UUID first = service.createService(admin, "First", 10).id();
    UUID second = service.createService(admin, "Second", 20).id();
    assertEquals(1, race(
        () -> service.updateService(admin, first, "Common", 10),
        () -> service.updateService(admin, second, " COMMON ", 20)));
    assertEquals(1, service.services(admin).stream().filter(view -> view.name().equalsIgnoreCase("Common")).count());
  }

  @Test void roomCreationAndTypeDeletionCannotLeaveOrphanReferences() throws Exception {
    assertEquals(1, race(
        () -> service.createRoom(admin, RoomCategory.STANDARD, 101, 1),
        () -> service.deleteRoomType(admin, RoomCategory.STANDARD)));
    if (service.rooms(admin).isEmpty()) assertTrue(service.roomTypes(admin).isEmpty());
    else assertEquals(RoomCategory.STANDARD, service.roomTypes(admin).getFirst().category());
  }

  @Test void reservationRegistrationAndServiceDeletionCannotLeaveOrphanReferences() throws Exception {
    room(101);
    UUID id = service.createService(admin, "Spa", 10).id();
    Reservation reservation = hotel.withSession(admin, () -> new Reservation.Builder()
        .client((Client) hotel.findUser("client-1")).room(hotel.findRoom(101))
        .checkIn(FROM).checkOut(FROM.plusDays(1)).additionalService(hotel.findService(id)).build());
    assertEquals(1, race(
        () -> hotel.withSession(admin, () -> { hotel.addReservation(reservation); return null; }),
        () -> service.deleteService(admin, id)));
    if (service.history(owner, "client-1").isEmpty()) assertTrue(service.services(admin).isEmpty());
    else assertEquals(id, service.services(admin).getFirst().id());
  }

  @Test void reservationRegistrationAndClientDeletionCannotLeaveOrphanReferences() throws Exception {
    room(101);
    Reservation reservation = hotel.withSession(admin, () -> new Reservation.Builder()
        .client((Client) hotel.findUser("client-1")).room(hotel.findRoom(101))
        .checkIn(FROM).checkOut(FROM.plusDays(1)).build());
    assertEquals(1, race(
        () -> hotel.withSession(admin, () -> { hotel.addReservation(reservation); return null; }),
        () -> service.deleteClient(admin, "client-1")));
    hotel.withSession(admin, () -> {
      if (!hotel.getReservations().isEmpty()) assertTrue(hotel.getClients().stream().anyMatch(client -> client.getId().equals("client-1")));
      else assertEquals(1, hotel.getClients().size());
      return null;
    });
  }

  @Test void concurrentAdministrativeRechargesPreserveBalanceAndEveryMovement() throws Exception {
    assertEquals(2, race(
        () -> service.recharge(admin, "client-1", 0.1, "One"),
        () -> service.recharge(admin, "client-1", 0.2, "Two")));
    assertEquals(0.3, service.card(owner, "client-1").balance());
    assertEquals(2, service.card(owner, "client-1").movements().size());
  }
  @Test void concurrentRegistrationsCannotDuplicateIds() throws Exception {
    assertEquals(1, race(
        () -> service.register("same-id", "One", "one@example.com", "secret"),
        () -> service.register("same-id", "Two", "two@example.com", "secret")));
    assertEquals(3, service.clients(admin).size());
  }

  @Test void rechargeAndClientDeletionCannotDiscardFinancialRecords() throws Exception {
    assertEquals(1, race(
        () -> service.recharge(admin, "client-1", 100, "Financial record"),
        () -> service.deleteClient(admin, "client-1")));
    if (service.clients(admin).stream().anyMatch(view -> view.id().equals("client-1"))) {
      assertEquals(100, service.card(admin, "client-1").balance());
      assertEquals(1, service.card(admin, "client-1").movements().size());
    } else assertEquals(1, service.clients(admin).size());
  }

  @Test void detachedStateMutationCannotRacePastThePendingRegistrationCheck() throws Exception {
    room(101);
    Client client = rawClient("client-1");
    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      for (int i = 0; i < 16; i++) {
        var from = FROM.plusDays(i * 3L);
        Reservation reservation = hotel.withSession(owner, () -> new Reservation.Builder().client(client)
            .room(hotel.findRoom(101)).checkIn(from).checkOut(from.plusDays(1)).build());
        CountDownLatch start = new CountDownLatch(1);
        Future<Boolean> insertion = executor.submit(() -> {
          assertTrue(start.await(5, TimeUnit.SECONDS));
          try { return hotel.withSession(owner, () -> { hotel.addReservation(reservation); return true; }); }
          catch (InvalidReservationException expected) { return false; }
        });
        Future<Boolean> alteration = executor.submit(() -> {
          assertTrue(start.await(5, TimeUnit.SECONDS));
          try { reservation.setStatus(ReservationStatus.CONFIRMED); return true; }
          catch (AuthenticationException | AuthorizationException expected) { return false; }
        });
        start.countDown();
        assertNotEquals(insertion.get(5, TimeUnit.SECONDS), alteration.get(5, TimeUnit.SECONDS));
        hotel.withSession(owner, () -> {
          if (hotel.getReservations().contains(reservation)) assertEquals(ReservationStatus.PENDING, reservation.getStatus());
          return null;
        });
      }
    }
  }

}
