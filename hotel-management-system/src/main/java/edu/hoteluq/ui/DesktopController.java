package edu.hoteluq.ui;

import edu.hoteluq.enums.Role;
import edu.hoteluq.exceptions.*;
import edu.hoteluq.service.*;
import edu.hoteluq.service.Views.UserView;
import edu.hoteluq.singleton.Hotel;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/** Presentation/session controller; toolkit independent and testable. No commercial arithmetic. */
public final class DesktopController {
  public enum Screen {
    SEARCH(false,"Habitaciones"), BOOKINGS(false,"Reservas"), CARD(false,"Tarjeta y movimientos"), POINTS(false,"Puntos"),
    CLIENTS(true,"Clientes"), ROOMS(true,"Habitaciones · gestión"), TYPES(true,"Tipos"), SERVICES(true,"Servicios y paquetes"),
    PROMOTIONS(true,"Promociones"), PARAMETERS(true,"Parámetros comerciales"), REPORTS(true,"Reportes y estadísticas");
    public final boolean adminOnly; public final String title;
    Screen(boolean adminOnly,String title) {this.adminOnly=adminOnly;this.title=title;}
  }
  public final HotelService hotel;
  public final ReservationService bookings;
  public final LoyaltyService loyalty;
  public final PromotionService promotions;
  public final ReportService reports;
  private volatile String token;
  private volatile UserView user;
  private volatile Screen screen;
  private final AtomicBoolean busy=new AtomicBoolean();
  public DesktopController(Hotel aggregate) {
    hotel=new HotelService(aggregate);bookings=new ReservationService(aggregate);loyalty=new LoyaltyService(aggregate);promotions=new PromotionService(aggregate);reports=new ReportService(aggregate);
  }
  public UserView user() {return user;}
  public boolean authenticated() {return token!=null && user!=null;}
  public String token() {if(!authenticated())throw new AuthenticationException("Invalid or expired session");return token;}
  public Screen screen() {return screen;}
  public boolean admin() {return user!=null && user.role()==Role.ADMINISTRATOR;}
  public void setup(String id,String name,String email,String password) {
    if(hotel.initialized())throw new AuthorizationException();
    hotel.initializeAdministrator(id,name,email,password,Duration.ofMinutes(30));login(email,password);
  }
  public void login(String email,String password) {
    logout(); var session=hotel.login(email,password);
    try {var actual=hotel.me(session.token());token=session.token();user=actual;screen=Screen.SEARCH;}
    catch(RuntimeException ex) {hotel.logout(session.token());throw ex;}
  }
  public void logout() {
    String previous=token; clear();
    if(previous!=null) try {hotel.logout(previous);}catch(AuthenticationException ignored) { /* expired tokens are already unusable */ }
  }
  public void clear() {token=null;user=null;screen=null;}
  public List<Screen> navigation() {return Arrays.stream(Screen.values()).filter(s->!s.adminOnly || admin()).toList();}
  public void navigate(Screen next) {
    UserView actual=hotel.me(token());
    if(next==null || (next.adminOnly && actual.role()!=Role.ADMINISTRATOR))throw new AuthorizationException();
    user=actual;screen=next;
  }
  public <T> T execute(Supplier<T> operation) {
    if(!busy.compareAndSet(false,true))throw new HotelException("An operation is already running");
    try {return operation.get();}
    catch(AuthenticationException ex) {clear();throw ex;}
    finally {busy.set(false);}
  }
}
