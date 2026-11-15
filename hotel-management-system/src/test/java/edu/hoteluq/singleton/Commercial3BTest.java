package edu.hoteluq.singleton;

import edu.hoteluq.config.HotelTime;
import edu.hoteluq.enums.*;
import edu.hoteluq.exceptions.*;
import edu.hoteluq.model.*;
import edu.hoteluq.model.BookingRecords.*;
import edu.hoteluq.money.CopMoney;
import edu.hoteluq.service.*;
import edu.hoteluq.strategy.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class Commercial3BTest extends Phase2Fixture {
  ReservationService bookings;
  @BeforeEach void setupCommercial() { bookings = new ReservationService(hotel); room(101); room(102); }
  BookingRequest request(int number) { return new BookingRequest(number, FROM, FROM.plusDays(2), 1, List.of(), null); }
  BookingView create() { return bookings.create(owner, "client-1", "one", request(101)); }
  BookingView paid() { service.recharge(admin, "client-1", new BigDecimal("1000"), "COP", "Funds"); return bookings.confirm(owner, create().id()); }
  void now(Instant at) {
    clock.advance(Duration.between(clock.instant(), at));
    admin = service.login("admin@example.com", adminPassword).token();
    owner = service.login("ana@example.com", ownerPassword).token();
    other = service.login("luis@example.com", ownerPassword).token();
  }
  void amount(String expected, BigDecimal value) { assertEquals(0, new BigDecimal(expected).compareTo(value)); }
  CancellationSchedule policy(long version, String late) {
    return new CancellationSchedule("COMMERCIAL-TEST", version, List.of(
      new CancellationSchedule.Window(null, Duration.ZERO, new BigDecimal(late)),
      new CancellationSchedule.Window(Duration.ZERO, Duration.ofHours(24), BigDecimal.ZERO),
      new CancellationSchedule.Window(Duration.ofHours(24), CancellationSchedule.FULL_REFUND_START, new BigDecimal("80")),
      new CancellationSchedule.Window(CancellationSchedule.FULL_REFUND_START, null, new BigDecimal("100"))));
  }

  @ParameterizedTest @CsvSource({"2026-11-15T04:59:59Z,2026-11-14", "2026-11-15T05:00:00Z,2026-11-15", "2026-12-01T04:59:59Z,2026-11-30", "2026-12-01T05:00:00Z,2026-12-01", "2027-01-01T04:59:59Z,2026-12-31", "2027-01-01T05:00:00Z,2027-01-01"})
  void dayMonthAndYearBoundariesUseBogotaForMovementsAndOperations(String instant, String localDate) {
    now(Instant.parse(instant));
    var pending = create();
    service.recharge(admin, "client-1", new BigDecimal("200"), "COP", "Official clock");
    var paid = bookings.confirm(owner, pending.id());
    var movement = service.card(owner, "client-1").movements().getLast();
    assertEquals(clock.instant(), paid.payment().at());
    assertEquals(clock.instant(), movement.getInstant());
    assertEquals(LocalDate.parse(localDate), movement.getDate().toLocalDate());
    assertEquals(HotelTime.ZONE, paid.schedule().zone());
    assertEquals(Instant.parse("2027-01-10T20:00:00Z"), paid.schedule().checkIn());
    assertEquals(FROM, paid.quote().checkIn());
  }

  @Test void foreignJvmZoneDoesNotAffectBusinessDatesOrClockInjection() {
    TimeZone previous = TimeZone.getDefault();
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
      now(Instant.parse("2027-01-01T04:00:00Z"));
      var paid = paid();
      assertEquals(LocalDate.of(2026,12,31), HotelTime.local(paid.payment().at()).toLocalDate());
      assertEquals(Instant.parse("2027-01-10T20:00:00Z"), paid.schedule().checkIn());
      now(Instant.parse("2027-01-01T05:00:00Z"));
      service.recharge(admin, "client-1", 1, "Next hotel day");
      assertEquals(LocalDate.of(2027,1,1), service.card(owner, "client-1").movements().getLast().getDate().toLocalDate());
    } finally { TimeZone.setDefault(previous); }
  }

  @ParameterizedTest @CsvSource({"259200,160", "259199,160", "86400,160", "86399,0", "0,0", "-1,0"})
  void officialWindowsHaveExactBoundariesAndNoLateRefund(long leftSeconds, String refund) {
    bookings.configureCancellation(admin, policy(1, "0"));
    var paid = paid();
    now(paid.schedule().checkIn().minusSeconds(leftSeconds));
    var cancelled = bookings.cancelByClient(owner, paid.id());
    amount(refund, cancelled.refund().amount());
    assertEquals("COMMERCIAL-TEST", cancelled.refund().policyId());
    assertEquals(1, cancelled.refund().policyVersion());
    assertEquals(Duration.ofSeconds(leftSeconds), cancelled.refund().timeLeft());
    assertEquals(cancelled.refund().timeLeft(),Duration.between(cancelled.refund().at(),paid.schedule().checkIn()));
    assertEquals(paid.payment().id(), cancelled.refund().paymentId());
    assertEquals("COP", cancelled.refund().currency());
    assertEquals(policy(1, "0"), cancelled.refund().policySnapshot());
    assertEquals(cancelled, bookings.cancelByClient(owner, paid.id()));
    assertTrue(cancelled.refund().amount().compareTo(paid.payment().amount()) <= 0);
  }

  @Test void policyVersionChangesNeverRewriteACompletedRefundOrAudit() {
    bookings.configureCancellation(admin, policy(1, "0"));
    var paid = paid(); now(paid.schedule().checkIn().plusSeconds(1));
    var cancelled = bookings.cancelByClient(owner, paid.id());
    bookings.configureCancellation(admin, policy(2, "0"));
    assertEquals(cancelled, bookings.cancelByClient(owner, paid.id()));
    assertEquals(policy(1, "0"), cancelled.refund().policySnapshot());
    assertThrows(HotelException.class, () -> bookings.configureCancellation(admin, policy(2, "45")));
    assertThrows(HotelException.class, () -> bookings.configureCancellation(admin, policy(1, "0")));
    assertEquals(3, bookings.policyHistory(admin).size()); // fixture, v1, v2
    assertThrows(AuthorizationException.class, () -> bookings.policyHistory(owner));
    assertThrows(UnsupportedOperationException.class, () -> bookings.policyHistory(admin).clear());
  }

  @ParameterizedTest @EnumSource(Hotel.Stage.class)
  void failedOfficialPartialRefundRestoresTraceAndMoney(Hotel.Stage stage) {
    bookings.configureCancellation(admin, policy(1,"0"));
    var paid = paid(); now(paid.schedule().checkIn().minus(Duration.ofHours(48)));
    var before = service.card(owner, "client-1");
    hotel.transactionProbe = at -> { if (at == stage) throw new IllegalStateException("Injected"); };
    assertThrows(IllegalStateException.class, () -> bookings.cancelByClient(owner, paid.id()));
    assertEquals(paid, bookings.get(owner, paid.id()));
    assertEquals(before, service.card(owner, "client-1"));
    hotel.transactionProbe = at -> {};
    amount("160", bookings.cancelByClient(owner, paid.id()).refund().amount());
  }

  @Test void policyActivationAndConcurrentCancellationKeepOneHistoricalSnapshot() throws Exception {
    bookings.configureCancellation(admin, policy(1,"0")); var paid = paid();
    now(paid.schedule().checkIn().minus(Duration.ofHours(48)));
    assertEquals(2, race(() -> bookings.configureCancellation(admin, policy(2,"0")),
        () -> bookings.cancelByClient(owner, paid.id())));
    var refund = bookings.get(owner, paid.id()).refund();
    assertEquals(refund.policyVersion(), refund.policySnapshot().version());
    amount("160", refund.amount());
    assertEquals(refund, bookings.cancelByClient(owner,paid.id()).refund());
  }

  @Test void concurrentPaymentsAtBogotaNewYearKeepOneInstantAndOfficialDay() throws Exception {
    now(Instant.parse("2027-01-01T04:59:59Z")); var pending = create();
    service.recharge(admin,"client-1",200,"Funds");
    assertEquals(2,race(() -> bookings.confirm(owner,pending.id()),() -> bookings.confirm(owner,pending.id())));
    var paid=bookings.get(owner,pending.id());
    assertEquals(Instant.parse("2027-01-01T04:59:59Z"),paid.payment().at());
    assertEquals(LocalDate.of(2026,12,31),HotelTime.local(paid.payment().at()).toLocalDate());
    assertEquals(1,service.card(owner,"client-1").movements().stream().filter(m -> m.getType()==MovementType.PAYMENT).count());
  }

  @Test void concurrentLateCancellationsHaveOneDecisionAndNoRefundMovement() throws Exception {
    bookings.configureCancellation(admin, policy(1,"0")); var paid = paid();
    now(paid.schedule().checkIn().plusSeconds(1));
    assertEquals(2, race(() -> bookings.cancelByClient(owner, paid.id()), () -> bookings.cancelByClient(owner, paid.id())));
    var result = bookings.get(owner, paid.id()); amount("0", result.refund().amount());
    assertEquals(0, service.card(owner,"client-1").movements().stream().filter(m -> m.getType()==MovementType.REFUND).count());
    assertEquals(1, result.history().stream().filter(e -> e.type().equals("CANCELLED")).count());
  }

  @Test void checkoutIsExplicitAuditedIdempotentAndNotAClockTimer() {
    var paid = paid(); now(paid.schedule().checkOut());
    assertEquals(ReservationStatus.CONFIRMED, bookings.get(owner,paid.id()).status());
    var checked = bookings.checkout(admin,paid.id(),"checkout-1");
    assertEquals(ReservationStatus.COMPLETED, checked.status());
    assertEquals("admin", checked.checkout().actorId()); assertEquals(clock.instant(),checked.checkout().at());
    assertNotNull(checked.checkout().operationId());
    assertEquals(checked.checkout().operationId(),checked.history().getLast().operationId());
    assertEquals(HotelTime.ZONE.getId(),checked.checkout().zone());
    assertEquals(checked,bookings.checkout(admin,paid.id(),"checkout-1"));
    assertThrows(InvalidReservationException.class,() -> bookings.checkout(admin,paid.id(),"different"));
    assertThrows(InvalidReservationException.class,() -> bookings.cancelByHotel(admin,paid.id()));
    assertEquals(paid.quote(),checked.quote()); assertEquals(paid.payment(),checked.payment());
    assertEquals(1,checked.history().stream().filter(e -> e.type().equals("CHECKED_OUT")).count());
    assertThrows(UnsupportedOperationException.class,() -> checked.history().clear());
  }

  @Test void checkoutRejectsUnauthenticatedOwnerPendingAndCancelled() {
    var pending=create();
    assertThrows(AuthenticationException.class,() -> bookings.checkout("bad",pending.id(),"one"));
    assertThrows(AuthorizationException.class,() -> bookings.checkout(owner,pending.id(),"one"));
    assertThrows(InvalidReservationException.class,() -> bookings.checkout(admin,pending.id(),"one"));
    bookings.cancelByHotel(admin,pending.id());
    assertThrows(InvalidReservationException.class,() -> bookings.checkout(admin,pending.id(),"one"));
  }

  @Test void earlyOrTooLateCheckoutRequiresExplicitWindowAuthorization() {
    var paid=paid(); now(paid.schedule().checkOut().minusNanos(1));
    assertThrows(InvalidReservationException.class,() -> bookings.checkout(admin,paid.id(),"early"));
    now(paid.schedule().checkOut().plus(Duration.ofDays(1)).plusNanos(1));
    assertThrows(InvalidReservationException.class,() -> bookings.checkout(admin,paid.id(),"late"));
    assertEquals(paid,bookings.get(owner,paid.id()));
  }

  @Test void concurrentCheckoutHasOneResponsibleOperationAndHistoryEntry() throws Exception {
    var paid=paid(); now(paid.schedule().checkOut());
    assertEquals(2,race(() -> bookings.checkout(admin,paid.id(),"same"),() -> bookings.checkout(admin,paid.id(),"same")));
    assertEquals(1,bookings.get(owner,paid.id()).history().stream().filter(e -> e.type().equals("CHECKED_OUT")).count());
  }

  @ParameterizedTest @EnumSource(value=Hotel.Stage.class,names={"AFTER_STATE","AFTER_RECEIPT"})
  void failedCheckoutRestoresStateReceiptAndAudit(Hotel.Stage stage) {
    var paid=paid(); now(paid.schedule().checkOut());
    hotel.transactionProbe=at -> {if(at==stage)throw new IllegalStateException("Injected");};
    assertThrows(IllegalStateException.class,() -> bookings.checkout(admin,paid.id(),"one"));
    assertEquals(paid,bookings.get(owner,paid.id()));
    hotel.transactionProbe=at -> {};
    assertEquals(ReservationStatus.COMPLETED,bookings.checkout(admin,paid.id(),"one").status());
  }

  @Test void controlledConfirmedChangeRequiresAdminReasonAndVersionAndNeverSettlesMoney() {
    var paid=paid(); var card=service.card(owner,"client-1");
    assertThrows(AuthorizationException.class,() -> bookings.prepareConfirmedModification(owner,paid.id(),paid.version(),"Move",request(102)));
    assertThrows(HotelException.class,() -> bookings.prepareConfirmedModification(admin,paid.id(),paid.version()," ",request(102)));
    assertThrows(InvalidReservationException.class,() -> bookings.prepareConfirmedModification(admin,paid.id(),0,"Move",request(102)));
    var proposal=bookings.prepareConfirmedModification(admin,paid.id(),paid.version(),"Guest requested another room",request(102));
    amount("0",proposal.difference()); assertEquals(102,proposal.proposed().roomNumber());
    assertNotNull(proposal.blockedReason());
    assertThrows(InvalidReservationException.class,() -> bookings.applyConfirmedModification(admin,paid.id(),paid.version(),"Move",request(102)));
    var current=bookings.get(owner,paid.id());
    assertEquals(paid.quote(),current.quote()); assertEquals(paid.payment(),current.payment()); assertEquals(paid.version(),current.version());
    assertEquals(card,service.card(owner,"client-1"));
    assertEquals("CONFIRMED_CHANGE_PROPOSED",current.history().getLast().type());
    assertEquals(proposal.operationId(),current.history().getLast().operationId());
  }

  @Test void creationFailureDoesNotLeaveAnOrphanAvailabilityBlockOrConsumedIdempotencyKey() {
    hotel.transactionProbe=stage -> {if(stage==Hotel.Stage.AFTER_RECEIPT)throw new IllegalStateException("Creation failed");};
    assertThrows(IllegalStateException.class,this::create);
    assertTrue(bookings.list(owner).isEmpty());
    assertEquals(2,service.availableRooms(owner,FROM,FROM.plusDays(2)).size());
    assertTrue(service.card(owner,"client-1").movements().isEmpty());
    hotel.transactionProbe=stage -> {};
    var created=create();assertEquals(1,created.history().size());
    assertEquals(created,create());
  }

  @Test void pendingModificationKeepsHistoricalSnapshotsAndRejectsBadReferencesAndCapacity() {
    var pending=create(); var spa=service.createService(admin,"Spa",new BigDecimal("10.5"),"COP");
    var modified=bookings.modify(owner,pending.id(),0,new BookingRequest(102,FROM.plusDays(1),FROM.plusDays(3),2,List.of(spa.id()),null));
    assertEquals(1,modified.version()); amount("211",modified.quote().total());
    assertEquals(pending.quote(),modified.history().getLast().before());
    assertEquals(modified.quote(),modified.history().getLast().after());
    assertThrows(InvalidReservationException.class,() -> bookings.modify(owner,pending.id(),0,request(101)));
    assertThrows(HotelException.class,() -> bookings.modify(owner,pending.id(),1,new BookingRequest(101,FROM,FROM.plusDays(1),1,List.of(UUID.randomUUID()),null)));
    assertThrows(InvalidReservationException.class,() -> bookings.modify(owner,pending.id(),1,new BookingRequest(101,FROM,FROM.plusDays(1),3,List.of(),null)));
    assertEquals(modified,bookings.get(owner,pending.id()));
  }

  @Test void incompatibleRoomAndZeroResultDuringRecalculationLeavePendingHistoryUnchanged() {
    var pending=create();
    service.createRoomType(admin,RoomCategory.FAMILY,"Small family",new BigDecimal("0.10"),"COP",1,"Description");
    service.createRoom(admin,RoomCategory.FAMILY,201,1);
    assertThrows(InvalidReservationException.class,() -> bookings.modify(owner,pending.id(),0,
        new BookingRequest(201,FROM,FROM.plusDays(1),2,List.of(),null)));
    assertThrows(HotelException.class,() -> bookings.modify(owner,pending.id(),0,
        new BookingRequest(201,FROM,FROM.plusDays(1),1,List.of(),null)));
    assertEquals(pending,bookings.get(owner,pending.id()));
    assertTrue(service.card(owner,"client-1").movements().isEmpty());
  }

  @ParameterizedTest @EnumSource(value=Hotel.Stage.class,names={"AFTER_STATE","AFTER_RECEIPT"})
  void modificationFailureRestoresVersionAvailabilityAndAudit(Hotel.Stage stage) {
    var pending=create(); hotel.transactionProbe=at -> {if(at==stage)throw new IllegalStateException("Injected");};
    assertThrows(IllegalStateException.class,() -> bookings.modify(owner,pending.id(),0,request(102)));
    assertEquals(pending,bookings.get(owner,pending.id()));
    assertEquals(102,service.availableRooms(owner,FROM,FROM.plusDays(2)).getFirst().number());
  }

  @Test void cancelledAndCheckedOutReservationsCannotBeModified() {
    var pending=bookings.create(owner,"client-1","second",request(102));
    var cancelled=bookings.cancelByHotel(admin,pending.id());
    assertThrows(InvalidReservationException.class,() -> bookings.modify(admin,cancelled.id(),cancelled.version(),request(101)));
    assertThrows(InvalidReservationException.class,() -> bookings.prepareConfirmedModification(admin,cancelled.id(),cancelled.version(),"Move",request(101)));
    var paid=paid(); now(paid.schedule().checkOut()); var checked=bookings.checkout(admin,paid.id(),"one");
    assertThrows(InvalidReservationException.class,() -> bookings.modify(admin,paid.id(),checked.version(),request(102)));
    assertThrows(InvalidReservationException.class,() -> bookings.prepareConfirmedModification(admin,paid.id(),checked.version(),"Move",request(102)));
  }

  @Test void roundedTotalAndPaymentReceiptMovementAndPartialRefundReconcile() {
    hotel.withSession(admin,() -> {hotel.getRoomType(RoomCategory.STANDARD).setPricePerNight(new BigDecimal("10.25"),"COP");return null;});
    var pending=create(); amount("20.5",pending.quote().rawSubtotal()); amount("21",pending.quote().total());
    service.recharge(admin,"client-1",new BigDecimal("100.5"),"COP","Funds");
    var paid=bookings.confirm(owner,pending.id()); amount("21",paid.payment().amount());
    amount("80",service.card(owner,"client-1").exactBalance());
    now(paid.schedule().checkIn().minus(Duration.ofHours(24)));
    var cancelled=bookings.cancelByClient(owner,paid.id()); amount("17",cancelled.refund().amount());
    amount("97",service.card(owner,"client-1").exactBalance());
    assertEquals("COP",paid.payment().currency());
    service.card(owner,"client-1").movements().forEach(m -> {assertEquals("COP",m.getCurrency());assertEquals(0,m.getExactAmount().scale());});
  }

  @Test void unsupportedCurrencyAndZeroRoundedRechargeHaveNoSideEffects() {
    var before=service.card(owner,"client-1");
    assertThrows(HotelException.class,() -> service.recharge(admin,"client-1",BigDecimal.TEN,"USD","Bad currency"));
    assertThrows(HotelException.class,() -> service.recharge(admin,"client-1",new BigDecimal("0.49"),"COP","Zero"));
    assertEquals(before,service.card(owner,"client-1"));
  }

  @Test void directDetachedAccountingCannotSmuggleFakePaymentsIntoPendingAggregate() {
    hotel.withSession(owner,() -> {
      var r=new Reservation.Builder().client((Client)hotel.currentUser()).room(hotel.findRoom(101)).checkIn(FROM).checkOut(FROM.plusDays(1)).build();
      assertThrows(HotelException.class,() -> r.recordPayment(new Payment(UUID.randomUUID(),r.getId(),"CARD-client-1",BigDecimal.ONE,clock.instant())));
      hotel.addReservation(r); assertNull(r.getPayment()); return null;
    });
  }
}
