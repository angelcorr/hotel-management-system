package edu.hoteluq.model;

import edu.hoteluq.enums.RoomCategory;
import edu.hoteluq.enums.MovementType;
import edu.hoteluq.exceptions.HotelException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class CatalogValidationTest {
  private RoomType type() {
    return new RoomType(RoomCategory.STANDARD, 100, "Standard", 2, "Description");
  }

  @ParameterizedTest
  @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
  void invalidPricesAreRejectedWithoutMutatingCatalog(double price) {
    RoomType type = type();
    AdditionalService service = new AdditionalService("Breakfast", 10);
    assertThrows(HotelException.class, () -> type.setPricePerNight(price));
    assertThrows(HotelException.class, () -> service.setPrice(price));
    assertThrows(HotelException.class, () -> new RoomType(RoomCategory.STANDARD, price, "Name", 1, "Description"));
    assertThrows(HotelException.class, () -> new AdditionalService("Breakfast", price));
    assertEquals(100, type.getPricePerNight());
    assertEquals(10, service.getPrice());
  }

  @ParameterizedTest
  @ValueSource(doubles = {-1, 100.01, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
  void invalidPromotionPercentagesAreRejected(double percent) {
    assertThrows(HotelException.class, () -> new Promotion("Promo", percent));
  }

  @Test
  void freeServicesAndPercentageBoundariesRemainSupported() {
    assertEquals(0, new AdditionalService("Courtesy", 0).getPrice());
    assertEquals(0, new Promotion("None", 0).getDiscountPercent());
    assertEquals(100, new Promotion("Full", 100).getDiscountPercent());
    RoomType type = type();
    type.setPricePerNight(0);
    assertEquals(0, type.getPricePerNight());
  }

  @Test
  void roomTypeRequiresCategoryNameDescriptionAndPositiveCapacity() {
    assertThrows(HotelException.class, () -> new RoomType(null, 1, "Name", 1, "Description"));
    assertThrows(HotelException.class, () -> new RoomType(RoomCategory.STANDARD, 1, " ", 1, "Description"));
    assertThrows(HotelException.class, () -> new RoomType(RoomCategory.STANDARD, 1, "Name", 0, "Description"));
    assertThrows(HotelException.class, () -> new RoomType(RoomCategory.STANDARD, 1, "Name", 1, null));
    RoomType type = type();
    assertThrows(HotelException.class, () -> type.setCapacity(-1));
    assertThrows(HotelException.class, () -> type.setDescription(" "));
    assertThrows(HotelException.class, () -> type.setName(null));
    assertEquals(2, type.getCapacity());
    assertEquals("Standard", type.getName());
    assertEquals("Description", type.getDescription());
    assertThrows(HotelException.class, () -> new AdditionalService(null, 0));
    assertThrows(HotelException.class, () -> new Promotion(" ", 0));
  }

  @Test
  void roomsRequirePositiveNumberAndMatchingTypeButAllowBasementFloors() {
    assertThrows(HotelException.class, () -> new StandardRoom(0, 1, type()));
    assertThrows(HotelException.class, () -> new StandardRoom(1, 1, null));
    assertThrows(HotelException.class, () -> new FamilyRoom(1, 1, type()));
    assertThrows(HotelException.class, () -> new SuiteRoom(1, 1, type()));
    assertThrows(HotelException.class, () -> new StandardRoom(1, 1,
        new RoomType(RoomCategory.SUITE, 10, "Suite", 2, "Description")));
    Room room = new StandardRoom(1, -1, type());
    assertEquals(-1, room.getFloor());
    assertFalse(room.getFeatures().isEmpty());
  }

  @Test
  void movementRequiresValidTypeAmountDateAndDescription() {
    LocalDateTime now = LocalDateTime.now();
    assertThrows(HotelException.class, () -> new CardMovement(null, 1, "Payment", now));
    assertThrows(HotelException.class, () -> new CardMovement(MovementType.PAYMENT, Double.NaN, "Payment", now));
    assertThrows(HotelException.class, () -> new CardMovement(MovementType.PAYMENT, 0, "Payment", now));
    assertThrows(HotelException.class, () -> new CardMovement(MovementType.PAYMENT, 1, " ", now));
    assertThrows(HotelException.class, () -> new CardMovement(MovementType.PAYMENT, 1, "Payment", null));
    CardMovement movement = new CardMovement(MovementType.PAYMENT, 1, "Payment", now);
    assertEquals(now, movement.getDate());
    assertEquals(1, movement.getAmount());
    assertEquals("Payment", movement.getDescription());
    assertEquals(MovementType.PAYMENT, movement.getType());
  }
}
