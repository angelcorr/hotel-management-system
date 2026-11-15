package edu.hoteluq.loyalty;

import edu.hoteluq.model.BookingRecords.*;
import edu.hoteluq.exceptions.HotelException;
import edu.hoteluq.config.HotelTime;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static edu.hoteluq.loyalty.LoyaltyPolicy.*;

class PointsLedgerTest {
  private final PointsLedger ledger=new PointsLedger();
  private final Instant now=Instant.parse("2026-11-13T17:00:00Z");
  private LoyaltyPolicy policy(long version,RefundTreatment refund,RedemptionCancellation restoration) {
    return new LoyaltyPolicy("TEST-ONLY",version,new BigDecimal("100000"),100,refund,SpentTreatment.CARRY_FORWARD_DEBT,restoration);
  }
  private void configure() { ledger.configure(policy(1,RefundTreatment.RECALCULATE_RETAINED_PAYMENT,RedemptionCancellation.RESTORE_ORIGINAL_EXPIRY),"admin",now); }
  private Payment payment(String client,String amount,Instant at) {
    Payment p=new Payment(UUID.randomUUID(),UUID.randomUUID(),"CARD-"+client,new BigDecimal(amount),at);ledger.paid(client,p);return p;
  }
  private Benefit benefit(long points) { return new Benefit(UUID.randomUUID(),1,"fixture",points,Benefit.Kind.RESERVATION_DISCOUNT,BigDecimal.ONE,null); }
  private PointsLedger.Redemption redeem(long points,Instant at) { return ledger.redeem("a",UUID.randomUUID(),0,UUID.randomUUID().toString(),benefit(points),at); }
  private Refund refund(Payment p,String amount,Instant at) {
    return new Refund(UUID.randomUUID(),p.reservationId(),p.id(),p.cardNumber(),new BigDecimal(amount),"official",true,at,"COP","HOTEL_FULL_REFUND",1,new BigDecimal("100"),Duration.ZERO,null);
  }
  @Test void unconfiguredPaymentsAreDeferredRatherThanInventingRate() {
    var p=payment("a","100000",now);assertEquals(0,ledger.account("a",now).available());assertEquals(1,ledger.deferred("a").size());
    assertThrows(HotelException.class,()->redeem(1,now)); assertThrows(HotelException.class,()->ledger.settle(p.id(),now));
  }
  @Test void deferredSettlementPreservesOriginalExpiryAndIsIdempotent() {
    var p=payment("a","100000",now);configure();ledger.settle(p.id(),now.plusSeconds(10));ledger.settle(p.id(),now.plusSeconds(11));
    var account=ledger.account("a",now);assertEquals(100,account.available());assertEquals(now,account.lots().getFirst().obtainedAt());assertEquals(PointsLedger.expiry(now),account.lots().getFirst().expiresAt());
  }
  @Test void cancelledDeferredCannotBeCreditedLater() {
    var p=payment("a","100000",now);ledger.refunded("a",refund(p,"100000",now),p);configure();assertThrows(HotelException.class,()->ledger.settle(p.id(),now));
  }
  @Test void paidRetryCreditsOnceAndFloorsFractionalPoints() {
    configure();var p=payment("a","100999",now);ledger.paid("a",p);assertEquals(100,ledger.account("a",now).available());assertEquals(1,ledger.account("a",now).lots().size());
  }
  @ParameterizedTest @CsvSource({"2024-02-29T17:00:00Z,2025-02-28T17:00:00Z","2026-12-31T04:59:59Z,2027-12-31T04:59:59Z","2026-12-31T05:00:00Z,2027-12-31T05:00:00Z","2026-01-01T04:59:59Z,2027-01-01T04:59:59Z"})
  void anniversaryUsesBogotaCalendar(String input,String expected) { assertEquals(Instant.parse(expected),PointsLedger.expiry(Instant.parse(input))); }
  @Test void expiryExactBoundaryAndTraceAreIdempotent() {
    configure();payment("a","100000",now);Instant end=PointsLedger.expiry(now);
    assertEquals(100,ledger.account("a",end.minusNanos(1)).available());assertEquals(0,ledger.account("a",end).available());
    assertThrows(HotelException.class,()->redeem(1,end));ledger.account("a",end.plusSeconds(1));
    var events=ledger.account("a",end).history().stream().filter(e->e.type().equals("EXPIRED")).toList();assertEquals(1,events.size());assertEquals(end,events.getFirst().effectiveAt());
  }
  @Test void hostZoneDoesNotChangeAnniversary() {
    TimeZone old=TimeZone.getDefault();try {TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));assertEquals(now.atZone(HotelTime.ZONE).plusYears(1).toInstant(),PointsLedger.expiry(now));}finally{TimeZone.setDefault(old);}
  }
  @Test void consumesEarliestExpiryNotInsertionOrder() {
    configure();var later=payment("a","100000",now.plusSeconds(10));var earlier=payment("a","100000",now);
    var r=redeem(150,now.plusSeconds(11));assertEquals(earlier.id(),r.allocations().getFirst().paymentId());assertEquals(100,r.allocations().getFirst().points());assertEquals(later.id(),r.allocations().getLast().paymentId());assertEquals(50,ledger.account("a",now.plusSeconds(11)).available());
  }
  @Test void insufficientPointsLeaveNoRedemption() {
    configure();payment("a","100000",now);assertThrows(HotelException.class,()->redeem(101,now));assertEquals(100,ledger.account("a",now).available());assertTrue(ledger.account("a",now).redemptions().isEmpty());
  }
  @Test void duplicateKeyAndConflictingRetriesAreRejected() {
    configure();payment("a","100000",now);var r=redeem(50,now);
    assertEquals(r,ledger.redeem("a",r.reservationId(),0,r.key(),r.benefit(),now.plusSeconds(1)));
    assertThrows(HotelException.class,()->ledger.redeem("a",UUID.randomUUID(),0,r.key(),r.benefit(),now));
    assertThrows(HotelException.class,()->ledger.redeem("a",r.reservationId(),1,r.key(),r.benefit(),now));assertEquals(50,ledger.account("a",now).available());
  }
  @Test void refundRecalculatesActualRetainedPaymentAndIsIdempotent() {
    configure();var p=payment("a","100000",now);var f=refund(p,"80000",now);ledger.refunded("a",f,p);ledger.refunded("a",f,p);assertEquals(20,ledger.account("a",now).available());
  }
  @Test void spentAwardRefundCreatesDebtAndFutureCreditsCannotBeDoubleSpent() {
    configure();var p=payment("a","100000",now);redeem(80,now);ledger.refunded("a",refund(p,"100000",now),p);
    assertEquals(80,ledger.account("a",now).debt());assertEquals(0,ledger.account("a",now).available());payment("a","100000",now);
    assertEquals(20,ledger.account("a",now).available());assertEquals(0,ledger.account("a",now).debt());assertThrows(HotelException.class,()->redeem(21,now));
  }
  @Test void restorationAfterClawbackDoesNotResurrectCancelledEarnings() {
    configure();var original=payment("a","100000",now);var r=redeem(80,now);ledger.refunded("a",refund(original,"100000",now),original);
    var spending=new Payment(UUID.randomUUID(),r.reservationId(),"CARD-a",BigDecimal.ONE,now);ledger.paid("a",spending);
    var cancellation=refund(spending,"1",now);ledger.refunded("a",cancellation,spending);ledger.refunded("a",cancellation,spending);
    assertEquals(0,ledger.account("a",now).available());assertEquals(0,ledger.account("a",now).debt());assertTrue(ledger.redemption(r.reservationId()).restored());
  }
  @Test void restorationKeepsOriginalExpiryAndNeverRevivesExpiredPoints() {
    configure();payment("a","100000",now);var r=redeem(50,now);Instant end=PointsLedger.expiry(now);
    var p=new Payment(UUID.randomUUID(),r.reservationId(),"CARD-a",BigDecimal.ONE,now);ledger.paid("a",p);ledger.refunded("a",refund(p,"1",end),p);
    assertEquals(0,ledger.account("a",end).available());assertTrue(ledger.redemption(r.reservationId()).restored());
  }
  @Test void historicalRulesSurvivePolicyModification() {
    configure();var p=payment("a","100000",now);ledger.configure(policy(2,RefundTreatment.REVOKE_ALL_ON_CANCELLATION,RedemptionCancellation.KEEP_SPENT),"admin",now);
    ledger.refunded("a",refund(p,"80000",now),p);assertEquals(20,ledger.account("a",now).available());assertEquals(1,ledger.account("a",now).lots().getFirst().policy().version());
  }
  @Test void allOnCancellationOptionAlsoRevokesRetainedCashAwards() {
    ledger.configure(policy(1,RefundTreatment.REVOKE_ALL_ON_CANCELLATION,RedemptionCancellation.KEEP_SPENT),"admin",now);var p=payment("a","100000",now);ledger.refunded("a",refund(p,"0",now),p);assertEquals(0,ledger.account("a",now).available());
  }
  @Test void checkpointRestoresAllFinancialAndAuditEntries() {
    configure();var p=payment("a","100000",now);var before=ledger.account("a",now);Object checkpoint=ledger.checkpoint();redeem(50,now);ledger.refunded("a",refund(p,"100000",now),p);ledger.restore(checkpoint);assertEquals(before,ledger.account("a",now));
  }
  @Test void configurationVersionCannotBeOverwritten() {
    configure();assertThrows(HotelException.class,()->ledger.configure(policy(1,RefundTreatment.REVOKE_ALL_ON_CANCELLATION,RedemptionCancellation.KEEP_SPENT),"admin",now));
  }
  @ParameterizedTest @ValueSource(longs={0,-1,Long.MIN_VALUE}) void invalidPointUnits(long unit) {
    assertThrows(HotelException.class,()->new LoyaltyPolicy("x",1,BigDecimal.ONE,unit,RefundTreatment.REVOKE_ALL_ON_CANCELLATION,SpentTreatment.CARRY_FORWARD_DEBT,RedemptionCancellation.KEEP_SPENT));
  }
  @ParameterizedTest @ValueSource(strings={"0","-1"}) void invalidPaidUnits(String unit) {
    assertThrows(HotelException.class,()->new LoyaltyPolicy("x",1,new BigDecimal(unit),1,RefundTreatment.REVOKE_ALL_ON_CANCELLATION,SpentTreatment.CARRY_FORWARD_DEBT,RedemptionCancellation.KEEP_SPENT));
  }
  @Test void cancellingBothSpentSourceAndRepaymentCreditsLeavesConsistentDebt() {
    configure();var source=payment("a","100000",now);var r=redeem(80,now);ledger.refunded("a",refund(source,"100000",now),source);
    var other=payment("a","100000",now);assertEquals(20,ledger.account("a",now).available());
    ledger.refunded("a",refund(other,"100000",now),other);assertEquals(80,ledger.account("a",now).debt());
    var used=new Payment(UUID.randomUUID(),r.reservationId(),"CARD-a",BigDecimal.ONE,now);ledger.paid("a",used);ledger.refunded("a",refund(used,"1",now),used);
    assertEquals(0,ledger.account("a",now).debt());assertEquals(0,ledger.account("a",now).available());
  }
  @Test void keepSpentOptionDoesNotRestoreRedeemedPointsOnCancellation() {
    ledger.configure(policy(1,RefundTreatment.RECALCULATE_RETAINED_PAYMENT,RedemptionCancellation.KEEP_SPENT),"admin",now);payment("a","100000",now);var r=redeem(50,now);
    var p=new Payment(UUID.randomUUID(),r.reservationId(),"CARD-a",BigDecimal.ONE,now);ledger.paid("a",p);ledger.refunded("a",refund(p,"1",now),p);
    assertEquals(50,ledger.account("a",now).available());assertFalse(ledger.redemption(r.reservationId()).restored());
  }

}
