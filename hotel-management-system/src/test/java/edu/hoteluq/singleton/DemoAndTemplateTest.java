package edu.hoteluq.singleton;

import edu.hoteluq.demo.DemoFixtures;
import edu.hoteluq.factory.*;
import edu.hoteluq.model.*;
import edu.hoteluq.enums.*;
import edu.hoteluq.service.*;
import edu.hoteluq.report.*;
import edu.hoteluq.exceptions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DemoAndTemplateTest extends Phase2Fixture {
  private DemoDataFactory.Credentials credentials(){return new DemoDataFactory.Credentials("operador@hoteluq.invalid",UUID.randomUUID().toString(),UUID.randomUUID().toString());}
  @Test void demoIsIsolatedAndResetCreatesAnotherHotelWithoutChangingNormalData(){boolean initialized=Hotel.getInstance().isInitialized();var input=credentials();var a=DemoDataFactory.create(input,Clock.fixed(clock.instant(),ZoneOffset.UTC));var b=DemoDataFactory.create(input,Clock.fixed(clock.instant(),ZoneOffset.UTC));assertNotSame(a.hotel(),b.hotel());assertNotSame(a.hotel(),Hotel.getInstance());assertEquals(initialized,Hotel.getInstance().isInitialized());assertTrue(new ReservationService(hotel).list(owner).isEmpty());}
  @Test void explicitDemoContainsRealStatesPaymentsPointsPromotionsAndReports(){var input=credentials();var demo=DemoDataFactory.create(input,Clock.fixed(clock.instant(),ZoneOffset.UTC));var users=new HotelService(demo.hotel());var token=users.login(input.adminEmail(),input.adminPassword()).token();var rows=new ReservationService(demo.hotel()).list(token);assertTrue(rows.stream().anyMatch(r->r.status()==ReservationStatus.COMPLETED));assertTrue(rows.stream().anyMatch(r->r.status()==ReservationStatus.CONFIRMED));assertTrue(rows.stream().anyMatch(r->r.status()==ReservationStatus.PENDING));assertTrue(rows.stream().filter(r->r.status()==ReservationStatus.CANCELLED).count()>=2);assertFalse(new PromotionService(demo.hotel()).list(token).isEmpty());assertTrue(new LoyaltyService(demo.hotel()).account(token,DemoFixtures.CLIENT_ID).available()>0);assertTrue(new ReportService(demo.hotel()).generate(token,new ReportQuery(ReportQuery.Kind.REVENUE,demo.historyFrom(),demo.historyThrough())).total("payments").signum()>0);}
  @Test void demoSecretsAreNeverDefaultsOrPrinted(){assertThrows(HotelException.class,()->new DemoDataFactory.Credentials("x@y.test","short","short"));String password=UUID.randomUUID().toString();assertThrows(HotelException.class,()->new DemoDataFactory.Credentials("x@y.test",password,password));var value=credentials();assertFalse(value.toString().contains(value.adminPassword()));assertFalse(value.toString().contains(value.clientPassword()));}
  @ParameterizedTest @EnumSource(RoomCategory.class)
  void concreteCreatorsUseTemplateResolutionHookThenRegistration(RoomCategory category){
    hotel.withSession(admin,()->{if(category!=RoomCategory.STANDARD)hotel.addRoomType(new RoomType(category,100,"Type "+category,2,"Fixture"));return null;});
    var creator=new RoomCreatorRegistry().get(category);var room=hotel.withSession(admin,()->creator.create(hotel,200+category.ordinal(),1));assertEquals(category,hotel.withSession(admin,()->room.getType().getCategory()));assertSame(room,hotel.withSession(admin,()->hotel.findRoom(room.getNumber())));
  }
  @Test void templateCallsHookWithRegisteredTypeBeforeAddingProduct(){
    List<String> order=new ArrayList<>();RoomCreator creator=new RoomCreator(){protected RoomCategory category(){order.add("resolve category");return RoomCategory.STANDARD;}protected Room createRoom(int number,int floor,RoomType type){order.add("factory hook");assertSame(hotel.getRoomType(RoomCategory.STANDARD),type);assertNull(hotel.findRoom(number));return new StandardRoom(number,floor,type);}};
    hotel.withSession(admin,()->{var product=creator.create(hotel,301,1);order.add("returned after registration");assertSame(product,hotel.findRoom(301));return null;});assertEquals(List.of("resolve category","factory hook","returned after registration"),order);
  }
  @Test void templateDoesNotRegisterInvalidOrUnauthorizedProduct(){assertThrows(AuthorizationException.class,()->hotel.withSession(owner,()->new StandardRoomCreator().create(hotel,302,1)));assertNull(hotel.withSession(admin,()->hotel.findRoom(302)));assertThrows(HotelException.class,()->hotel.withSession(admin,()->new StandardRoomCreator().create(hotel,-1,1)));}
}
