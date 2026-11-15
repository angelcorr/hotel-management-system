package edu.hoteluq.singleton;

import edu.hoteluq.demo.DemoFixtures;
import edu.hoteluq.service.*;
import edu.hoteluq.loyalty.*;
import edu.hoteluq.promotion.*;
import edu.hoteluq.enums.*;
import edu.hoteluq.config.HotelTime;
import edu.hoteluq.exceptions.HotelException;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

/** Explicit, isolated academic replay. Never seeds/reset the normal Singleton or writes credentials. */
public final class DemoDataFactory {
  private DemoDataFactory(){}
  public record Credentials(String adminEmail,String adminPassword,String clientPassword){
    public Credentials{
      if(adminPassword==null || clientPassword==null || adminPassword.length()<12 || clientPassword.length()<12 || adminPassword.equals(clientPassword))throw new HotelException("Demo: use dos contraseñas distintas de al menos 12 caracteres");
    }
    @Override public String toString(){return "Demo credentials [REDACTED]";}
  }
  public record Context(Hotel hotel,LocalDate historyFrom,LocalDate historyThrough,LocalDate nextStay,String clientEmail){}
  private static final class ReplayClock extends Clock {
    private final Clock live;private Instant at;
    ReplayClock(Clock live,Instant start){this.live=live;at=start;}
    void at(Instant value){at=value;}void live(){at=null;}
    public Instant instant(){return at==null?live.instant():at;}
    public ZoneId getZone(){return HotelTime.ZONE;}
    public Clock withZone(ZoneId zone){return HotelTime.inHotelZone(this);}
  }
  public static Context create(Credentials input){return create(input,HotelTime.systemClock());}
  static Context create(Credentials input,Clock source){
    Objects.requireNonNull(input);Instant cut=source.instant();ReplayClock clock=new ReplayClock(source,cut.minus(Duration.ofDays(14)));
    Hotel hotel=new Hotel(clock,Duration.ofMinutes(30));var users=new HotelService(hotel);var bookings=new ReservationService(hotel);var loyalty=new LoyaltyService(hotel);var promotions=new PromotionService(hotel);
    users.initializeAdministrator(DemoFixtures.ADMIN_ID,"Operador DEMOSTRACIÓN",input.adminEmail(),input.adminPassword(),Duration.ofMinutes(30));
    users.register(DemoFixtures.CLIENT_ID,"Cliente DEMOSTRACIÓN",DemoFixtures.CLIENT_EMAIL,input.clientPassword());
    String admin=users.login(input.adminEmail(),input.adminPassword()).token(),client=users.login(DemoFixtures.CLIENT_EMAIL,input.clientPassword()).token();
    bookings.configure(admin,DemoFixtures.rules());loyalty.configure(admin,DemoFixtures.points());
    for(var category:RoomCategory.values())users.createRoomType(admin,category,"Demo "+category,DemoFixtures.nightly(),"COP",2,"Catálogo académico explícito");
    users.createRoom(admin,RoomCategory.STANDARD,101,1);users.createRoom(admin,RoomCategory.FAMILY,102,1);users.createRoom(admin,RoomCategory.SUITE,103,2);users.createRoom(admin,RoomCategory.STANDARD,104,2);users.createRoom(admin,RoomCategory.STANDARD,105,2);
    var breakfast=users.createService(admin,"Desayuno DEMO",DemoFixtures.servicePrice(),"COP");var transport=users.createService(admin,"Traslado DEMO",DemoFixtures.servicePrice(),"COP");
    var pack=loyalty.createPackage(admin,"Paquete DEMO","Dos servicios, no regla comercial",List.of(breakfast.id(),transport.id()),List.of());
    loyalty.benefit(admin,new Benefit(UUID.randomUUID(),1,"Descuento DEMO (fixture)",50,Benefit.Kind.RESERVATION_DISCOUNT,new BigDecimal("20000"),null));
    loyalty.benefit(admin,new Benefit(UUID.randomUUID(),1,"Desayuno DEMO (fixture)",50,Benefit.Kind.SERVICE,null,breakfast.id()));
    loyalty.benefit(admin,new Benefit(UUID.randomUUID(),1,"Paquete DEMO (fixture)",100,Benefit.Kind.PACKAGE,null,pack.id()));
    users.recharge(admin,DemoFixtures.CLIENT_ID,DemoFixtures.funds(),"COP","Recarga de escenario académico");
    LocalDate start=clock.instant().atZone(HotelTime.ZONE).toLocalDate(),today=cut.atZone(HotelTime.ZONE).toLocalDate();
    var completed=bookings.create(client,DemoFixtures.CLIENT_ID,"demo-completed",new BookingRequest(101,start.plusDays(1),start.plusDays(3),1,List.of(breakfast.id()),null));bookings.confirm(client,completed.id());
    clock.at(completed.schedule().checkOut());admin=users.login(input.adminEmail(),input.adminPassword()).token();client=users.login(DemoFixtures.CLIENT_EMAIL,input.clientPassword()).token();bookings.checkout(admin,completed.id(),"demo-checkout");
    var promo=promotions.create(admin,new PromotionDefinition.Settings("Promoción DEMO","Fixture: fechas, habitación, noches y desayuno; no aprobada para producción",new BigDecimal("10"),true,null,null,List.of(new PromotionCondition.StayWindow(today.plusDays(1),today.plusDays(90)),new PromotionCondition.RoomTypes(Set.of(RoomCategory.STANDARD)),new PromotionCondition.MinimumNights(2),new PromotionCondition.FrequentClient(1),new PromotionCondition.Services(Set.of(breakfast.id()))),PromotionDefinition.Combination.POINTS_THEN_PROMOTION,PromotionDefinition.PointsAccrual.EARN_ON_NET_PAYMENT));
    var confirmed=bookings.create(client,DemoFixtures.CLIENT_ID,"demo-confirmed",new BookingRequest(101,today.plusDays(3),today.plusDays(5),1,List.of(breakfast.id()),promo.id()));bookings.confirm(client,confirmed.id());
    bookings.create(client,DemoFixtures.CLIENT_ID,"demo-pending",new BookingRequest(102,today.plusDays(6),today.plusDays(8),1,List.of(),null));
    var cancelled=bookings.create(client,DemoFixtures.CLIENT_ID,"demo-client-cancellation",new BookingRequest(103,today.plusDays(9),today.plusDays(11),1,List.of(),null));bookings.confirm(client,cancelled.id());bookings.cancelByClient(client,cancelled.id());
    var hotelCancelled=bookings.create(client,DemoFixtures.CLIENT_ID,"demo-hotel-cancellation",new BookingRequest(104,today.plusDays(12),today.plusDays(14),1,List.of(),null));bookings.cancelByHotel(admin,hotelCancelled.id());
    users.logout(admin);users.logout(client);clock.live();
    return new Context(hotel,start,today,today.plusDays(40),DemoFixtures.CLIENT_EMAIL);
  }
}
