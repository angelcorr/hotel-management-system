package edu.hoteluq.singleton;

import edu.hoteluq.enums.*;
import edu.hoteluq.exceptions.*;
import edu.hoteluq.model.*;
import edu.hoteluq.service.Views;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class UserAuthorizationTest extends Phase2Fixture {
  enum AdminOperation {
    CLIENT_CREATE, CLIENT_LIST, CLIENT_DELETE, PASSWORD_RESET, TYPE_CREATE, TYPE_UPDATE, TYPE_DELETE,
    ROOM_CREATE, ROOM_UPDATE, ROOM_DELETE, SERVICE_CREATE, SERVICE_UPDATE, SERVICE_DELETE,
    PROMOTION_CREATE, PROMOTION_UPDATE, PROMOTION_DELETE, RECHARGE, CARD_STATE, TYPE_STATE, ROOM_STATE, SERVICE_STATE
  }

  @ParameterizedTest @EnumSource(AdminOperation.class)
  void everyAdministrativeOperationRejectsClientRole(AdminOperation operation) {
    room(101);
    UUID serviceId = service.createService(admin, "Spa", 10).id();
    UUID promotionId = service.createPromotion(admin, "Promo", 10).id();
    Runnable call = switch (operation) {
      case CLIENT_CREATE -> () -> service.createClient(owner, "new", "New", "new@example.com", "secret");
      case CLIENT_LIST -> () -> service.clients(owner);
      case CLIENT_DELETE -> () -> service.deleteClient(owner, "client-2");
      case PASSWORD_RESET -> () -> service.resetClientPassword(owner, "client-2", "secret");
      case TYPE_CREATE -> () -> service.createRoomType(owner, RoomCategory.FAMILY, "Family", 100, 2, "Description");
      case TYPE_UPDATE -> () -> service.updateRoomType(owner, RoomCategory.STANDARD, "Changed", 200, 4, "Changed");
      case TYPE_DELETE -> () -> service.deleteRoomType(owner, RoomCategory.STANDARD);
      case ROOM_CREATE -> () -> service.createRoom(owner, RoomCategory.STANDARD, 102, 1);
      case ROOM_UPDATE -> () -> service.updateRoom(owner, 101, 3, false);
      case ROOM_DELETE -> () -> service.deleteRoom(owner, 101);
      case SERVICE_CREATE -> () -> service.createService(owner, "Breakfast", 20);
      case SERVICE_UPDATE -> () -> service.updateService(owner, serviceId, "Changed", 20);
      case SERVICE_DELETE -> () -> service.deleteService(owner, serviceId);
      case PROMOTION_CREATE -> () -> service.createPromotion(owner, "Other", 20);
      case PROMOTION_UPDATE -> () -> service.updatePromotion(owner, promotionId, "Changed", 20);
      case PROMOTION_DELETE -> () -> service.deletePromotion(owner, promotionId);
      case RECHARGE -> () -> service.recharge(owner, "client-1", 100, "Unauthorized");
      case TYPE_STATE -> () -> service.setRoomTypeActive(owner, RoomCategory.STANDARD, false);
      case ROOM_STATE -> () -> service.setRoomActive(owner, 101, false);
      case SERVICE_STATE -> () -> service.setServiceAvailable(owner, serviceId, false);
      case CARD_STATE -> () -> service.setCardActive(owner, "client-1", false);
    };
    var before = List.of(service.clients(admin), service.roomTypes(admin), service.rooms(admin),
        service.services(admin), service.promotions(admin), service.card(admin, "client-1"));
    assertThrows(AuthorizationException.class, call::run);
    assertEquals(before, List.of(service.clients(admin), service.roomTypes(admin), service.rooms(admin),
        service.services(admin), service.promotions(admin), service.card(admin, "client-1")));
  }

  @Test void clientCanReadAndEditOnlyItsOwnProfileCardAndHistory() {
    assertEquals("client-1", service.getUser(owner, "client-1").id());
    assertEquals(0, service.card(owner, "client-1").balance());
    assertTrue(service.history(owner, "client-1").isEmpty());
    var updated = service.updateUser(owner, "client-1", "Ana Maria", " ANA.MARIA@example.com ");
    assertEquals("Ana Maria", updated.name());
    assertEquals(Role.CLIENT, updated.role());
    assertThrows(AuthorizationException.class, () -> service.getUser(owner, "client-2"));
    assertThrows(AuthorizationException.class, () -> service.updateUser(owner, "client-2", "Changed", "changed@example.com"));
    assertThrows(AuthorizationException.class, () -> service.card(owner, "client-2"));
    assertThrows(AuthorizationException.class, () -> service.history(owner, "client-2"));
    assertThrows(AuthorizationException.class, () -> service.updateUser(owner, "admin", "Changed", "changed@example.com"));
    assertEquals("Luis", service.me(other).name());
  }

  @Test void administrativeClientCrudIsRealAndReturnsNoCredentials() {
    Views.UserView created = service.createClient(admin, "new", "New", "new@example.com", "test-only password");
    assertEquals(Role.CLIENT, created.role());
    assertEquals(3, service.clients(admin).size());
    String token = service.login("new@example.com", "test-only password").token();
    service.updateUser(admin, "new", "Edited", "edited@example.com");
    assertEquals("Edited", service.me(token).name());
    assertThrows(AuthenticationException.class, () -> service.login("new@example.com", "test-only password"));
    assertNotNull(service.login("edited@example.com", "test-only password"));
    service.deleteClient(admin, "new");
    assertThrows(AuthenticationException.class, () -> service.me(token));
    assertThrows(HotelException.class, () -> service.getUser(admin, "new"));
    assertFalse(created.toString().contains("test-only password"));
    assertTrue(Arrays.stream(Views.UserView.class.getRecordComponents()).noneMatch(component ->
        component.getName().toLowerCase().matches(".*(password|hash|salt|token).*")));
  }

  @Test void duplicateIdsAndNormalizedEmailsAreRejectedOnRegistrationAndAdministrativeCreate() {
    assertThrows(HotelException.class, () -> service.register("another", "Other", " ANA@EXAMPLE.COM ", "secret"));
    assertThrows(HotelException.class, () -> service.register("client-1", "Other", "unique@example.com", "secret"));
    assertThrows(HotelException.class, () -> service.createClient(admin, "another", "Other", " ANA@EXAMPLE.COM ", "secret"));
    assertEquals(2, service.clients(admin).size());
  }

  @Test void duplicateEmailUpdateIsAtomicAndAlsoCheckedByDirectSetters() {
    assertThrows(HotelException.class, () -> service.updateUser(owner, "client-1", "Changed", " LUIS@EXAMPLE.COM "));
    assertEquals("Ana", service.me(owner).name());
    assertThrows(HotelException.class, () -> service.updateUser(admin, "client-1", "Changed", "luis@example.com"));
    Client client = rawClient("client-1");
    assertThrows(HotelException.class, () -> hotel.withSession(owner, () -> { client.setEmail(" LUIS@EXAMPLE.COM "); return null; }));
    assertEquals("ana@example.com", service.me(owner).email());
    service.updateUser(owner, "client-1", "Ana", " ANA@EXAMPLE.COM ");
    assertEquals("ANA@EXAMPLE.COM", service.me(owner).email());
  }

  @Test void malformedUpdatesDoNotPartiallyChangeUser() {
    assertThrows(HotelException.class, () -> service.updateUser(owner, "client-1", "Changed", "bad-email"));
    assertThrows(HotelException.class, () -> service.updateUser(owner, "client-1", " ", "new@example.com"));
    assertEquals("Ana", service.me(owner).name());
    assertEquals("ana@example.com", service.me(owner).email());
  }

  @Test void rolesCannotBeElevatedThroughRegistrationDetachedObjectsOrBootstrap() {
    assertEquals(Role.CLIENT, service.register("new", "Administrator", "new@example.com", "secret").role());
    assertThrows(AuthorizationException.class, () -> hotel.withSession(owner, () -> {
      hotel.addUser(new Administrator("evil", "Evil", "evil@example.com", "secret")); return null;
    }));
    assertThrows(AuthorizationException.class, () -> hotel.withSession(admin, () -> {
      hotel.addUser(new Administrator("evil", "Evil", "evil@example.com", "secret")); return null;
    }));
    assertThrows(AuthorizationException.class, () -> hotel.initializeAdministrator("evil", "Evil", "evil@example.com", "secret"));
    assertThrows(AuthorizationException.class, () -> service.deleteClient(admin, "admin"));
    assertEquals(Role.CLIENT, service.me(owner).role());
  }

  @Test void directBusinessAndEntityCallsCannotBypassFacadePermissions() {
    room(101);
    Client first = rawClient("client-1");
    Client second = rawClient("client-2");
    VirtualCard card = rawCard("client-1");
    Room room = hotel.findRoom(101);
    assertThrows(AuthenticationException.class, hotel::getClients);
    assertThrows(AuthenticationException.class, first::getEmail);
    assertThrows(AuthenticationException.class, card::getBalance);
    assertThrows(AuthenticationException.class, () -> first.setName("Anonymous"));
    assertThrows(AuthenticationException.class, () -> card.recharge(100, "Anonymous"));
    assertThrows(AuthenticationException.class, () -> room.setActive(false));
    hotel.withSession(owner, () -> {
      assertThrows(AuthorizationException.class, () -> hotel.addService(new AdditionalService("Unauthorized", 1)));
      assertThrows(AuthorizationException.class, hotel::getClients);
      assertThrows(AuthorizationException.class, () -> card.recharge(100, "Unauthorized"));
      assertThrows(AuthorizationException.class, () -> card.refund(100, "Unauthorized"));
      assertThrows(AuthorizationException.class, () -> card.setActive(false));
      assertThrows(AuthorizationException.class, () -> second.setEmail("stolen@example.com"));
      assertThrows(AuthorizationException.class, second::getName);
      assertThrows(AuthorizationException.class, () -> room.setActive(false));
      assertThrows(HotelException.class, () -> first.attachGuard(null));
      return null;
    });
    assertEquals(0, service.card(admin, "client-1").balance());
    assertTrue(hotel.findRoom(101).isActive());
  }

  @Test void rechargeIsAdministratorOnlyAndCreatesExactlyOneMovement() {
    var card = service.recharge(admin, "client-1", 123.45, "Administrative recharge");
    assertEquals(123.45, card.balance());
    assertEquals(1, card.movements().size());
    assertEquals(MovementType.RECHARGE, card.movements().getFirst().getType());
    assertEquals(card, service.card(owner, "client-1"));
    service.setCardActive(admin, "client-1", false);
    assertThrows(InactiveCardException.class, () -> service.recharge(admin, "client-1", 1, "Rejected"));
    assertEquals(123.45, service.card(owner, "client-1").balance());
    assertEquals(1, service.card(owner, "client-1").movements().size());
  }

  @Test void financialRecordsPreventDeletingClientAndDeletionDoesNotReopenOldEntity() {
    service.recharge(admin, "client-1", 1, "Financial record");
    assertThrows(HotelException.class, () -> service.deleteClient(admin, "client-1"));
    Client removed = rawClient("client-2");
    service.deleteClient(admin, "client-2");
    assertThrows(HotelException.class, () -> hotel.withSession(admin, () -> { removed.setName("Resurrected"); return null; }));
  }
}
