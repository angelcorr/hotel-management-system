package edu.hoteluq.singleton;

import edu.hoteluq.enums.*;
import edu.hoteluq.exceptions.*;
import edu.hoteluq.model.*;
import edu.hoteluq.model.BookingRecords.*;
import edu.hoteluq.service.*;
import edu.hoteluq.state.*;
import edu.hoteluq.strategy.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class BookingTransactionTest extends Phase2Fixture {
  private ReservationService bookings;
  @BeforeEach void bookingFixture() { bookings = new ReservationService(hotel); room(101); room(102); }
  private BookingRequest request(int room) { return new BookingRequest(room, FROM, FROM.plusDays(2), 2, List.of(), null); }
  private BookingView create() { return bookings.create(owner, "client-1", "request-1", request(101)); }
  private BookingView paid() { service.recharge(admin, "client-1", 1000, "Funds"); return bookings.confirm(owner, create().id()); }
  private void money(String amount, BigDecimal actual) { assertEquals(0, new BigDecimal(amount).compareTo(actual)); }
  private BigDecimal balance() { return hotel.withSession(owner, () -> rawCard("client-1").getBalanceAmount()); }
  private long movements(MovementType type) { return service.card(owner, "client-1").movements().stream().filter(m -> m.getType() == type).count(); }
  private void now(Instant at) {
    clock.advance(Duration.between(clock.instant(), at));
    admin = service.login("admin@example.com", adminPassword).token();
    owner = service.login("ana@example.com", ownerPassword).token();
    other = service.login("luis@example.com", ownerPassword).token();
  }

  @Test void createsPendingSnapshotAndConfirmsWithOneLinkedPayment() {
    var spa = service.createService(admin, "Spa", 10.25);
    var request = new BookingRequest(101, FROM, FROM.plusDays(2), 2, List.of(spa.id()), null);
    BookingView pending = bookings.create(owner, "client-1", "one", request);
    assertEquals(ReservationStatus.PENDING, pending.status());
    money("210.25", pending.quote().rawSubtotal());
    money("210", pending.quote().total());
    assertNull(pending.payment());
    service.recharge(admin, "client-1", 300, "Funds");
    BookingView confirmed = bookings.confirm(owner, pending.id());
    assertEquals(ReservationStatus.CONFIRMED, confirmed.status());
    assertEquals(pending.id(), confirmed.payment().reservationId());
    assertEquals("CARD-client-1", confirmed.payment().cardNumber());
    assertEquals(pending.quote(), confirmed.quote());
    money("90", balance());
    var movement = service.card(owner, "client-1").movements().getLast();
    assertEquals(confirmed.payment().id(), movement.getPaymentId());
    assertEquals(pending.id(), movement.getReservationId());
    money("210", movement.getExactAmount());
  }

  @Test void createAndConfirmRetriesAreIdempotent() {
    BookingView pending = create();
    assertEquals(pending, bookings.create(owner, "client-1", "request-1", request(101)));
    assertThrows(InvalidReservationException.class, () -> bookings.create(owner, "client-1", "request-1", request(102)));
    service.recharge(admin, "client-1", 1000, "Funds");
    BookingView confirmed = bookings.confirm(owner, pending.id());
    assertEquals(confirmed, bookings.confirm(owner, pending.id()));
    assertEquals(confirmed, bookings.create(owner, "client-1", "request-1", request(101)));
    assertEquals(1, bookings.list(owner).size());
    assertEquals(1, movements(MovementType.PAYMENT));
    money("800", balance());
  }

  @Test void insufficientFundsLeavesPendingAndCanBeRetriedAfterRecharge() {
    BookingView pending = create();
    var cardBefore = service.card(owner, "client-1");
    assertThrows(InsufficientBalanceException.class, () -> bookings.confirm(owner, pending.id()));
    assertEquals(pending, bookings.get(owner, pending.id()));
    assertEquals(cardBefore, service.card(owner, "client-1"));
    service.recharge(admin, "client-1", 200, "Funds");
    assertEquals(ReservationStatus.CONFIRMED, bookings.confirm(owner, pending.id()).status());
    money("0", balance());
  }

  @Test void inactiveCardAndUnavailableRoomNeverProduceDebits() {
    var pending = create();
    service.recharge(admin, "client-1", 200, "Funds");
    service.setCardActive(admin, "client-1", false);
    assertThrows(InactiveCardException.class, () -> bookings.confirm(owner, pending.id()));
    service.setCardActive(admin, "client-1", true);
    service.setRoomActive(admin, 101, false);
    assertThrows(RoomNotAvailableException.class, () -> bookings.confirm(owner, pending.id()));
    assertEquals(pending, bookings.get(owner, pending.id()));
    assertEquals(0, movements(MovementType.PAYMENT));
    money("200", balance());
  }

  @Test void serviceAvailabilityIsRecheckedAtConfirmation() {
    var spa = service.createService(admin, "Spa", 10);
    var pending = bookings.create(owner, "client-1", "one", new BookingRequest(101, FROM, FROM.plusDays(1), 1, List.of(spa.id()), null));
    service.recharge(admin, "client-1", 200, "Funds");
    service.setServiceAvailable(admin, spa.id(), false);
    assertThrows(InvalidReservationException.class, () -> bookings.confirm(owner, pending.id()));
    assertEquals(pending, bookings.get(owner, pending.id()));
    money("200", balance());
  }

  @Test void pendingConflictAndAdjacentStayUseExclusiveCheckout() {
    create();
    assertThrows(RoomNotAvailableException.class, () -> bookings.create(other, "client-2", "conflict", request(101)));
    var adjacent = new BookingRequest(101, FROM.plusDays(2), FROM.plusDays(3), 1, List.of(), null);
    assertNotNull(bookings.create(other, "client-2", "adjacent", adjacent));
  }

  @Test void invalidCapacityPastDatesAndForeignCatalogReferencesAreRejected() {
    assertThrows(InvalidReservationException.class, () -> bookings.create(owner, "client-1", "capacity",
        new BookingRequest(101, FROM, FROM.plusDays(1), 3, List.of(), null)));
    assertThrows(InvalidReservationException.class, () -> bookings.create(owner, "client-1", "past",
        new BookingRequest(101, LocalDate.of(2020, 1, 1), FROM, 1, List.of(), null)));
    assertThrows(HotelException.class, () -> bookings.create(owner, "client-1", "missing-service",
        new BookingRequest(101, FROM, FROM.plusDays(1), 1, List.of(UUID.randomUUID()), null)));
    assertThrows(HotelException.class, () -> new BookingRequest(101, FROM, FROM, 1, List.of(), null));
    UUID service = UUID.randomUUID();
    assertThrows(HotelException.class, () -> new BookingRequest(101, FROM, FROM.plusDays(1), 1, List.of(service, service), null));
    assertTrue(bookings.list(owner).isEmpty());
  }

  @Test void promotionsWithoutEligibilityCannotReachAPaidReservation() {
    var promo = service.createPromotion(admin, "Unknown conditions", 10);
    assertThrows(InvalidReservationException.class, () -> bookings.create(owner, "client-1", "promo",
        new BookingRequest(101, FROM, FROM.plusDays(1), 1, List.of(), promo.id())));
    var legacy = reserve("client-1", 101, null, promo.id());
    service.recharge(admin, "client-1", 200, "Funds");
    assertThrows(InvalidReservationException.class, () -> bookings.confirm(owner, hotel.withSession(owner, legacy::getId)));
    assertEquals(0, movements(MovementType.PAYMENT));
  }

  @Test void pendingModificationIsAtomicVersionedAndPreservesIdentity() {
    var pending = create();
    var modified = bookings.modify(owner, pending.id(), pending.version(), request(102));
    assertEquals(pending.id(), modified.id());
    assertEquals(102, modified.quote().roomNumber());
    assertEquals(1, modified.version());
    assertThrows(InvalidReservationException.class, () -> bookings.modify(owner, pending.id(), pending.version(), request(101)));
    assertEquals(1, service.availableRooms(owner, FROM, FROM.plusDays(2)).size());
    assertEquals(101, service.availableRooms(owner, FROM, FROM.plusDays(2)).getFirst().number());
    service.recharge(admin, "client-1", 200, "Funds");
    var confirmed = bookings.confirm(owner, pending.id());
    assertThrows(InvalidReservationException.class, () -> bookings.modify(owner, pending.id(), confirmed.version(), request(101)));
    assertEquals(confirmed, bookings.get(owner, pending.id()));
  }

  @Test void conflictingModificationLeavesOriginalPendingAndBothRoomsConsistent() {
    var first = create();
    bookings.create(other, "client-2", "other", request(102));
    assertThrows(RoomNotAvailableException.class, () -> bookings.modify(owner, first.id(), first.version(), request(102)));
    assertEquals(first, bookings.get(owner, first.id()));
    assertTrue(service.availableRooms(owner, FROM, FROM.plusDays(2)).isEmpty());
  }

  @Test void snapshotSurvivesProfileChangesAndReturnedCollectionsCannotBeMutated() {
    var paid = paid();
    service.updateUser(owner, "client-1", "New name", "changed@example.com");
    assertEquals("Ana", bookings.get(owner, paid.id()).quote().clientName());
    assertEquals("ana@example.com", bookings.get(owner, paid.id()).quote().clientEmail());
    assertThrows(UnsupportedOperationException.class, () -> paid.quote().features().clear());
    assertThrows(UnsupportedOperationException.class, () -> paid.quote().services().clear());
    assertThrows(UnsupportedOperationException.class, () -> bookings.list(owner).clear());
  }

  @Test void authorizationCoversOwnerHotelRoleAndDirectMoneyOrStateBypasses() {
    var pending = create();
    assertThrows(AuthenticationException.class, () -> bookings.confirm("invalid", pending.id()));
    assertThrows(AuthorizationException.class, () -> bookings.confirm(other, pending.id()));
    assertThrows(AuthorizationException.class, () -> bookings.cancelByClient(other, pending.id()));
    assertThrows(AuthorizationException.class, () -> bookings.cancelByHotel(owner, pending.id()));
    assertThrows(AuthorizationException.class, () -> bookings.cancelByClient(admin, pending.id()));
    assertThrows(AuthorizationException.class, () -> bookings.create(owner, "client-2", "other-client", request(102)));
    hotel.withSession(admin, () -> {
      var r = hotel.findReservation(pending.id());
      assertThrows(InvalidReservationException.class, () -> r.setState(new ConfirmedState()));
      assertThrows(InvalidReservationException.class, r::pay);
      assertThrows(AuthorizationException.class, () -> r.recordPayment(new Payment(UUID.randomUUID(), pending.id(), "fake", BigDecimal.ONE, clock.instant())));
      assertThrows(InvalidReservationException.class, () -> rawCard("client-1").refund(100, "unlinked"));
      assertThrows(InvalidReservationException.class, () -> rawCard("client-1").charge(100, "unlinked"));
      assertThrows(AuthorizationException.class, () -> rawCard("client-1").bookingTransfer(true, BigDecimal.TEN, pending.id(), UUID.randomUUID(), clock.instant()));
      assertThrows(AuthorizationException.class, () -> r.restore(r.checkpoint()));
      return null;
    });
    assertEquals(pending, bookings.get(owner, pending.id()));
  }

  @Test void detachedOrForeignReservationCannotDebitRegisteredCard() {
    service.recharge(admin, "client-1", 200, "Funds");
    hotel.withSession(owner, () -> {
      var detached = new Reservation.Builder().client((Client) hotel.currentUser()).room(hotel.findRoom(101)).checkIn(FROM).checkOut(FROM.plusDays(2)).build();
      assertThrows(InvalidReservationException.class, () -> bookings.confirm(detached));
      return null;
    });
    assertEquals(0, movements(MovementType.PAYMENT));
    money("200", balance());
  }

  @ParameterizedTest @EnumSource(Hotel.Stage.class)
  void paymentPartialFailureRestoresExactPrestateAndRetrySucceeds(Hotel.Stage stage) {
    var pending = create();
    service.recharge(admin, "client-1", 200, "Funds");
    var cardBefore = service.card(owner, "client-1");
    hotel.transactionProbe = at -> { if (at == stage) throw new IllegalStateException("Injected " + stage); };
    assertThrows(IllegalStateException.class, () -> bookings.confirm(owner, pending.id()));
    assertEquals(pending, bookings.get(owner, pending.id()));
    assertEquals(cardBefore, service.card(owner, "client-1"));
    hotel.transactionProbe = at -> {};
    var paid = bookings.confirm(owner, pending.id());
    assertEquals(paid, bookings.confirm(owner, pending.id()));
    assertEquals(1, movements(MovementType.PAYMENT));
    money("0", balance());
  }

  @Test void fatalErrorAfterDebitAlsoRestoresBeforePropagating() {
    var pending = create();
    service.recharge(admin, "client-1", 200, "Funds");
    hotel.transactionProbe = stage -> { throw new AssertionError("Injected error"); };
    assertThrows(AssertionError.class, () -> bookings.confirm(owner, pending.id()));
    assertEquals(pending, bookings.get(owner, pending.id()));
    assertEquals(0, movements(MovementType.PAYMENT));
    money("200", balance());
  }

  @ParameterizedTest @CsvSource({"259201,200", "259200,160", "86400,160", "86399,0"})
  void clientCancellationUsesDocumentedBoundaries(long seconds, String returned) {
    var paid = paid();
    now(FROM.atTime(15, 0).atZone(edu.hoteluq.config.HotelTime.ZONE).toInstant().minusSeconds(seconds));
    var cancelled = bookings.cancelByClient(owner, paid.id());
    assertEquals(ReservationStatus.CANCELLED, cancelled.status());
    money(returned, cancelled.refund().amount());
    assertEquals(paid.payment().id(), cancelled.refund().paymentId());
    assertEquals(paid.payment().cardNumber(), cancelled.refund().cardNumber());
    assertTrue(cancelled.refund().amount().compareTo(paid.payment().amount()) <= 0);
    money(new BigDecimal("800").add(new BigDecimal(returned)).toPlainString(), balance());
    assertEquals(cancelled, bookings.cancelByClient(owner, paid.id()));
    assertEquals(cancelled, bookings.cancelByHotel(admin, paid.id())); // no second refund or hotel top-up
    assertEquals(returned.equals("0") ? 0 : 1, movements(MovementType.REFUND));
    assertThrows(InvalidReservationException.class, () -> bookings.confirm(owner, paid.id()));
  }

  @Test void hotelReturnsFullPaymentToOriginalInactiveCard() {
    var paid = paid();
    now(FROM.atTime(14, 59).atZone(edu.hoteluq.config.HotelTime.ZONE).toInstant());
    service.setCardActive(admin, "client-1", false);
    var cancelled = bookings.cancelByHotel(admin, paid.id());
    money("200", cancelled.refund().amount());
    money("1000", balance());
    assertEquals(1, movements(MovementType.REFUND));
    var refundMovement = service.card(owner, "client-1").movements().getLast();
    assertEquals(paid.payment().id(), refundMovement.getPaymentId());
    assertEquals(paid.id(), refundMovement.getReservationId());
  }

  @Test void pendingCancellationNeverInventsARefundAndFreesAvailability() {
    var pending = create();
    var cancelled = bookings.cancelByClient(owner, pending.id());
    assertNull(cancelled.refund().paymentId());
    money("0", cancelled.refund().amount());
    assertEquals(0, movements(MovementType.REFUND));
    assertNotNull(bookings.create(other, "client-2", "replacement", request(101)));
  }

  @ParameterizedTest @EnumSource(Hotel.Stage.class)
  void refundPartialFailureRestoresPaymentStateCardAndAllowsOneRetry(Hotel.Stage stage) {
    var paid = paid();
    var cardBefore = service.card(owner, "client-1");
    hotel.transactionProbe = at -> { if (at == stage) throw new IllegalStateException("Injected " + stage); };
    assertThrows(IllegalStateException.class, () -> bookings.cancelByClient(owner, paid.id()));
    assertEquals(paid, bookings.get(owner, paid.id()));
    assertEquals(cardBefore, service.card(owner, "client-1"));
    hotel.transactionProbe = at -> {};
    var cancelled = bookings.cancelByClient(owner, paid.id());
    assertEquals(cancelled, bookings.cancelByClient(owner, paid.id()));
    assertEquals(1, movements(MovementType.REFUND));
    money("1000", balance());
  }

  @Test void clockFromCallerCannotForgeLargerRefund() {
    var paid = paid();
    hotel.withSession(owner, () -> {
      assertThrows(InvalidReservationException.class, () -> bookings.cancelByClient(hotel.findReservation(paid.id()), LocalDateTime.of(2000, 1, 1, 0, 0)));
      return null;
    });
    assertEquals(paid, bookings.get(owner, paid.id()));
  }

  @Test void checkInDeadlineAndConfiguredLateCancellationAreEnforced() {
    var pending = create();
    service.recharge(admin, "client-1", 200, "Funds");
    now(FROM.atTime(15, 0).atZone(edu.hoteluq.config.HotelTime.ZONE).toInstant());
    assertThrows(InvalidReservationException.class, () -> bookings.confirm(owner, pending.id()));
    assertThrows(InvalidReservationException.class, () -> bookings.create(other, "client-2", "late", request(102)));
    assertEquals(ReservationStatus.CANCELLED, bookings.cancelByClient(owner, pending.id()).status());
    assertNull(bookings.get(owner, pending.id()).payment());
    money("200", balance());
  }

  @Test void completionRequiresAdministratorPaymentAndConfiguredCheckoutTime() {
    var paid = paid();
    assertThrows(AuthorizationException.class, () -> bookings.complete(owner, paid.id()));
    assertThrows(InvalidReservationException.class, () -> bookings.complete(admin, paid.id()));
    now(FROM.plusDays(2).atTime(12, 0).atZone(edu.hoteluq.config.HotelTime.ZONE).toInstant());
    var completed = bookings.complete(admin, paid.id());
    assertEquals(ReservationStatus.COMPLETED, completed.status());
    assertEquals(completed, bookings.complete(admin, paid.id()));
    assertThrows(InvalidReservationException.class, () -> bookings.cancelByHotel(admin, paid.id()));
    assertEquals(1, movements(MovementType.PAYMENT));
    assertEquals(0, movements(MovementType.REFUND));
  }

  @Test void exactDecimalPaymentAndPartialRefundConserveEveryDigit() {
    service.updateRoomType(admin, RoomCategory.STANDARD, "Decimal", 123.456789, 2, "Exact");
    service.recharge(admin, "client-1", 1000, "Funds");
    var paid = bookings.confirm(owner, create().id());
    money("247", paid.payment().amount());
    now(FROM.atTime(15, 0).atZone(edu.hoteluq.config.HotelTime.ZONE).toInstant().minusSeconds(86400));
    var cancelled = bookings.cancelByClient(owner, paid.id());
    money("198", cancelled.refund().amount());
    money("951", balance());
  }

  @Test @Timeout(10) void concurrentReaderCannotObserveDebitBeforeConfirmationOrRollback() throws Exception {
    var pending = create();
    service.recharge(admin, "client-1", 200, "Funds");
    CountDownLatch debited = new CountDownLatch(1), release = new CountDownLatch(1), reading = new CountDownLatch(1);
    hotel.transactionProbe = stage -> {
      if (stage != Hotel.Stage.AFTER_TRANSFER) return;
      debited.countDown();
      try { if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Probe timed out"); }
      catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
      throw new IllegalStateException("Fail after reader attempts to observe");
    };
    try (var executor = Executors.newFixedThreadPool(2)) {
      Future<?> writer = executor.submit(() -> bookings.confirm(owner, pending.id()));
      try {
        assertTrue(debited.await(5, TimeUnit.SECONDS));
        Future<BookingView> reader = executor.submit(() -> { reading.countDown(); return bookings.get(owner, pending.id()); });
        assertTrue(reading.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> reader.get(100, TimeUnit.MILLISECONDS));
        release.countDown();
        assertThrows(ExecutionException.class, () -> writer.get(5, TimeUnit.SECONDS));
        assertEquals(pending, reader.get(5, TimeUnit.SECONDS));
      } finally { release.countDown(); }
    }
    money("200", balance());
    assertEquals(0, movements(MovementType.PAYMENT));
  }

  @Test void concurrentCreationsCannotAcquireOverlappingRoom() throws Exception {
    assertEquals(1, race(() -> bookings.create(owner, "client-1", "a", request(101)),
        () -> bookings.create(other, "client-2", "b", request(101))));
    assertEquals(1, bookings.list(admin).size());
  }

  @Test void concurrentIdenticalCreateAndConfirmReturnOneReservationAndOnePayment() throws Exception {
    assertEquals(2, race(() -> bookings.create(owner, "client-1", "same", request(101)),
        () -> bookings.create(owner, "client-1", "same", request(101))));
    var id = bookings.list(owner).getFirst().id();
    service.recharge(admin, "client-1", 200, "Funds");
    assertEquals(2, race(() -> bookings.confirm(owner, id), () -> bookings.confirm(owner, id)));
    assertEquals(1, bookings.list(owner).size());
    assertEquals(1, movements(MovementType.PAYMENT));
    money("0", balance());
  }

  @Test void concurrentPaymentsOnDifferentReservationsCannotOverdrawSameCard() throws Exception {
    var first = create();
    var second = bookings.create(owner, "client-1", "second", request(102));
    service.recharge(admin, "client-1", 200, "Funds");
    assertEquals(1, race(() -> bookings.confirm(owner, first.id()), () -> bookings.confirm(owner, second.id())));
    assertEquals(1, movements(MovementType.PAYMENT));
    money("0", balance());
  }

  @Test void concurrentClientAndHotelCancellationNeverRefundTwice() throws Exception {
    var paid = paid();
    assertEquals(2, race(() -> bookings.cancelByClient(owner, paid.id()), () -> bookings.cancelByHotel(admin, paid.id())));
    assertEquals(1, movements(MovementType.REFUND));
    money("1000", balance());
  }

  @Test void concurrentConfirmAndCancelCannotLeaveAnUnpaidConfirmation() throws Exception {
    var pending = create();
    service.recharge(admin, "client-1", 200, "Funds");
    race(() -> bookings.confirm(owner, pending.id()), () -> bookings.cancelByClient(owner, pending.id()));
    var result = bookings.get(owner, pending.id());
    assertEquals(ReservationStatus.CANCELLED, result.status());
    assertNotNull(result.refund());
    assertEquals(movements(MovementType.PAYMENT), movements(MovementType.REFUND));
    money("200", balance());
  }

  @Test void concurrentModificationsRequireCurrentVersion() throws Exception {
    var pending = create();
    assertEquals(1, race(() -> bookings.modify(owner, pending.id(), 0, request(102)),
        () -> bookings.modify(owner, pending.id(), 0, request(101))));
    assertEquals(1, bookings.get(owner, pending.id()).version());
  }
}
