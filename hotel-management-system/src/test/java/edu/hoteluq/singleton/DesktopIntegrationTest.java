package edu.hoteluq.singleton;

import edu.hoteluq.ui.*;
import edu.hoteluq.service.*;
import edu.hoteluq.exceptions.*;
import edu.hoteluq.enums.*;
import edu.hoteluq.loyalty.*;
import edu.hoteluq.promotion.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class DesktopIntegrationTest extends Phase2Fixture {
  private DesktopController desktop;
  private ReservationService bookings;
  @BeforeEach void desktopSetup(){desktop=new DesktopController(hotel);bookings=new ReservationService(hotel);room(101);}
  private void login(){desktop.login("ana@example.com",ownerPassword);}
  private BookingRequest request(){return new BookingRequest(101,FROM,FROM.plusDays(2),1,List.of(),null);}
  @Test void loginLoadsActualRoleAndNavigation(){login();assertEquals("client-1",desktop.user().id());assertFalse(desktop.admin());assertFalse(desktop.navigation().contains(DesktopController.Screen.CLIENTS));assertTrue(desktop.navigation().contains(DesktopController.Screen.BOOKINGS));}
  @Test void administratorGetsAdministrativeNavigation(){desktop.login("admin@example.com",adminPassword);assertTrue(desktop.admin());assertTrue(desktop.navigation().contains(DesktopController.Screen.PARAMETERS));desktop.navigate(DesktopController.Screen.PROMOTIONS);assertEquals(DesktopController.Screen.PROMOTIONS,desktop.screen());}
  @ParameterizedTest @EnumSource(value=DesktopController.Screen.class,names={"CLIENTS","ROOMS","TYPES","SERVICES","PROMOTIONS","PARAMETERS"})
  void customerCannotNavigateToAdminScreenOrBypassBackend(DesktopController.Screen screen){login();assertThrows(AuthorizationException.class,()->desktop.navigate(screen));assertThrows(AuthorizationException.class,()->desktop.hotel.clients(desktop.token()));}
  @Test void logoutRevokesTokenAndClearsNavigation(){login();String token=desktop.token();desktop.logout();assertFalse(desktop.authenticated());assertNull(desktop.screen());assertThrows(AuthenticationException.class,()->service.me(token));assertThrows(AuthenticationException.class,desktop::token);}
  @Test void expiredSessionClearsPresentationState(){login();clock.advance(Duration.ofMinutes(5));assertThrows(AuthenticationException.class,()->desktop.execute(()->{desktop.navigate(DesktopController.Screen.BOOKINGS);return true;}));assertFalse(desktop.authenticated());}
  @Test void failedLoginDoesNotLeavePreviousSession(){login();String previous=desktop.token();assertThrows(AuthenticationException.class,()->desktop.login("ana@example.com","wrong"));assertFalse(desktop.authenticated());assertThrows(AuthenticationException.class,()->service.me(previous));}
  @Test void failedCommandReleasesSubmissionGate(){assertThrows(HotelException.class,()->desktop.execute(()->{throw new HotelException("failure");}));assertEquals("ok",desktop.execute(()->"ok"));}
  @Test void simultaneousSubmissionDoesNotExecuteTwice() throws Exception {
    CountDownLatch entered=new CountDownLatch(1),finish=new CountDownLatch(1);
    try(var executor=Executors.newSingleThreadExecutor()){
      var first=executor.submit(()->desktop.execute(()->{entered.countDown();try{if(!finish.await(5,TimeUnit.SECONDS))throw new IllegalStateException();}catch(InterruptedException ex){throw new IllegalStateException(ex);}return 1;}));
      assertTrue(entered.await(5,TimeUnit.SECONDS));try{assertThrows(HotelException.class,()->desktop.execute(()->2));}finally{finish.countDown();}assertEquals(1,first.get(5,TimeUnit.SECONDS));
    }
  }
  @Test void previewDoesNotBlockRoomOrCreateMovements(){var quote=bookings.preview(owner,"client-1",request());assertEquals(new BigDecimal("200"),quote.total());assertTrue(bookings.list(owner).isEmpty());assertTrue(service.card(owner,"client-1").movements().isEmpty());assertTrue(service.availableRooms(owner,FROM,FROM.plusDays(2)).stream().anyMatch(r->r.number()==101));}
  @Test void previewCannotReadOrBookAnotherClient(){assertThrows(AuthorizationException.class,()->bookings.preview(other,"client-1",request()));}
  @Test void pendingModificationPreviewExcludesOwnAvailabilityAndDoesNotMutate(){var r=bookings.create(owner,"client-1","draft",request());var quote=bookings.previewModification(owner,r.id(),r.version(),request());assertEquals(r.quote(),quote);assertEquals(r,bookings.get(owner,r.id()));}
  @Test void staleVersionCannotCharge(){var r=bookings.create(owner,"client-1","draft",request());service.recharge(admin,"client-1",1000,"Funds");var modified=bookings.modify(owner,r.id(),r.version(),new BookingRequest(101,FROM,FROM.plusDays(3),1,List.of(),null));assertThrows(InvalidReservationException.class,()->bookings.confirm(owner,r.id(),r.version()));assertEquals(new BigDecimal("1000"),service.card(owner,"client-1").exactBalance());assertEquals(modified,bookings.get(owner,r.id()));}
  @Test void confirmIsIdempotentWithExpectedVersion(){var r=bookings.create(owner,"client-1","draft",request());service.recharge(admin,"client-1",1000,"Funds");var paid=bookings.confirm(owner,r.id(),r.version());assertEquals(paid,bookings.confirm(owner,r.id(),r.version()));assertEquals(new BigDecimal("800"),service.card(owner,"client-1").exactBalance());}
  @Test void cancellationPreviewIsPureAndQuotedCancellationIsIdempotent(){var r=bookings.create(owner,"client-1","draft",request());service.recharge(admin,"client-1",1000,"Funds");var paid=bookings.confirm(owner,r.id());var before=service.card(owner,"client-1");var quote=bookings.cancellationQuote(owner,r.id(),false);assertEquals(new BigDecimal("200"),quote.amount());assertEquals(paid,bookings.get(owner,r.id()));assertEquals(before,service.card(owner,"client-1"));var cancelled=bookings.cancel(owner,quote);assertEquals(cancelled,bookings.cancel(owner,quote));assertEquals(new BigDecimal("1000"),service.card(owner,"client-1").exactBalance());}
  @Test void refundWindowChangeRequiresRenewedExplicitAcceptance(){var r=bookings.create(owner,"client-1","draft",request());service.recharge(admin,"client-1",1000,"Funds");var paid=bookings.confirm(owner,r.id());clock.advance(Duration.between(clock.instant(),paid.schedule().checkIn().minus(Duration.ofHours(72)).minusNanos(1)));owner=service.login("ana@example.com",ownerPassword).token();var quote=bookings.cancellationQuote(owner,r.id(),false);clock.advance(Duration.ofNanos(1));assertThrows(InvalidReservationException.class,()->bookings.cancel(owner,quote));assertEquals(paid,bookings.get(owner,r.id()));assertEquals(new BigDecimal("160"),bookings.cancellationQuote(owner,r.id(),false).amount());}
  @Test void customerCannotAskForHotelRefund(){var r=bookings.create(owner,"client-1","draft",request());assertThrows(AuthorizationException.class,()->bookings.cancellationQuote(owner,r.id(),true));}
  @Test void unconfiguredPointsHaveClearMessageAndNoSideEffects(){var loyalty=new LoyaltyService(hotel);var r=bookings.create(owner,"client-1","draft",request());var ex=assertThrows(HotelException.class,()->loyalty.preview(owner,r.id(),r.version(),UUID.randomUUID()));assertTrue(UiText.error(ex).contains("política comercial"));assertEquals(r,bookings.get(owner,r.id()));}
  @Test void pointQuoteMatchesPaymentAndDoesNotConsumePoints(){var loyalty=new LoyaltyService(hotel);loyalty.configure(admin,new LoyaltyPolicy("TEST",1,BigDecimal.ONE,1,LoyaltyPolicy.RefundTreatment.RECALCULATE_RETAINED_PAYMENT,LoyaltyPolicy.SpentTreatment.CARRY_FORWARD_DEBT,LoyaltyPolicy.RedemptionCancellation.RESTORE_ORIGINAL_EXPIRY));service.recharge(admin,"client-1",1000,"Funds");var first=bookings.create(owner,"client-1","earn",request());bookings.confirm(owner,first.id());room(102);var r=bookings.create(owner,"client-1","next",new BookingRequest(102,FROM,FROM.plusDays(2),1,List.of(),null));var b=new Benefit(UUID.randomUUID(),1,"Test",50,Benefit.Kind.RESERVATION_DISCOUNT,new BigDecimal("20"),null);loyalty.benefit(admin,b);var before=loyalty.account(owner,"client-1");var quote=loyalty.preview(owner,r.id(),r.version(),b.id());assertEquals(before,loyalty.account(owner,"client-1"));assertEquals(r,bookings.get(owner,r.id()));assertEquals(quote,loyalty.redeem(owner,r.id(),r.version(),"request",b.id()).quote());}
  @Test void eligiblePromotionsPreviewUsesTheServiceRules(){var promotions=new PromotionService(hotel);var d=promotions.create(admin,new PromotionDefinition.Settings("Test","Fixture",BigDecimal.TEN,true,null,null,List.of(new PromotionCondition.MinimumNights(2)),PromotionDefinition.Combination.EXCLUSIVE,PromotionDefinition.PointsAccrual.EXCLUDED));var offers=bookings.offers(owner,"client-1",request());assertEquals(1,offers.size());assertTrue(offers.getFirst().eligible());assertEquals(d,offers.getFirst().promotion());assertTrue(bookings.list(owner).isEmpty());}
  @Test void emptyPromotionCatalogIsSupported(){assertTrue(bookings.offers(owner,"client-1",request()).isEmpty());assertTrue(bookings.list(owner).isEmpty());}
  @Test void exactCatalogAmountsSurviveDesktopEditing(){var s=service.createService(admin,"Exact",new BigDecimal("0.123456789123456789"),"COP");assertEquals(new BigDecimal("0.123456789123456789"),s.priceAmount());var changed=service.updateService(admin,s.id(),"Exact name",s.priceAmount(),"COP");assertEquals(s.priceAmount(),changed.priceAmount());}
}
