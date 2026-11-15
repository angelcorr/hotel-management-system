package edu.hoteluq.singleton;

import edu.hoteluq.loyalty.*;
import edu.hoteluq.service.*;
import edu.hoteluq.model.BookingRecords.*;
import edu.hoteluq.enums.*;
import edu.hoteluq.exceptions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static edu.hoteluq.loyalty.LoyaltyPolicy.*;

class LoyaltyIntegrationTest extends Phase2Fixture {
  private LoyaltyService loyalty;
  private ReservationService bookings;
  private int roomNumber=100;
  @BeforeEach void loyaltySetup() {loyalty=new LoyaltyService(hotel);bookings=new ReservationService(hotel);}
  private void configure() {
    // Deliberately explicit fixtures, NOT an approved production equivalence.
    loyalty.configure(admin,new LoyaltyPolicy("TEST-ONLY",1,BigDecimal.ONE,1,RefundTreatment.RECALCULATE_RETAINED_PAYMENT,SpentTreatment.CARRY_FORWARD_DEBT,RedemptionCancellation.RESTORE_ORIGINAL_EXPIRY));
  }
  private BookingView pending() {
    int number=++roomNumber;room(number);
    return bookings.create(owner,"client-1",UUID.randomUUID().toString(),new BookingRequest(number,FROM,FROM.plusDays(2),1,List.of(),null));
  }
  private BookingView earn() {
    configure();service.recharge(admin,"client-1",1000,"Fixture funds");return bookings.confirm(owner,pending().id());
  }
  private Benefit discount(long cost,String amount) {
    var b=new Benefit(UUID.randomUUID(),1,"Test discount",cost,Benefit.Kind.RESERVATION_DISCOUNT,new BigDecimal(amount),null);loyalty.benefit(admin,b);return b;
  }
  private PointsLedger.Account account() {return loyalty.account(owner,"client-1");}
  private void now(Instant instant) {
    clock.advance(Duration.between(clock.instant(),instant));admin=service.login("admin@example.com",adminPassword).token();owner=service.login("ana@example.com",ownerPassword).token();other=service.login("luis@example.com",ownerPassword).token();
  }
  @Test void everyClientHasAnIsolatedEmptyAccount() {
    assertEquals(0,account().available());assertTrue(account().lots().isEmpty());assertEquals("client-1",account().clientId());assertEquals(0,loyalty.account(other,"client-2").available());
  }
  @Test void onlyOwnerOrAdminCanConsultAccount() {
    assertThrows(AuthorizationException.class,()->loyalty.account(other,"client-1"));assertThrows(AuthenticationException.class,()->loyalty.account("bad","client-1"));assertDoesNotThrow(()->loyalty.account(admin,"client-1"));
  }
  @Test void onlyAdminCanConfigurePolicyBenefitsAndPackages() {
    assertThrows(AuthorizationException.class,()->loyalty.configure(owner,new LoyaltyPolicy("x",1,BigDecimal.ONE,1,RefundTreatment.REVOKE_ALL_ON_CANCELLATION,SpentTreatment.CARRY_FORWARD_DEBT,RedemptionCancellation.KEEP_SPENT)));
    assertThrows(AuthorizationException.class,()->loyalty.benefit(owner,new Benefit(UUID.randomUUID(),1,"x",1,Benefit.Kind.RESERVATION_DISCOUNT,BigDecimal.ONE,null)));
    assertThrows(AuthorizationException.class,()->loyalty.createPackage(owner,"x","x",List.of(),List.of()));
  }
  @Test void rechargeAndRejectedPaymentDoNotEarnPoints() {
    configure();var p=pending();assertThrows(HotelException.class,()->bookings.confirm(owner,p.id()));assertTrue(account().lots().isEmpty());assertTrue(account().history().isEmpty());
    service.recharge(admin,"client-1",1000,"Fixture funds");assertEquals(0,account().available());bookings.confirm(owner,p.id());assertEquals(200,account().available());
  }
  @Test void paidReservationCreditsActualPaymentOnce() {
    var paid=earn();assertEquals(200,account().available());assertEquals(paid.payment().id(),account().lots().getFirst().paymentId());assertEquals(paid.id(),account().lots().getFirst().reservationId());assertEquals(paid.payment().at(),account().lots().getFirst().obtainedAt());
    bookings.confirm(owner,paid.id());assertEquals(1,account().lots().size());
  }
  @Test void absenceOfRateDefersAndRequiresExplicitAdminSettlement() {
    service.recharge(admin,"client-1",1000,"Fixture funds");var paid=bookings.confirm(owner,pending().id());assertEquals(0,account().available());assertEquals(1,loyalty.deferred(owner,"client-1").size());
    configure();assertEquals(0,account().available());assertThrows(AuthorizationException.class,()->loyalty.settle(owner,"client-1",paid.payment().id()));
    loyalty.settle(admin,"client-1",paid.payment().id());loyalty.settle(admin,"client-1",paid.payment().id());assertEquals(200,account().available());
  }
  @Test void undefinedBenefitIsBlockedWithoutCharge() {
    earn();var p=pending();var before=service.card(owner,"client-1");assertThrows(HotelException.class,()->loyalty.redeem(owner,p.id(),p.version(),"r",UUID.randomUUID()));assertEquals(before,service.card(owner,"client-1"));
  }
  @Test void discountIsGrantedAndCashAndAccrualUseReducedAmount() {
    earn();var b=discount(50,"20");var p=pending();var paid=loyalty.redeem(owner,p.id(),p.version(),"r",b.id());
    assertEquals(new BigDecimal("180"),paid.payment().amount());assertEquals(paid.quote().total(),paid.payment().amount());assertEquals(new BigDecimal("20"),paid.quote().rawDiscount());assertEquals(330,account().available());assertEquals(1,account().redemptions().size());
    assertEquals(paid,loyalty.redeem(owner,p.id(),p.version(),"r",b.id()));assertEquals(paid,bookings.confirm(owner,p.id()));assertEquals(330,account().available());
  }
  @Test void insufficientPointsCannotBorrowPointsFromCurrentPayment() {
    configure();service.recharge(admin,"client-1",1000,"Fixture funds");var b=discount(1,"20");var p=pending();var card=service.card(owner,"client-1");
    assertThrows(HotelException.class,()->loyalty.redeem(owner,p.id(),p.version(),"r",b.id()));assertEquals(p,bookings.get(owner,p.id()));assertEquals(card,service.card(owner,"client-1"));assertTrue(account().lots().isEmpty());
  }
  @Test void staleVersionCannotSpendOrChangeQuote() {
    earn();var p=pending();var b=discount(50,"20");var before=account();assertThrows(HotelException.class,()->loyalty.redeem(owner,p.id(),p.version()+1,"r",b.id()));assertEquals(before,account());assertEquals(p,bookings.get(owner,p.id()));
  }
  @Test void anotherClientCannotRedeemOwnersPoints() {
    earn();var p=pending();var b=discount(50,"20");assertThrows(AuthorizationException.class,()->loyalty.redeem(other,p.id(),p.version(),"r",b.id()));assertEquals(200,account().available());
  }
  @Test void confirmedReservationCannotReceiveNewBenefit() {
    var paid=earn();var b=discount(50,"20");assertThrows(HotelException.class,()->loyalty.redeem(owner,paid.id(),paid.version(),"r",b.id()));assertEquals(200,account().available());
  }
  @Test void zeroCashBenefitIsRejectedBeforeAnyMovement() {
    earn();var p=pending();var b=discount(50,"200");var card=service.card(owner,"client-1");var before=account();
    assertThrows(HotelException.class,()->loyalty.redeem(owner,p.id(),p.version(),"r",b.id()));assertEquals(p,bookings.get(owner,p.id()));assertEquals(card,service.card(owner,"client-1"));assertEquals(before,account());
  }
  @ParameterizedTest @EnumSource(Hotel.Stage.class)
  void partialConfirmationFailureRestoresQuoteCardPointsAndHistory(Hotel.Stage stage) {
    earn();var p=pending();var b=discount(50,"20");var card=service.card(owner,"client-1");var before=account();
    hotel.transactionProbe=s->{if(s==stage)throw new HotelException("Injected failure");};
    assertThrows(HotelException.class,()->loyalty.redeem(owner,p.id(),p.version(),"r",b.id()));hotel.transactionProbe=s->{};
    assertEquals(p,bookings.get(owner,p.id()));assertEquals(card,service.card(owner,"client-1"));assertEquals(before,account());assertDoesNotThrow(()->loyalty.redeem(owner,p.id(),p.version(),"r",b.id()));
  }
  @ParameterizedTest @EnumSource(Hotel.Stage.class)
  void failedCancellationRestoresMoneyAwardAndRedemption(Hotel.Stage stage) {
    earn();var b=discount(50,"20");var p=pending();var paid=loyalty.redeem(owner,p.id(),p.version(),"r",b.id());var before=account();var card=service.card(owner,"client-1");
    hotel.transactionProbe=s->{if(s==stage)throw new HotelException("Injected refund failure");};assertThrows(HotelException.class,()->bookings.cancelByHotel(admin,p.id()));hotel.transactionProbe=s->{};
    assertEquals(before,account());assertEquals(card,service.card(owner,"client-1"));assertEquals(paid,bookings.get(owner,p.id()));
  }
  @Test void refundRestoresRedemptionAndRevokesCashAwardExactlyOnce() {
    earn();var b=discount(50,"20");var p=pending();loyalty.redeem(owner,p.id(),p.version(),"r",b.id());var cancelled=bookings.cancelByHotel(admin,p.id());assertEquals(new BigDecimal("180"),cancelled.refund().amount());assertEquals(200,account().available());assertTrue(account().redemptions().getFirst().restored());
    bookings.cancelByHotel(admin,p.id());assertEquals(200,account().available());
  }
  @ParameterizedTest @CsvSource({"73,200,0","72,160,40","24,160,40","23,0,200"})
  void officialCancellationCashWindowsRemainUnchangedAndAdjustAward(int hours,String refund,long points) {
    var paid=earn();now(paid.schedule().checkIn().minus(Duration.ofHours(hours)));var cancelled=bookings.cancelByClient(owner,paid.id());assertEquals(new BigDecimal(refund),cancelled.refund().amount());assertEquals(points,account().available());
  }
  @Test void concurrentConfirmationsCreditOnlyOneLot() throws Exception {
    configure();service.recharge(admin,"client-1",1000,"Funds");var p=pending();assertEquals(2,race(()->bookings.confirm(owner,p.id()),()->bookings.confirm(owner,p.id())));assertEquals(1,account().lots().size());assertEquals(200,account().available());
  }
  @Test void concurrentRedemptionRetriesProduceOneGrantAndPayment() throws Exception {
    earn();var b=discount(50,"20");var p=pending();assertEquals(2,race(()->loyalty.redeem(owner,p.id(),p.version(),"same",b.id()),()->loyalty.redeem(owner,p.id(),p.version(),"same",b.id())));assertEquals(1,account().redemptions().size());assertEquals(2,account().lots().size());
  }
  @Test void concurrentDifferentReservationsCannotOverspend() throws Exception {
    earn();var b=discount(200,"199");var p=pending();var q=pending();assertEquals(1,race(()->loyalty.redeem(owner,p.id(),p.version(),"one",b.id()),()->loyalty.redeem(owner,q.id(),q.version(),"two",b.id())));assertEquals(1,account().redemptions().size());assertEquals(1,account().available());
  }
  @Test void concurrentCancellationDoesNotReverseAwardTwice() throws Exception {
    var paid=earn();assertEquals(2,race(()->bookings.cancelByHotel(admin,paid.id()),()->bookings.cancelByHotel(admin,paid.id())));assertEquals(0,account().available());assertEquals(1,account().history().stream().filter(e->e.type().equals("REFUND_ADJUSTMENT")).count());
  }
  @Test void serviceBenefitIncludesExactlyOneFreeServiceWithRealSnapshot() {
    earn();var s=service.createService(admin,"Spa",new BigDecimal("25.5"),"COP");var b=new Benefit(UUID.randomUUID(),1,"Spa benefit",50,Benefit.Kind.SERVICE,null,s.id());loyalty.benefit(admin,b);var p=pending();var paid=loyalty.redeem(owner,p.id(),p.version(),"spa",b.id());
    assertEquals(new BigDecimal("200"),paid.payment().amount());assertEquals(1,paid.quote().services().size());assertEquals(new BigDecimal("25.5"),paid.quote().rawDiscount());assertEquals(350,account().available());
  }
  @Test void nestedPackageIsCompositeAndRoundsOnce() {
    earn();var a=service.createService(admin,"A",new BigDecimal("0.4"),"COP");var b=service.createService(admin,"B",new BigDecimal("0.4"),"COP");var one=loyalty.createPackage(admin,"One","First",List.of(a.id()),List.of());var both=loyalty.createPackage(admin,"Both","Nested",List.of(b.id()),List.of(one.id()));
    assertEquals(new BigDecimal("0.8"),both.rawPrice());assertEquals(BigDecimal.ONE,both.price());assertEquals("COP",both.currency());
    var benefit=new Benefit(UUID.randomUUID(),1,"Package",50,Benefit.Kind.PACKAGE,null,both.id());loyalty.benefit(admin,benefit);var p=pending();var paid=loyalty.redeem(owner,p.id(),p.version(),"package",benefit.id());
    assertEquals(2,paid.quote().services().size());assertEquals(new BigDecimal("200"),paid.payment().amount());assertEquals(new BigDecimal("0.8"),paid.quote().rawDiscount());
  }
  @Test void packageRejectsDuplicatesInvalidReferencesAndEmptyContents() {
    var s=service.createService(admin,"Spa",10);assertThrows(HotelException.class,()->loyalty.createPackage(admin,"Empty","Empty",List.of(),List.of()));
    assertThrows(HotelException.class,()->loyalty.createPackage(admin,"Bad","Bad",List.of(UUID.randomUUID()),List.of()));
    assertThrows(HotelException.class,()->loyalty.createPackage(admin,"Bad","Bad",List.of(),List.of(UUID.randomUUID())));
    var p=loyalty.createPackage(admin,"Valid","Valid",List.of(s.id()),List.of());assertThrows(HotelException.class,()->loyalty.createPackage(admin,"Duplicate","Duplicate",List.of(s.id()),List.of(p.id())));
    assertThrows(HotelException.class,()->hotel.withSession(admin,()->{hotel.removeService(s.id());return null;}));
  }
  @Test void unavailableServiceBlocksBenefitWithoutSpending() {
    earn();var s=service.createService(admin,"Spa",10);var b=new Benefit(UUID.randomUUID(),1,"Spa",50,Benefit.Kind.SERVICE,null,s.id());loyalty.benefit(admin,b);var p=pending();hotel.withSession(admin,()->{hotel.findService(s.id()).setAvailable(false);return null;});
    var before=account();assertThrows(HotelException.class,()->loyalty.redeem(owner,p.id(),p.version(),"spa",b.id()));assertEquals(before,account());assertEquals(p,bookings.get(owner,p.id()));
  }
  @Test void benefitUpdateDoesNotRewriteReceiptOrInvalidateIdenticalRetry() {
    earn();var b=discount(50,"20");var p=pending();var paid=loyalty.redeem(owner,p.id(),p.version(),"r",b.id());loyalty.benefit(admin,new Benefit(b.id(),2,b.name(),100,b.kind(),new BigDecimal("50"),null));
    assertEquals(paid,loyalty.redeem(owner,p.id(),p.version(),"r",b.id()));assertEquals(b,account().redemptions().getFirst().benefit());assertThrows(HotelException.class,()->loyalty.redeem(owner,p.id(),p.version(),"different",b.id()));
  }
  @Test void failedNormalConfirmationLeavesNoDeferredSource() {
    service.recharge(admin,"client-1",1000,"Funds");var p=pending();hotel.transactionProbe=s->{if(s==Hotel.Stage.AFTER_RECEIPT)throw new HotelException("failure");};assertThrows(HotelException.class,()->bookings.confirm(owner,p.id()));hotel.transactionProbe=s->{};assertTrue(loyalty.deferred(owner,"client-1").isEmpty());assertEquals(p,bookings.get(owner,p.id()));
  }
  @Test void redeemedServiceAlreadySelectedIsNotDuplicated() {
    earn();var s=service.createService(admin,"Spa",10);var b=new Benefit(UUID.randomUUID(),1,"Spa",50,Benefit.Kind.SERVICE,null,s.id());loyalty.benefit(admin,b);int n=++roomNumber;room(n);var p=bookings.create(owner,"client-1","with-service",new BookingRequest(n,FROM,FROM.plusDays(2),1,List.of(s.id()),null));var paid=loyalty.redeem(owner,p.id(),p.version(),"r",b.id());assertEquals(1,paid.quote().services().size());assertEquals(new BigDecimal("200"),paid.payment().amount());
  }
  @Test void insufficientCashAfterPointDebitRollsBackEntireRedemption() {
    earn();var b=discount(50,"20");int n=++roomNumber;room(n);
    var p=bookings.create(owner,"client-1","expensive",new BookingRequest(n,FROM,FROM.plusDays(10),1,List.of(),null));var before=account();var card=service.card(owner,"client-1");
    assertThrows(HotelException.class,()->loyalty.redeem(owner,p.id(),p.version(),"r",b.id()));assertEquals(before,account());assertEquals(card,service.card(owner,"client-1"));assertEquals(p,bookings.get(owner,p.id()));
  }
  @Test void expiredCreditsCannotFundRedemptionAtExactAnniversary() {
    var paid=earn();var b=discount(50,"20");now(PointsLedger.expiry(paid.payment().at()));int n=++roomNumber;room(n);LocalDate arrival=LocalDate.of(2030,1,10);
    var p=bookings.create(owner,"client-1","future",new BookingRequest(n,arrival,arrival.plusDays(2),1,List.of(),null));var card=service.card(owner,"client-1");
    assertEquals(0,account().available());assertThrows(HotelException.class,()->loyalty.redeem(owner,p.id(),p.version(),"r",b.id()));assertEquals(card,service.card(owner,"client-1"));assertEquals(p,bookings.get(owner,p.id()));
  }
  @Test void concurrentExpiryQueriesRecordOneExpirationEvent() throws Exception {
    var paid=earn();now(PointsLedger.expiry(paid.payment().at()));assertEquals(2,race(()->loyalty.account(owner,"client-1"),()->loyalty.account(owner,"client-1")));
    assertEquals(0,account().available());assertEquals(1,account().history().stream().filter(e->e.type().equals("EXPIRED")).count());
  }
  @Test void paidRoomAndServicesAccrueOnlyActualCopPayment() {
    configure();service.recharge(admin,"client-1",1000,"Funds");var s=service.createService(admin,"Spa",new BigDecimal("25.5"),"COP");int n=++roomNumber;room(n);
    var p=bookings.create(owner,"client-1","with-service",new BookingRequest(n,FROM,FROM.plusDays(2),1,List.of(s.id()),null));var paid=bookings.confirm(owner,p.id());assertEquals(new BigDecimal("226"),paid.payment().amount());assertEquals(226,account().available());
  }
  @Test void missingOrUnavailablePackageTargetsCannotBecomeBenefits() {
    assertThrows(HotelException.class,()->loyalty.benefit(admin,new Benefit(UUID.randomUUID(),1,"bad",1,Benefit.Kind.PACKAGE,null,UUID.randomUUID())));
    var s=service.createService(admin,"Spa",10);hotel.withSession(admin,()->{hotel.findService(s.id()).setAvailable(false);return null;});assertThrows(HotelException.class,()->loyalty.createPackage(admin,"Spa package","unavailable",List.of(s.id()),List.of()));
  }

  @Test void overflowingAccumulationRollsBackPaymentRatherThanCorruptingBalance() {
    loyalty.configure(admin,new LoyaltyPolicy("OVERFLOW-TEST",1,new BigDecimal("200"),Long.MAX_VALUE,RefundTreatment.RECALCULATE_RETAINED_PAYMENT,SpentTreatment.CARRY_FORWARD_DEBT,RedemptionCancellation.KEEP_SPENT));
    service.recharge(admin,"client-1",1000,"Funds");bookings.confirm(owner,pending().id());assertEquals(Long.MAX_VALUE,account().available());
    var p=pending();var before=account();var card=service.card(owner,"client-1");assertThrows(ArithmeticException.class,()->bookings.confirm(owner,p.id()));assertEquals(before,account());assertEquals(card,service.card(owner,"client-1"));assertEquals(p,bookings.get(owner,p.id()));
  }
  @Test void packageNamesAreNormalizedBeforeUniquenessChecks() {
    var s=service.createService(admin,"Spa",10);var p=loyalty.createPackage(admin," Spa package "," Description ",List.of(s.id()),List.of());assertEquals("Spa package",p.name());assertEquals("Description",p.description());
    assertThrows(HotelException.class,()->loyalty.createPackage(admin," spa PACKAGE ","Other",List.of(s.id()),List.of()));
  }

  @Test void invalidCombinedPackagePriceLeavesNoRegisteredPackage() {
    var a=service.createService(admin,"A",new BigDecimal("1E308"),"COP");var b=service.createService(admin,"B",new BigDecimal("1E308"),"COP");
    assertThrows(HotelException.class,()->loyalty.createPackage(admin,"Overflow","Invalid combined COP",List.of(a.id(),b.id()),List.of()));assertTrue(loyalty.packages(owner).isEmpty());
  }

}
