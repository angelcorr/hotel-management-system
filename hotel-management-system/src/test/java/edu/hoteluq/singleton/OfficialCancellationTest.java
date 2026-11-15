package edu.hoteluq.singleton;

import edu.hoteluq.config.HotelTime;
import edu.hoteluq.enums.*;
import edu.hoteluq.exceptions.*;
import edu.hoteluq.model.BookingRecords.*;
import edu.hoteluq.service.*;
import edu.hoteluq.strategy.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/** Official section 3.3 through the real authenticated, reservation-linked financial flow. */
class OfficialCancellationTest extends Phase2Fixture {
  private ReservationService bookings;
  @BeforeEach void setupBookings() { bookings = new ReservationService(hotel); room(101); }
  private BookingView pending(LocalDate checkIn) {
    return bookings.create(owner, "client-1", "request", new BookingRequest(101, checkIn, checkIn.plusDays(2), 1, List.of(), null));
  }
  private BookingView paid(LocalDate checkIn, int funds) {
    service.recharge(admin, "client-1", funds, "Funds");
    return bookings.confirm(owner, pending(checkIn).id());
  }
  private void now(Instant at) {
    clock.advance(Duration.between(clock.instant(), at));
    admin = service.login("admin@example.com", adminPassword).token();
    owner = service.login("ana@example.com", ownerPassword).token();
    other = service.login("luis@example.com", ownerPassword).token();
  }
  private void amount(String expected, BigDecimal actual) { assertEquals(0, new BigDecimal(expected).compareTo(actual)); }
  private long movements(MovementType type) {
    return service.card(owner, "client-1").movements().stream().filter(m -> m.getType() == type).count();
  }
  static Stream<Arguments> officialBoundaries() {
    return Stream.of(
        Arguments.of(Duration.ofHours(72).plusNanos(1), "200", "100"),
        Arguments.of(Duration.ofHours(72), "160", "80"),
        Arguments.of(Duration.ofHours(72).minusNanos(1), "160", "80"),
        Arguments.of(Duration.ofHours(48), "160", "80"),
        Arguments.of(Duration.ofHours(24).plusNanos(1), "160", "80"),
        Arguments.of(Duration.ofHours(24), "160", "80"),
        Arguments.of(Duration.ofHours(24).minusNanos(1), "0", "0"),
        Arguments.of(Duration.ZERO, "0", "0"),
        Arguments.of(Duration.ofHours(-1), "0", "0"));
  }
  @ParameterizedTest @MethodSource("officialBoundaries")
  void officialClientWindowsAreExactAndRetriesNeverCreditTwice(Duration left, String returned, String percent) {
    var paid = paid(FROM, 1000);
    now(paid.schedule().checkIn().minus(left));
    var result = bookings.cancelByClient(owner, paid.id());
    assertEquals(ReservationStatus.CANCELLED, result.status());
    amount(returned, result.refund().amount()); amount(percent, result.refund().percent());
    assertEquals(paid.payment().id(), result.refund().paymentId());
    assertEquals(paid.payment().cardNumber(), result.refund().cardNumber());
    assertEquals(paid.payment(), result.payment()); assertEquals(paid.quote(), result.quote());
    assertEquals(left, Duration.between(result.refund().at(), paid.schedule().checkIn()));
    assertEquals("COP", result.refund().currency());
    assertEquals(paid.schedule(), result.schedule()); assertEquals(HotelTime.ZONE, result.schedule().zone());
    assertDoesNotThrow(result.refund().policySnapshot()::requireOfficial);
    assertEquals(result, bookings.cancelByClient(owner, paid.id()));
    assertEquals(result, bookings.cancelByHotel(admin, paid.id()));
    amount(new BigDecimal("800").add(new BigDecimal(returned)).toPlainString(), service.card(owner, "client-1").exactBalance());
    assertEquals(new BigDecimal(returned).signum() == 0 ? 0 : 1, movements(MovementType.REFUND));
    assertEquals(1, result.history().stream().filter(e -> e.type().equals("CANCELLED")).count());
    amount(returned, RefundPolicySelector.forTimeLeft(left).calculateRefund(paid.payment().amount()));
  }

  @ParameterizedTest @ValueSource(longs = {345600, 259200, 43200, -172800})
  void hotelReturnsFullOriginalPaymentRegardlessOfTime(long secondsLeft) {
    var paid = paid(FROM, 1000); now(paid.schedule().checkIn().minusSeconds(secondsLeft));
    service.setCardActive(admin, "client-1", false);
    var result = bookings.cancelByHotel(admin, paid.id());
    amount("200", result.refund().amount()); amount("100", result.refund().percent());
    assertTrue(result.refund().byHotel()); assertEquals(paid.payment().id(), result.refund().paymentId());
    assertEquals(paid.payment().cardNumber(), result.refund().cardNumber());
    amount("1000", service.card(owner, "client-1").exactBalance());
    assertEquals(result, bookings.cancelByHotel(admin, paid.id())); assertEquals(1, movements(MovementType.REFUND));
  }

  private static CancellationSchedule.Window window(Duration lower, Duration upper, String percent) {
    return new CancellationSchedule.Window(lower, upper, new BigDecimal(percent));
  }
  static Stream<CancellationSchedule> incompatibleSchedules() {
    Duration h24 = Duration.ofHours(24), h72 = Duration.ofHours(72), full = CancellationSchedule.FULL_REFUND_START;
    Duration remote = Duration.ofDays(1000);
    return Stream.of(
      new CancellationSchedule("wrong-72", 1, List.of(window(null,h24,"0"), window(h24,h72,"80"), window(h72,null,"100"))),
      new CancellationSchedule("wrong-under24", 1, List.of(window(null,h24,"40"), window(h24,full,"80"), window(full,null,"100"))),
      new CancellationSchedule("wrong-late", 1, List.of(window(null,Duration.ZERO,"25"), window(Duration.ZERO,h24,"0"), window(h24,full,"80"), window(full,null,"100"))),
      new CancellationSchedule("wrong-24", 1, List.of(window(null,h24.plusNanos(1),"0"), window(h24.plusNanos(1),full,"80"), window(full,null,"100"))),
      new CancellationSchedule("wrong-remote-nanosecond", 1, List.of(window(null,h24,"0"), window(h24,full,"80"), window(full,remote,"100"), window(remote,remote.plusNanos(1),"0"), window(remote.plusNanos(1),null,"100"))));
  }
  @ParameterizedTest @MethodSource("incompatibleSchedules")
  void incompatibleActivationHasNoFinancialOrRegistrySideEffects(CancellationSchedule candidate) {
    var paid = paid(FROM, 1000); var cardBefore = service.card(owner,"client-1"); var historyBefore = bookings.policyHistory(admin);
    assertThrows(HotelException.class, () -> bookings.configureCancellation(admin,candidate));
    assertEquals(paid,bookings.get(owner,paid.id())); assertEquals(cardBefore,service.card(owner,"client-1"));
    assertEquals(historyBefore,bookings.policyHistory(admin));
    // A rejected activation must not consume its ID/version or poison the registry.
    var correct = new CancellationSchedule(candidate.id(),candidate.version(),CancellationSchedule.official().windows());
    bookings.configureCancellation(admin,correct);
    now(paid.schedule().checkIn().minus(Duration.ofHours(48)));
    var result = bookings.cancelByClient(owner,paid.id()); amount("160",result.refund().amount());
    assertEquals(correct,result.refund().policySnapshot());
  }

  @ParameterizedTest @CsvSource({
      "2027-01-01,2026-12-29T20:00:00Z,2026-12-29,160",
      "2027-03-01,2027-02-28T20:00:00Z,2027-02-28,160",
      "2027-02-01,2027-01-31T20:00:00.000000001Z,2027-01-31,0",
      "2027-01-03,2027-01-01T04:59:59Z,2026-12-31,160",
      "2027-01-03,2027-01-01T05:00:00Z,2027-01-01,160"})
  void dayMonthAndYearBoundariesUseTheOriginalBogotaCheckIn(String arrival, String cancelledAt, String localDay, String returned) {
    var paid = paid(LocalDate.parse(arrival),1000); now(Instant.parse(cancelledAt));
    var result = bookings.cancelByClient(owner,paid.id()); amount(returned,result.refund().amount());
    assertEquals(Instant.parse(cancelledAt),result.refund().at());
    assertEquals(LocalDate.parse(localDay),HotelTime.local(result.refund().at()).toLocalDate());
    assertEquals(paid.schedule(),result.schedule()); assertEquals(LocalDate.parse(arrival),result.quote().checkIn());
  }

  @Test void failedConfirmationWithInsufficientFundsCannotMintARefund() {
    var pending = pending(FROM); var before = service.card(owner,"client-1");
    assertThrows(InsufficientBalanceException.class,() -> bookings.confirm(owner,pending.id()));
    assertEquals(pending,bookings.get(owner,pending.id())); assertEquals(before,service.card(owner,"client-1"));
    var cancelled = bookings.cancelByClient(owner,pending.id());
    assertNull(cancelled.payment()); assertNull(cancelled.refund().paymentId()); amount("0",cancelled.refund().amount());
    assertEquals(before,service.card(owner,"client-1"));
  }
  @Test void refundCreditsTheOriginalCardEvenWhenItsCurrentBalanceIsZero() {
    var paid = paid(FROM,200); amount("0",service.card(owner,"client-1").exactBalance());
    now(paid.schedule().checkIn().minus(Duration.ofHours(48)));
    var cancelled = bookings.cancelByClient(owner,paid.id()); amount("160",cancelled.refund().amount());
    amount("160",service.card(owner,"client-1").exactBalance());
    assertEquals(paid.payment().id(),cancelled.refund().paymentId()); assertEquals(1,movements(MovementType.REFUND));
  }
  @Test void equivalentSplitsAndVersionChangesPreserveTheFirstRefundReceipt() {
    Duration h24=Duration.ofHours(24), h48=Duration.ofHours(48), full=CancellationSchedule.FULL_REFUND_START;
    var split = new CancellationSchedule("equivalent",1,List.of(window(null,Duration.ZERO,"0.0"),window(Duration.ZERO,h24,"0"),
        window(h24,h48,"80.00"),window(h48,full,"80"),window(full,null,"100.000")));
    bookings.configureCancellation(admin,split); var paid = paid(FROM,1000); now(paid.schedule().checkIn().minus(Duration.ofHours(72)));
    var cancelled=bookings.cancelByClient(owner,paid.id()); amount("160",cancelled.refund().amount());
    bookings.configureCancellation(admin,new CancellationSchedule("equivalent",2,CancellationSchedule.official().windows()));
    assertEquals(cancelled,bookings.cancelByClient(owner,paid.id())); assertEquals(split,cancelled.refund().policySnapshot());
    assertEquals(1,cancelled.refund().policyVersion()); assertEquals(1,movements(MovementType.REFUND));
  }
  @Test void concurrentClientCancellationsAt48HoursCreditOnlyOnePartialRefund() throws Exception {
    var paid=paid(FROM,1000); now(paid.schedule().checkIn().minus(Duration.ofHours(48)));
    assertEquals(2,race(() -> bookings.cancelByClient(owner,paid.id()),() -> bookings.cancelByClient(owner,paid.id())));
    amount("960",service.card(owner,"client-1").exactBalance()); assertEquals(1,movements(MovementType.REFUND));
    var result=bookings.get(owner,paid.id()); amount("160",result.refund().amount());
    assertEquals(1,result.history().stream().filter(e -> e.type().equals("CANCELLED")).count());
  }
  @Test void concurrentClientAndHotelCancellationCommitOneConsistentDecision() throws Exception {
    var paid=paid(FROM,1000); now(paid.schedule().checkIn().minus(Duration.ofHours(48)));
    assertEquals(2,race(() -> bookings.cancelByClient(owner,paid.id()),() -> bookings.cancelByHotel(admin,paid.id())));
    var result=bookings.get(owner,paid.id()); String returned=result.refund().byHotel()?"200":"160";
    amount(returned,result.refund().amount());
    amount(new BigDecimal("800").add(new BigDecimal(returned)).toPlainString(),service.card(owner,"client-1").exactBalance());
    assertEquals(1,movements(MovementType.REFUND));
    assertEquals(result,bookings.cancelByClient(owner,paid.id())); assertEquals(result,bookings.cancelByHotel(admin,paid.id()));
  }
  @Test void officialRulesDoNotGrantOwnerAdministratorOrForeignUserPrivileges() {
    var paid=paid(FROM,1000); var before=service.card(owner,"client-1");
    assertThrows(AuthenticationException.class,() -> bookings.cancelByClient("bad",paid.id()));
    assertThrows(AuthorizationException.class,() -> bookings.cancelByClient(other,paid.id()));
    assertThrows(AuthorizationException.class,() -> bookings.cancelByHotel(owner,paid.id()));
    assertThrows(AuthorizationException.class,() -> bookings.cancelByClient(admin,paid.id()));
    assertThrows(AuthorizationException.class,() -> bookings.configureCancellation(owner,CancellationSchedule.official()));
    assertEquals(paid,bookings.get(owner,paid.id())); assertEquals(before,service.card(owner,"client-1"));
  }
  @Test void nonofficialHistoricalSnapshotsRemainReadableButCannotBeActivated() {
    var legacy=new CancellationSchedule("legacy-archive",1,List.of(window(null,Duration.ZERO,"25"),window(Duration.ZERO,null,"40")));
    amount("25",legacy.select(Duration.ofHours(-1)).calculateRefund(new BigDecimal("100")));
    assertThrows(HotelException.class,() -> bookings.configureCancellation(admin,legacy));
    var paid=paid(FROM,1000); now(paid.schedule().checkIn().plusSeconds(1));
    var result=bookings.cancelByClient(owner,paid.id()); amount("0",result.refund().amount());
    assertNotEquals(legacy,result.refund().policySnapshot()); assertEquals(0,movements(MovementType.REFUND));
  }
}
