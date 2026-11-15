package edu.hoteluq.singleton;

import edu.hoteluq.promotion.*;
import edu.hoteluq.loyalty.*;
import edu.hoteluq.model.*;
import edu.hoteluq.model.BookingRecords.*;
import edu.hoteluq.service.*;
import edu.hoteluq.enums.*;
import edu.hoteluq.exceptions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static edu.hoteluq.promotion.PromotionDefinition.*;

class PromotionIntegrationTest extends Phase2Fixture {
  private PromotionService promotions;
  private ReservationService bookings;
  private LoyaltyService loyalty;
  private int number=100;
  @BeforeEach void setupPromotions() {promotions=new PromotionService(hotel);bookings=new ReservationService(hotel);loyalty=new LoyaltyService(hotel);}
  private Settings settings(String percent,List<PromotionCondition> conditions,Combination combination,PointsAccrual accrual) {
    // Every commercial value in this suite is an explicit TEST fixture.
    return new Settings("Promotion","Fixture conditions",new BigDecimal(percent),true,null,null,conditions,combination,accrual);
  }
  private Settings basic() {return settings("10",List.of(new PromotionCondition.MinimumNights(2)),Combination.EXCLUSIVE,PointsAccrual.EARN_ON_NET_PAYMENT);}
  private BookingView pending(UUID promotion) {return pending(promotion,FROM,2,List.of(),List.of());}
  private BookingView pending(UUID promotion,LocalDate from,int nights,List<UUID> services,List<UUID> packages) {
    int n=++number;room(n);return bookings.create(owner,"client-1",UUID.randomUUID().toString(),new BookingRequest(n,from,from.plusDays(nights),1,services,promotion,packages));
  }
  private void funds() {service.recharge(admin,"client-1",10000,"Fixture funds");}
  private void pointPolicy() {loyalty.configure(admin,new LoyaltyPolicy("TEST-ONLY",1,BigDecimal.ONE,1,LoyaltyPolicy.RefundTreatment.RECALCULATE_RETAINED_PAYMENT,LoyaltyPolicy.SpentTreatment.CARRY_FORWARD_DEBT,LoyaltyPolicy.RedemptionCancellation.RESTORE_ORIGINAL_EXPIRY));}
  private void earn() {pointPolicy();funds();bookings.confirm(owner,pending(null).id());}
  private Benefit benefit(Benefit.Kind kind,UUID target) {
    var b=new Benefit(UUID.randomUUID(),1,"Fixture benefit",50,kind,kind==Benefit.Kind.RESERVATION_DISCOUNT?new BigDecimal("20"):null,target);loyalty.benefit(admin,b);return b;
  }
  private void now(Instant instant) {
    clock.advance(Duration.between(clock.instant(),instant));admin=service.login("admin@example.com",adminPassword).token();owner=service.login("ana@example.com",ownerPassword).token();other=service.login("luis@example.com",ownerPassword).token();
  }
  @Test void validPromotionIsPaidWithVersionedContractAndAdminAudit() {
    var d=promotions.create(admin,basic());var p=pending(d.id());funds();var paid=bookings.confirm(owner,p.id());
    assertEquals(new BigDecimal("180"),paid.payment().amount());assertEquals(paid.payment().amount(),paid.quote().total());assertEquals(d,paid.promotion());assertEquals(d,paid.quote().promotionDefinition());assertEquals(d.id(),paid.quote().promotionId());
    var change=promotions.history(admin).getFirst();assertEquals("admin",change.actorId());assertEquals(d,change.after());assertNull(change.before());assertEquals(clock.instant(),change.at());
  }
  @Test void unauthorizedAdministrationAndHistoryAreRejected() {
    assertThrows(AuthorizationException.class,()->promotions.create(owner,basic()));assertThrows(AuthorizationException.class,()->promotions.history(owner));assertThrows(AuthenticationException.class,()->promotions.list("invalid"));
    var d=promotions.create(admin,basic());assertThrows(AuthorizationException.class,()->promotions.update(other,d.id(),1,basic()));assertThrows(AuthorizationException.class,()->promotions.setActive(owner,d.id(),1,false));assertEquals(List.of(d),promotions.list(owner));
  }
  @Test void duplicateNamesAndInvalidReferencesLeaveCatalogUnchanged() {
    var d=promotions.create(admin,basic());assertThrows(HotelException.class,()->promotions.create(admin,basic()));assertEquals(List.of(d),promotions.list(owner));assertEquals(1,promotions.history(admin).size());
    var invalid=new Settings("Other","Bad reference",BigDecimal.TEN,true,null,null,List.of(new PromotionCondition.Services(Set.of(UUID.randomUUID()))),Combination.EXCLUSIVE,PointsAccrual.EXCLUDED);
    assertThrows(HotelException.class,()->promotions.create(admin,invalid));assertEquals(1,service.promotions(owner).size());assertEquals(List.of(d),promotions.list(owner));
  }
  @Test void inactivePromotionCanBeActivatedAndDeactivatedWithVersionCheck() {
    var d=promotions.create(admin,basic().withActive(false));assertThrows(HotelException.class,()->pending(d.id()));var active=promotions.setActive(admin,d.id(),1,true);assertEquals(2,active.version());var p=pending(d.id());
    promotions.setActive(admin,d.id(),2,false);assertThrows(HotelException.class,()->bookings.confirm(owner,p.id()));assertThrows(HotelException.class,()->promotions.setActive(admin,d.id(),2,true));assertNull(bookings.get(owner,p.id()).payment());
  }
  @ParameterizedTest @ValueSource(booleans={false,true}) void futureAndExpiredPromotionsAreRejected(boolean future) {
    Instant from=future?clock.instant().plusSeconds(1):clock.instant().minusSeconds(10);Instant until=future?clock.instant().plusSeconds(10):clock.instant();
    var s=new Settings("Promotion","Window",BigDecimal.TEN,true,from,until,basic().conditions(),Combination.EXCLUSIVE,PointsAccrual.EXCLUDED);var d=promotions.create(admin,s);assertThrows(HotelException.class,()->pending(d.id()));
  }
  @Test void expirationIsRecheckedBeforeChargeButDoesNotInvalidatePaymentRetries() {
    var s=new Settings("Promotion","Window",BigDecimal.TEN,true,clock.instant(),clock.instant().plusSeconds(10),basic().conditions(),Combination.EXCLUSIVE,PointsAccrual.EXCLUDED);
    var d=promotions.create(admin,s);funds();var first=bookings.confirm(owner,pending(d.id()).id());var second=pending(d.id());now(clock.instant().plusSeconds(10));var card=service.card(owner,"client-1");
    assertThrows(HotelException.class,()->bookings.confirm(owner,second.id()));assertEquals(card,service.card(owner,"client-1"));assertEquals(first,bookings.confirm(owner,first.id()));assertEquals(second,bookings.get(owner,second.id()));
  }
  @Test void roomAndNightAndWholeStayConditionsAreAllRequired() {
    var d=promotions.create(admin,settings("10",List.of(new PromotionCondition.RoomTypes(Set.of(RoomCategory.STANDARD)),new PromotionCondition.MinimumNights(3),new PromotionCondition.StayWindow(FROM,FROM.plusDays(4))),Combination.EXCLUSIVE,PointsAccrual.EXCLUDED));
    assertThrows(HotelException.class,()->pending(d.id(),FROM,2,List.of(),List.of()));assertThrows(HotelException.class,()->pending(d.id(),FROM,5,List.of(),List.of()));assertDoesNotThrow(()->pending(d.id(),FROM,4,List.of(),List.of()));
  }
  @Test void wrongRoomTypeCannotReceiveDiscount() {
    var d=promotions.create(admin,settings("10",List.of(new PromotionCondition.RoomTypes(Set.of(RoomCategory.SUITE))),Combination.EXCLUSIVE,PointsAccrual.EXCLUDED));assertThrows(HotelException.class,()->pending(d.id()));
  }
  @Test void frequentClientCountsOnlyCompletedPaidReservations() {
    funds();var prior=bookings.confirm(owner,pending(null).id());var d=promotions.create(admin,settings("10",List.of(new PromotionCondition.FrequentClient(1)),Combination.EXCLUSIVE,PointsAccrual.EXCLUDED));assertThrows(HotelException.class,()->pending(d.id()));
    now(prior.schedule().checkOut());bookings.checkout(admin,prior.id(),"out");assertDoesNotThrow(()->pending(d.id(),FROM.plusDays(5),2,List.of(),List.of()));
  }
  @Test void cancelledReservationsNeverQualifyAsFrequentCustomerHistory() {
    funds();var prior=bookings.confirm(owner,pending(null).id());bookings.cancelByHotel(admin,prior.id());var d=promotions.create(admin,settings("10",List.of(new PromotionCondition.FrequentClient(1)),Combination.EXCLUSIVE,PointsAccrual.EXCLUDED));assertThrows(HotelException.class,()->pending(d.id()));
  }
  @Test void requiredServiceMustBeSelectedAvailableAndIncludedInDiscountBase() {
    var s=service.createService(admin,"Spa",new BigDecimal("25.5"),"COP");var d=promotions.create(admin,settings("10",List.of(new PromotionCondition.Services(Set.of(s.id()))),Combination.EXCLUSIVE,PointsAccrual.EARN_ON_NET_PAYMENT));assertThrows(HotelException.class,()->pending(d.id()));
    var p=pending(d.id(),FROM,2,List.of(s.id()),List.of());funds();var paid=bookings.confirm(owner,p.id());assertEquals(new BigDecimal("203"),paid.payment().amount());assertEquals(0,new BigDecimal("22.55").compareTo(paid.quote().rawDiscount()));
  }
  @Test void selectedPackagesAreChargedOnceAndPackageConditionRequiresItsIdentity() {
    var a=service.createService(admin,"A",25);var b=service.createService(admin,"B",15);var pack=loyalty.createPackage(admin,"Bundle","Two services",List.of(a.id(),b.id()),List.of());
    var d=promotions.create(admin,settings("10",List.of(new PromotionCondition.Packages(Set.of(pack.id()))),Combination.EXCLUSIVE,PointsAccrual.EXCLUDED));
    assertThrows(HotelException.class,()->pending(d.id(),FROM,2,List.of(a.id(),b.id()),List.of()));var p=pending(d.id(),FROM,2,List.of(),List.of(pack.id()));funds();var paid=bookings.confirm(owner,p.id());assertEquals(new BigDecimal("216"),paid.payment().amount());assertEquals(List.of(pack.id()),paid.packageIds());assertEquals(2,paid.quote().services().size());
  }
  @Test void duplicateServicesAcrossPackagesAreRejectedWithoutReservation() {
    var a=service.createService(admin,"A",25);var pack=loyalty.createPackage(admin,"Bundle","One service",List.of(a.id()),List.of());assertThrows(HotelException.class,()->pending(null,FROM,2,List.of(a.id()),List.of(pack.id())));assertTrue(bookings.list(owner).isEmpty());
  }
  @Test void legacyCatalogWithoutConditionsStillCannotDiscountPayment() {
    var old=service.createPromotion(admin,"Old",10);assertThrows(HotelException.class,()->pending(old.id()));var def=promotions.update(admin,old.id(),0,basic());assertEquals(1,def.version());assertDoesNotThrow(()->pending(old.id()));
  }
  @Test void directMutationsAndLegacyUpdateCannotBypassVersioning() {
    var d=promotions.create(admin,basic());assertThrows(HotelException.class,()->service.updatePromotion(admin,d.id(),"Changed",20));assertThrows(HotelException.class,()->hotel.withSession(admin,()->{hotel.findPromotion(d.id()).setName("Changed");return null;}));assertThrows(HotelException.class,()->service.deletePromotion(admin,d.id()));assertEquals(List.of(d),promotions.list(owner));
  }
  @Test void promotionUpdatesRequirePendingQuoteAcceptanceAndPreservePaidHistory() {
    var d=promotions.create(admin,basic());funds();var paid=bookings.confirm(owner,pending(d.id()).id());var p=pending(d.id());var changed=promotions.update(admin,d.id(),1,settings("20",basic().conditions(),Combination.EXCLUSIVE,PointsAccrual.EXCLUDED));
    assertThrows(HotelException.class,()->bookings.confirm(owner,p.id()));assertEquals(paid,bookings.get(owner,paid.id()));assertEquals(d,paid.promotion());assertEquals(d,paid.history().getFirst().after().promotionDefinition());
    var request=new BookingRequest(p.quote().roomNumber(),FROM,FROM.plusDays(2),1,List.of(),d.id());var modified=bookings.modify(owner,p.id(),p.version(),request);assertEquals(changed,modified.promotion());assertEquals(new BigDecimal("160"),modified.quote().total());assertEquals(d,modified.history().getLast().before().promotionDefinition());assertEquals(changed,modified.history().getLast().after().promotionDefinition());
  }
  @Test void changingDatesSoEligibilityFailsLeavesPendingContractUnchanged() {
    var d=promotions.create(admin,basic());var p=pending(d.id());var request=new BookingRequest(p.quote().roomNumber(),FROM,FROM.plusDays(1),1,List.of(),d.id());assertThrows(HotelException.class,()->bookings.modify(owner,p.id(),p.version(),request));assertEquals(p,bookings.get(owner,p.id()));
  }
  @ParameterizedTest @ValueSource(strings={"100","99.9"}) void zeroAfterRoundingIsRejectedBeforeMovement(String percent) {
    var d=promotions.create(admin,settings(percent,basic().conditions(),Combination.EXCLUSIVE,PointsAccrual.EXCLUDED));funds();var before=service.card(owner,"client-1");assertThrows(HotelException.class,()->pending(d.id()));assertEquals(before,service.card(owner,"client-1"));
  }
  @Test void wrongOwnerCannotApplyOrConfirmAnotherClientsPromotion() {
    var d=promotions.create(admin,basic());var p=pending(d.id());funds();assertThrows(AuthorizationException.class,()->bookings.confirm(other,p.id()));assertThrows(AuthorizationException.class,()->bookings.modify(other,p.id(),p.version(),new BookingRequest(p.quote().roomNumber(),FROM,FROM.plusDays(2),1,List.of(),d.id())));assertNull(bookings.get(owner,p.id()).payment());
  }
  @Test void exclusiveCombinationBlocksBenefitWithoutMoneyOrPointChanges() {
    earn();var b=benefit(Benefit.Kind.RESERVATION_DISCOUNT,null);var d=promotions.create(admin,basic());var p=pending(d.id());var card=service.card(owner,"client-1");var points=loyalty.account(owner,"client-1");assertThrows(HotelException.class,()->loyalty.redeem(owner,p.id(),p.version(),"r",b.id()));assertEquals(points,loyalty.account(owner,"client-1"));assertEquals(card,service.card(owner,"client-1"));assertEquals(p,bookings.get(owner,p.id()));
  }
  @ParameterizedTest @CsvSource({"PROMOTION_THEN_POINTS,160,310","POINTS_THEN_PROMOTION,162,312"})
  void configuredPointCombinationsAreAtomicAndRefundOriginalPayment(Combination order,String cash,long balance) {
    earn();var b=benefit(Benefit.Kind.RESERVATION_DISCOUNT,null);var d=promotions.create(admin,settings("10",basic().conditions(),order,PointsAccrual.EARN_ON_NET_PAYMENT));var p=pending(d.id());var paid=loyalty.redeem(owner,p.id(),p.version(),"r",b.id());assertEquals(new BigDecimal(cash),paid.payment().amount());assertEquals(balance,loyalty.account(owner,"client-1").available());assertEquals(d,paid.promotion());assertEquals(paid,loyalty.redeem(owner,p.id(),p.version(),"r",b.id()));
    var cancelled=bookings.cancelByHotel(admin,p.id());assertEquals(paid.payment().amount(),cancelled.refund().amount());assertEquals(200,loyalty.account(owner,"client-1").available());assertEquals(paid.quote(),cancelled.quote());
  }
  @ParameterizedTest @EnumSource(value=Benefit.Kind.class,names={"SERVICE","PACKAGE"})
  void serviceAndPackagePointBenefitsCombineOnlyAsConfigured(Benefit.Kind kind) {
    earn();var s=service.createService(admin,"Spa",40);UUID target=kind==Benefit.Kind.SERVICE?s.id():loyalty.createPackage(admin,"Bundle","Spa",List.of(s.id()),List.of()).id();var b=benefit(kind,target);
    var d=promotions.create(admin,settings("10",basic().conditions(),Combination.POINTS_THEN_PROMOTION,PointsAccrual.EARN_ON_NET_PAYMENT));var p=pending(d.id());var paid=loyalty.redeem(owner,p.id(),p.version(),"r",b.id());assertEquals(new BigDecimal("180"),paid.payment().amount());assertEquals(1,paid.quote().services().size());assertEquals(d.id(),paid.quote().promotionId());if(kind==Benefit.Kind.PACKAGE)assertEquals(List.of(target),paid.packageIds());
  }
  @Test void explicitExcludedAccrualDoesNotCreatePointsOrDeferredAccrual() {
    pointPolicy();funds();var d=promotions.create(admin,settings("10",basic().conditions(),Combination.EXCLUSIVE,PointsAccrual.EXCLUDED));bookings.confirm(owner,pending(d.id()).id());assertEquals(0,loyalty.account(owner,"client-1").available());assertTrue(loyalty.deferred(owner,"client-1").isEmpty());
  }
  @ParameterizedTest @EnumSource(Hotel.Stage.class)
  void partialCombinedConfirmationFailureRestoresAllSnapshots(Hotel.Stage stage) {
    earn();var b=benefit(Benefit.Kind.RESERVATION_DISCOUNT,null);var d=promotions.create(admin,settings("10",basic().conditions(),Combination.POINTS_THEN_PROMOTION,PointsAccrual.EARN_ON_NET_PAYMENT));var p=pending(d.id());var card=service.card(owner,"client-1");var points=loyalty.account(owner,"client-1");hotel.transactionProbe=s->{if(s==stage)throw new HotelException("Failure");};assertThrows(HotelException.class,()->loyalty.redeem(owner,p.id(),p.version(),"r",b.id()));hotel.transactionProbe=s->{};assertEquals(p,bookings.get(owner,p.id()));assertEquals(card,service.card(owner,"client-1"));assertEquals(points,loyalty.account(owner,"client-1"));
  }
  @ParameterizedTest @EnumSource(Hotel.Stage.class)
  void failedRefundRestoresPromotedContractMoneyAndPoints(Hotel.Stage stage) {
    pointPolicy();funds();var d=promotions.create(admin,basic());var p=bookings.confirm(owner,pending(d.id()).id());var card=service.card(owner,"client-1");var points=loyalty.account(owner,"client-1");hotel.transactionProbe=s->{if(s==stage)throw new HotelException("Failure");};assertThrows(HotelException.class,()->bookings.cancelByHotel(admin,p.id()));hotel.transactionProbe=s->{};assertEquals(p,bookings.get(owner,p.id()));assertEquals(card,service.card(owner,"client-1"));assertEquals(points,loyalty.account(owner,"client-1"));
  }
  @Test void repeatedConcurrentConfirmationsProduceOnePaymentAndOneNetAccrual() throws Exception {
    pointPolicy();funds();var d=promotions.create(admin,basic());var p=pending(d.id());assertEquals(2,race(()->bookings.confirm(owner,p.id()),()->bookings.confirm(owner,p.id())));assertEquals(180,loyalty.account(owner,"client-1").available());assertEquals(1,loyalty.account(owner,"client-1").lots().size());
  }
  @Test void concurrentAdministrativeUpdatesCannotLoseVersions() throws Exception {
    var d=promotions.create(admin,basic());assertEquals(1,race(()->promotions.update(admin,d.id(),1,settings("15",basic().conditions(),Combination.EXCLUSIVE,PointsAccrual.EXCLUDED)),()->promotions.update(admin,d.id(),1,settings("20",basic().conditions(),Combination.EXCLUSIVE,PointsAccrual.EXCLUDED))));assertEquals(2,promotions.list(owner).getFirst().version());assertEquals(2,promotions.history(admin).size());assertEquals(promotions.list(owner).getFirst().settings().percent(),hotel.withSession(admin,()->hotel.findPromotion(d.id()).getDiscountPercentAmount()));
  }
  @Test void serviceReferencedByConditionsCannotBeDeleted() {
    var s=service.createService(admin,"Spa",10);promotions.create(admin,settings("10",List.of(new PromotionCondition.Services(Set.of(s.id()))),Combination.EXCLUSIVE,PointsAccrual.EXCLUDED));assertThrows(HotelException.class,()->service.deleteService(admin,s.id()));
  }
  @ParameterizedTest @CsvSource({"73,180","72,144","24,144","23,0"})
  void officialRefundsUseDiscountedCashWithoutChangingWindows(int hours,String returned) {
    pointPolicy();funds();var d=promotions.create(admin,basic());var paid=bookings.confirm(owner,pending(d.id()).id());now(paid.schedule().checkIn().minus(Duration.ofHours(hours)));var result=bookings.cancelByClient(owner,paid.id());assertEquals(new BigDecimal(returned),result.refund().amount());assertEquals(paid.payment().id(),result.refund().paymentId());assertEquals(180-Integer.parseInt(returned),loyalty.account(owner,"client-1").available());assertEquals(result,bookings.cancelByClient(owner,paid.id()));
  }
  @Test void concurrentDeactivationAndConfirmationHaveAConsistentSerialOutcome() throws Exception {
    pointPolicy();funds();var d=promotions.create(admin,basic());var p=pending(d.id());
    int successes=race(()->bookings.confirm(owner,p.id()),()->promotions.setActive(admin,d.id(),1,false));assertTrue(successes==1 || successes==2);
    var result=bookings.get(owner,p.id());assertFalse(promotions.list(owner).getFirst().settings().active());
    if(result.status()==ReservationStatus.CONFIRMED) {assertEquals(d,result.promotion());assertEquals(new BigDecimal("180"),result.payment().amount());assertEquals(180,loyalty.account(owner,"client-1").available());assertEquals(result,bookings.confirm(owner,p.id()));}
    else {assertEquals(p,result);assertEquals(0,loyalty.account(owner,"client-1").available());assertNull(result.payment());}
  }
  @Test void insufficientCashRestoresCombinedPromotionAndPointsBeforeAnyMovement() {
    earn();var b=benefit(Benefit.Kind.RESERVATION_DISCOUNT,null);var d=promotions.create(admin,settings("10",basic().conditions(),Combination.PROMOTION_THEN_POINTS,PointsAccrual.EARN_ON_NET_PAYMENT));var p=pending(d.id(),FROM,120,List.of(),List.of());var card=service.card(owner,"client-1");var points=loyalty.account(owner,"client-1");
    assertThrows(HotelException.class,()->loyalty.redeem(owner,p.id(),p.version(),"expensive",b.id()));assertEquals(card,service.card(owner,"client-1"));assertEquals(points,loyalty.account(owner,"client-1"));assertEquals(p,bookings.get(owner,p.id()));
  }
  @Test void failedAdministrativeUpdatesDoNotChangeCatalogVersionsOrAudit() {
    var d=promotions.create(admin,basic());var otherSettings=new Settings("Other","Other",BigDecimal.TEN,true,null,null,basic().conditions(),Combination.EXCLUSIVE,PointsAccrual.EXCLUDED);promotions.create(admin,otherSettings);
    var before=promotions.list(owner);var history=promotions.history(admin);assertThrows(HotelException.class,()->promotions.update(admin,d.id(),1,otherSettings));
    var invalid=settings("15",List.of(new PromotionCondition.Packages(Set.of(UUID.randomUUID()))),Combination.EXCLUSIVE,PointsAccrual.EXCLUDED);
    assertThrows(HotelException.class,()->promotions.update(admin,d.id(),1,invalid));assertThrows(HotelException.class,()->promotions.update(admin,d.id(),0,basic()));assertEquals(before,promotions.list(owner));assertEquals(history,promotions.history(admin));assertEquals(d.settings().percent(),hotel.withSession(admin,()->hotel.findPromotion(d.id()).getDiscountPercentAmount()));
  }

}
