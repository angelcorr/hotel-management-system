package edu.hoteluq.singleton;

import edu.hoteluq.enums.*;
import edu.hoteluq.exceptions.*;
import edu.hoteluq.model.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CatalogAdministrationTest extends Phase2Fixture {
  @Test void clientCannotSmuggleConfirmationBeforeReservationRegistration() {
    room(101);
    hotel.withSession(owner, () -> {
      Reservation reservation = new Reservation.Builder().client((Client) hotel.currentUser())
          .room(hotel.findRoom(101)).checkIn(FROM).checkOut(FROM.plusDays(1)).build();
      reservation.setStatus(ReservationStatus.CONFIRMED);
      assertThrows(InvalidReservationException.class, () -> hotel.addReservation(reservation));
      assertTrue(hotel.getReservations().isEmpty());
      return null;
    });
  }

  @Test void roomTypeCrudValidatesCategoryAndPreservesStableIdentity() {
    service.createRoomType(admin, RoomCategory.FAMILY, "Family", 200, 4, "Family description");
    var changed = service.updateRoomType(admin, RoomCategory.FAMILY, "Family deluxe", 250, 5, "Updated");
    assertEquals(RoomCategory.FAMILY, changed.category());
    assertEquals(250, changed.pricePerNight());
    assertEquals(5, changed.capacity());
    assertThrows(HotelException.class, () -> service.createRoomType(admin, RoomCategory.FAMILY, "Duplicate", 1, 1, "Description"));
    assertThrows(HotelException.class, () -> service.createRoomType(admin, RoomCategory.SUITE, " STANDARD ", 1, 1, "Description"));
    assertThrows(HotelException.class, () -> service.updateRoomType(admin, RoomCategory.FAMILY, "STANDARD", 1, 1, "Description"));
    service.deleteRoomType(admin, RoomCategory.FAMILY);
    assertEquals(1, service.roomTypes(owner).size());
    assertThrows(HotelException.class, () -> service.deleteRoomType(admin, RoomCategory.FAMILY));
  }

  @Test void roomCrudUsesFactoryAndRejectsDuplicateNumbersAndMissingTypes() {
    var created = service.createRoom(admin, RoomCategory.STANDARD, 101, -1);
    assertEquals(RoomCategory.STANDARD, created.category());
    assertFalse(created.features().isEmpty());
    var changed = service.updateRoom(admin, 101, 2, false);
    assertEquals(2, changed.floor());
    assertFalse(changed.active());
    assertTrue(service.availableRooms(owner, FROM, FROM.plusDays(1)).isEmpty());
    service.setRoomActive(admin, 101, true);
    assertEquals(1, service.availableRooms(owner, FROM, FROM.plusDays(1)).size());
    assertThrows(DuplicateRoomException.class, () -> service.createRoom(admin, RoomCategory.STANDARD, 101, 3));
    assertThrows(HotelException.class, () -> service.createRoom(admin, RoomCategory.SUITE, 102, 3));
    assertThrows(HotelException.class, () -> service.deleteRoomType(admin, RoomCategory.STANDARD));
    service.deleteRoom(admin, 101);
    assertTrue(service.rooms(owner).isEmpty());
    service.deleteRoomType(admin, RoomCategory.STANDARD);
    assertTrue(service.roomTypes(owner).isEmpty());
  }

  @Test void inactiveTypeExcludesItsRoomsWithoutDeletingOrCancellingAnything() {
    room(101);
    service.setRoomTypeActive(admin, RoomCategory.STANDARD, false);
    assertFalse(service.rooms(owner).getFirst().active());
    assertTrue(service.availableRooms(owner, FROM, FROM.plusDays(1)).isEmpty());
    service.setRoomTypeActive(admin, RoomCategory.STANDARD, true);
    assertEquals(1, service.availableRooms(owner, FROM, FROM.plusDays(1)).size());
  }

  @Test void additionalServiceCrudHasStableIdAndNormalizedNameUniqueness() {
    var created = service.createService(admin, " Spa ", 10);
    assertEquals("Spa", created.name());
    assertThrows(HotelException.class, () -> service.createService(admin, " SPA ", 20));
    var breakfast = service.createService(admin, "Breakfast", 20);
    assertThrows(HotelException.class, () -> service.updateService(admin, breakfast.id(), "spa", 30));
    var changed = service.updateService(admin, created.id(), "Wellness", 25);
    assertEquals(created.id(), changed.id());
    assertEquals("Wellness", changed.name());
    assertEquals(25, changed.price());
    service.setServiceAvailable(admin, created.id(), false);
    assertFalse(service.services(owner).stream().filter(view -> view.id().equals(created.id())).findFirst().orElseThrow().available());
    service.deleteService(admin, created.id());
    assertEquals(1, service.services(owner).size());
    assertThrows(HotelException.class, () -> service.deleteService(admin, created.id()));
  }

  @Test void promotionCatalogCrudDoesNotPretendToImplementEligibility() {
    var created = service.createPromotion(admin, "Long stay", 15);
    assertThrows(HotelException.class, () -> service.createPromotion(admin, " LONG STAY ", 10));
    var second = service.createPromotion(admin, "Other", 10);
    assertThrows(HotelException.class, () -> service.updatePromotion(admin, second.id(), "long stay", 10));
    var updated = service.updatePromotion(admin, created.id(), "Updated", 20);
    assertEquals(created.id(), updated.id());
    assertEquals("Updated", updated.name());
    assertEquals(20, updated.discountPercent());
    service.deletePromotion(admin, created.id());
    assertEquals(1, service.promotions(owner).size());
    assertThrows(HotelException.class, () -> service.deletePromotion(admin, created.id()));
  }

  @Test void invalidMultiFieldUpdatesAreAtomic() {
    var typeBefore = service.roomTypes(admin);
    assertThrows(HotelException.class, () -> service.updateRoomType(admin, RoomCategory.STANDARD, "Changed", 200, 0, "Changed"));
    assertEquals(typeBefore, service.roomTypes(admin));
    var serviceBefore = service.createService(admin, "Spa", 10);
    assertThrows(HotelException.class, () -> service.updateService(admin, serviceBefore.id(), "Changed", Double.NaN));
    assertEquals(List.of(serviceBefore), service.services(admin));
    var promotionBefore = service.createPromotion(admin, "Promo", 10);
    assertThrows(HotelException.class, () -> service.updatePromotion(admin, promotionBefore.id(), "Changed", 101));
    assertEquals(List.of(promotionBefore), service.promotions(admin));
  }

  @Test void unknownRecordsAndInvalidInputsAreRejected() {
    assertThrows(HotelException.class, () -> service.createRoomType(admin, null, "Null", 1, 1, "Description"));
    assertThrows(HotelException.class, () -> service.createRoom(admin, RoomCategory.STANDARD, 0, 1));
    assertThrows(HotelException.class, () -> service.updateRoom(admin, 404, 1, true));
    assertThrows(HotelException.class, () -> service.deleteRoom(admin, 404));
    assertThrows(HotelException.class, () -> service.updateService(admin, UUID.randomUUID(), "Missing", 1));
    assertThrows(HotelException.class, () -> service.updatePromotion(admin, UUID.randomUUID(), "Missing", 1));
    assertThrows(HotelException.class, () -> service.deleteService(admin, null));
    assertThrows(HotelException.class, () -> service.deletePromotion(admin, null));
  }

  @Test void directCatalogSettersCannotBypassUniquenessOrAuthorization() {
    var first = service.createService(admin, "First", 1);
    service.createService(admin, "Second", 1);
    var promo = service.createPromotion(admin, "First promo", 10);
    service.createPromotion(admin, "Second promo", 20);
    hotel.withSession(admin, () -> {
      assertThrows(HotelException.class, () -> hotel.findService(first.id()).setName(" SECOND "));
      assertThrows(HotelException.class, () -> hotel.findPromotion(promo.id()).setName(" SECOND PROMO "));
      return null;
    });
    hotel.withSession(owner, () -> {
      assertThrows(AuthorizationException.class, () -> hotel.getRoomType(RoomCategory.STANDARD).setPricePerNight(1));
      assertThrows(AuthorizationException.class, () -> hotel.findService(first.id()).setAvailable(false));
      assertThrows(AuthorizationException.class, () -> hotel.findPromotion(promo.id()).setDiscountPercent(100));
      return null;
    });
    assertEquals("First", service.services(owner).getFirst().name());
    assertEquals(100, service.roomTypes(owner).getFirst().pricePerNight());
  }

  @Test void historicalReferencesBlockDestructiveDeletesAndDetailEdits() {
    room(101);
    var spa = service.createService(admin, "Spa", 10);
    var promo = service.createPromotion(admin, "Promo", 10);
    reserve("client-1", 101, spa.id(), promo.id());
    assertThrows(HotelException.class, () -> service.deleteClient(admin, "client-1"));
    assertThrows(HotelException.class, () -> service.deleteRoom(admin, 101));
    assertThrows(HotelException.class, () -> service.deleteRoomType(admin, RoomCategory.STANDARD));
    assertThrows(HotelException.class, () -> service.deleteService(admin, spa.id()));
    assertThrows(HotelException.class, () -> service.deletePromotion(admin, promo.id()));
    assertThrows(HotelException.class, () -> service.updateRoom(admin, 101, 2, true));
    assertThrows(HotelException.class, () -> service.updateRoomType(admin, RoomCategory.STANDARD, "Changed", 200, 3, "Changed"));
    assertThrows(HotelException.class, () -> service.updateService(admin, spa.id(), "Changed", 50));
    assertThrows(HotelException.class, () -> service.updatePromotion(admin, promo.id(), "Changed", 20));
    assertEquals(189, service.history(owner, "client-1").getFirst().total());
    service.setRoomActive(admin, 101, false);
    service.setRoomTypeActive(admin, RoomCategory.STANDARD, false);
    service.setServiceAvailable(admin, spa.id(), false);
    assertEquals(1, service.history(owner, "client-1").size());
  }

  @Test void referencesRemainProtectedAfterCancellation() {
    room(101);
    Reservation reservation = reserve("client-1", 101, null, null);
    hotel.withSession(admin, () -> { hotel.cancelReservation(reservation, true); return null; });
    assertThrows(HotelException.class, () -> service.deleteRoom(admin, 101));
    assertThrows(HotelException.class, () -> service.deleteClient(admin, "client-1"));
    assertEquals(1, service.history(owner, "client-1").size());
  }

  @Test void clientHistoryIsFilteredAndSensitiveReservationReferencesRemainProtected() {
    room(101);
    room(102);
    Reservation first = reserve("client-1", 101, null, null);
    Reservation second = reserve("client-2", 102, null, null);
    assertEquals(1, service.history(owner, "client-1").size());
    assertEquals(1, service.history(other, "client-2").size());
    assertEquals(2, hotel.withSession(admin, hotel::getReservations).size());
    assertEquals(1, hotel.withSession(owner, hotel::getReservations).size());
    hotel.withSession(owner, () -> {
      assertThrows(AuthorizationException.class, second::getClient);
      assertThrows(AuthorizationException.class, second::getTotal);
      assertThrows(AuthorizationException.class, () -> first.setStatus(ReservationStatus.CONFIRMED));
      return null;
    });
    assertThrows(AuthenticationException.class, first::getTotal);
  }

  @Test void detachedServicesOrPromotionsCannotEnterRegisteredReservations() {
    room(101);
    hotel.withSession(admin, () -> {
      var builder = new Reservation.Builder().client((Client) hotel.findUser("client-1"))
          .room(hotel.findRoom(101)).checkIn(FROM).checkOut(FROM.plusDays(1));
      assertThrows(HotelException.class, () -> hotel.addReservation(builder.additionalService(new AdditionalService("External", 1)).build()));
      var withPromo = new Reservation.Builder().client((Client) hotel.findUser("client-1"))
          .room(hotel.findRoom(101)).checkIn(FROM).checkOut(FROM.plusDays(1)).promotion(new Promotion("External", 1)).build();
      assertThrows(HotelException.class, () -> hotel.addReservation(withPromo));
      assertTrue(hotel.getReservations().isEmpty());
      return null;
    });
  }

  @Test void deletedCatalogEntitiesCannotBeReusedThroughOldReferences() {
    var created = service.createService(admin, "Spa", 10);
    AdditionalService reference = hotel.findService(created.id());
    service.deleteService(admin, created.id());
    assertThrows(HotelException.class, () -> hotel.withSession(admin, () -> { reference.setPrice(1); return null; }));
    assertThrows(HotelException.class, () -> hotel.withSession(admin, () -> { hotel.addService(reference); return null; }));
  }

  @Test void viewCollectionsAreImmutableAndDoNotExposeManagedObjects() {
    room(101);
    var types = service.roomTypes(owner);
    var rooms = service.rooms(owner);
    assertThrows(UnsupportedOperationException.class, types::clear);
    assertThrows(UnsupportedOperationException.class, rooms::clear);
    assertThrows(UnsupportedOperationException.class, () -> rooms.getFirst().features().add("Fake"));
    assertThrows(UnsupportedOperationException.class, () -> service.card(owner, "client-1").movements().clear());
    service.updateRoomType(admin, RoomCategory.STANDARD, "Changed", 150, 2, "Description");
    assertEquals("Standard", types.getFirst().name());
  }
}
