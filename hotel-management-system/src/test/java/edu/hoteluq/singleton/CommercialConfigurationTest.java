package edu.hoteluq.singleton;

import edu.hoteluq.enums.*;
import edu.hoteluq.exceptions.*;
import edu.hoteluq.service.*;
import edu.hoteluq.model.BookingRecords.BookingView;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class CommercialConfigurationTest {
  Hotel hotel; HotelService users; ReservationService bookings;
  Phase2Fixture.MutableClock clock; String admin, client;
  @BeforeEach void setup() {
    clock=new Phase2Fixture.MutableClock(); hotel=new Hotel(clock,Duration.ofDays(365));
    users=new HotelService(hotel); bookings=new ReservationService(hotel);
    users.initializeAdministrator("a","Admin","a@example.com","admin-password",Duration.ofDays(365));
    admin=users.login("a@example.com","admin-password").token();
    users.register("c","Client","c@example.com","client-password");client=users.login("c@example.com","client-password").token();
    users.createRoomType(admin,RoomCategory.STANDARD,"Standard",100,2,"Description");
    users.createRoom(admin,RoomCategory.STANDARD,101,1);
  }
  BookingView paid() {
    users.recharge(admin,"c",200,"Funds");
    var pending=bookings.create(client,"c","one",new BookingRequest(101,LocalDate.of(2027,1,1),LocalDate.of(2027,1,3),1,List.of(),null));
    return bookings.confirm(client,pending.id());
  }
  @Test void officialCancellationScheduleIsActiveWithoutManualConfiguration() {
    bookings.configure(admin,TestCommercialPolicies.rules());var paid=paid();var before=users.card(client,"c");
    assertTrue(bookings.policyHistory(admin).isEmpty());
    var cancelled=bookings.cancelByClient(client,paid.id());
    assertEquals(edu.hoteluq.strategy.CancellationSchedule.OFFICIAL_ID,cancelled.refund().policyId());
    assertEquals(0,cancelled.refund().amount().compareTo(paid.payment().amount()));
    assertEquals(before.balance()+paid.payment().amount().doubleValue(),users.card(client,"c").balance());
    assertEquals(cancelled,bookings.cancelByHotel(admin,paid.id()));
  }
  @Test void missingCheckoutHourNeverFallsBackToSystemOrOldHardcodedTime() {
    bookings.configure(admin,new BookingRules(LocalTime.of(15,0),null,null));var paid=paid();
    assertNull(paid.schedule().checkOut());
    clock.advance(Duration.ofDays(100));
    assertEquals(ReservationStatus.CONFIRMED,bookings.get(client,paid.id()).status());
    assertThrows(InvalidReservationException.class,() -> bookings.checkout(admin,paid.id(),"one"));
    assertNull(bookings.get(client,paid.id()).checkout());
  }
  @Test void onlyAdminCanActivatePoliciesAndAbsentPolicyIsRejected() {
    assertThrows(AuthorizationException.class,() -> bookings.configureCancellation(client,TestCommercialPolicies.cancellation()));
    assertThrows(HotelException.class,() -> bookings.configureCancellation(admin,null));
    assertTrue(bookings.policyHistory(admin).isEmpty());
  }
  @Test void absentCheckoutWindowCanBeCompletedWithoutRewritingScheduledInstants() {
    bookings.configure(admin,new BookingRules(LocalTime.of(15,0),LocalTime.NOON,null));var paid=paid();
    clock.advance(Duration.between(clock.instant(),paid.schedule().checkOut()));
    assertThrows(InvalidReservationException.class,() -> bookings.checkout(admin,paid.id(),"one"));
    assertThrows(AuthorizationException.class,() -> bookings.configureCheckoutWindow(client,new BookingRules.CheckoutWindow(Duration.ZERO,Duration.ofHours(1))));
    var window=new BookingRules.CheckoutWindow(Duration.ZERO,Duration.ofHours(1));
    bookings.configureCheckoutWindow(admin,window);
    var checked=bookings.checkout(admin,paid.id(),"one");
    assertEquals(paid.schedule(),checked.schedule());assertEquals(window,checked.checkout().authorizedWindow());
    assertThrows(HotelException.class,() -> bookings.configureCheckoutWindow(admin,window));
  }

  @Test void commercialHourChangeCannotSilentlyMoveContractualInstants() {
    bookings.configure(admin,TestCommercialPolicies.rules());var paid=paid();
    assertThrows(HotelException.class,() -> bookings.configure(admin,new BookingRules(LocalTime.of(16,0),LocalTime.of(13,0),null)));
    assertEquals(paid,bookings.get(client,paid.id()));
  }
}
