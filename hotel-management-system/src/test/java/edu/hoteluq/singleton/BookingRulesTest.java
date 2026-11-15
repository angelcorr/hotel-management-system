package edu.hoteluq.singleton;

import edu.hoteluq.enums.*;
import edu.hoteluq.exceptions.*;
import edu.hoteluq.service.*;
import edu.hoteluq.config.HotelTime;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Phase 3's seven policy tests retained, expectations updated to the approved 3B rules. */
class BookingRulesTest {
  Hotel hotel; HotelService users; ReservationService bookings;
  Phase2Fixture.MutableClock clock;
  String admin, client;
  final LocalDate date = LocalDate.of(2027, 1, 10);
  @BeforeEach void initialize() {
    clock = new Phase2Fixture.MutableClock(); hotel = new Hotel(clock, Duration.ofDays(365));
    users = new HotelService(hotel); bookings = new ReservationService(hotel);
    users.initializeAdministrator("a", "Admin", "a@example.com", "admin-password", Duration.ofDays(365));
    admin = users.login("a@example.com", "admin-password").token();
    users.register("c", "Client", "c@example.com", "client-password");
    client = users.login("c@example.com", "client-password").token();
    users.createRoomType(admin, RoomCategory.STANDARD, "Standard", 100, 2, "Description");
    users.createRoom(admin, RoomCategory.STANDARD, 101, 1);
  }
  BookingRequest request() { return new BookingRequest(101, date, date.plusDays(1), 1, List.of(), null); }
  void configure() { bookings.configure(admin, TestCommercialPolicies.rules()); bookings.configureCancellation(admin, TestCommercialPolicies.cancellation()); }
  void time(Instant at) { clock.advance(Duration.between(clock.instant(), at)); }
  @Test void operationsRequireExplicitRulesAndOnlyAdminCanConfigureOnce() {
    assertThrows(HotelException.class, () -> bookings.create(client, "c", "one", request()));
    assertThrows(AuthorizationException.class, () -> bookings.configure(client, TestCommercialPolicies.rules()));
    configure();
    assertThrows(HotelException.class, () -> bookings.configure(admin, TestCommercialPolicies.rules()));
    assertNotNull(bookings.create(client, "c", "one", request()));
  }
  @Test void zeroTotalIsAlwaysRejectedBeforeCreation() {
    configure(); users.updateRoomType(admin, RoomCategory.STANDARD, "Zero", 0, 2, "Description");
    assertThrows(HotelException.class, () -> bookings.create(client, "c", "one", request()));
    assertTrue(bookings.list(client).isEmpty()); assertTrue(users.card(client, "c").movements().isEmpty());
  }
  @Test void legacyFlagCannotReenableZeroOrOverrideOfficialZone() {
    assertThrows(HotelException.class, () -> new BookingRules(HotelTime.ZONE, LocalTime.NOON, true, true, true, LocalTime.NOON));
    assertThrows(HotelException.class, () -> new BookingRules(ZoneOffset.UTC, LocalTime.NOON, true, false, true, LocalTime.NOON));
  }
  @Test void activeCardIsStillRequiredForEveryPositivePayment() {
    configure(); var pending = bookings.create(client, "c", "one", request());
    users.setCardActive(admin, "c", false);
    assertThrows(InactiveCardException.class, () -> bookings.confirm(client, pending.id()));
    assertEquals(pending, bookings.get(client, pending.id()));
  }
  @Test void missingCheckoutWindowIsExplicitAndPendingModificationIsAllowed() {
    bookings.configure(admin, new BookingRules(LocalTime.of(15,0), LocalTime.NOON, null));
    var pending = bookings.create(client, "c", "one", request());
    assertEquals(1, bookings.modify(client, pending.id(), 0, request()).version());
    users.recharge(admin, "c", 100, "Funds"); bookings.confirm(client, pending.id());
    assertThrows(InvalidReservationException.class, () -> bookings.complete(admin, pending.id()));
  }
  @Test void hotelTimeZoneControlsRefundBoundaryInsteadOfJvmDefault() {
    configure(); users.recharge(admin, "c", 100, "Funds");
    var paid = bookings.confirm(client, bookings.create(client, "c", "one", request()).id());
    time(Instant.parse("2027-01-07T20:00:00Z"));
    assertEquals(0, new java.math.BigDecimal("80").compareTo(bookings.cancelByClient(client, paid.id()).refund().amount()));
  }
  @Test void officialLateCancellationIsZeroAndHotelFullRefundRemains() {
    configure(); users.createRoom(admin, RoomCategory.STANDARD, 102, 1); users.recharge(admin, "c", 200, "Funds");
    var first = bookings.confirm(client, bookings.create(client, "c", "one", request()).id());
    var second = bookings.confirm(client, bookings.create(client, "c", "two", new BookingRequest(102, date, date.plusDays(1), 1, List.of(), null)).id());
    time(Instant.parse("2027-01-10T21:00:00Z"));
    assertEquals(0, new java.math.BigDecimal("0").compareTo(bookings.cancelByClient(client, first.id()).refund().amount()));
    assertEquals(0, new java.math.BigDecimal("100").compareTo(bookings.cancelByHotel(admin, second.id()).refund().amount()));
    assertEquals(100, users.card(client, "c").balance());
  }
}
