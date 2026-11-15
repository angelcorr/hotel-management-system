package edu.hoteluq.model;

import edu.hoteluq.enums.*;
import edu.hoteluq.exceptions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {
  private final LocalDate from = LocalDate.of(2027, 1, 10);
  private final Client client = new Client("1", "Ana", "ana@example.com", "secret");
  private final RoomType type = new RoomType(RoomCategory.STANDARD, 100, "Standard", 2, "Description");
  private final Room room = new StandardRoom(101, 1, type);

  private Reservation.Builder builder() {
    return new Reservation.Builder().client(client).room(room).checkIn(from).checkOut(from.plusDays(2));
  }

  @Test
  void pricingServicesAndPromotionPreserveExistingFormulaAndFreezeTotal() {
    AdditionalService breakfast = new AdditionalService("Breakfast", 10);
    Reservation reservation = builder().additionalService(breakfast).promotion(new Promotion("Promo", 15)).build();
    assertEquals(2, reservation.getNights());
    assertEquals(210, reservation.getSubtotal());
    assertEquals(31.5, reservation.getDiscount());
    assertEquals(178.5, reservation.getTotal());
    breakfast.setPrice(1000);
    type.setPricePerNight(1000);
    assertEquals(178.5, reservation.getTotal());
    assertThrows(UnsupportedOperationException.class, () -> reservation.getServices().clear());
  }

  @Test
  void decimalPricesAndFullDiscountDoNotProduceNegativeOrNonFiniteTotals() {
    type.setPricePerNight(0.1);
    assertEquals(0.3, builder().checkOut(from.plusDays(3)).build().getTotal());
    assertEquals(0, builder().promotion(new Promotion("Full", 100)).build().getTotal());
    type.setPricePerNight(Double.MAX_VALUE);
    Reservation huge = builder().checkOut(from.plusDays(1)).promotion(new Promotion("Half", 50)).build();
    assertTrue(Double.isFinite(huge.getDiscount()));
    assertTrue(Double.isFinite(huge.getTotal()));
    assertThrows(HotelException.class, () -> builder().build());
  }

  @Test
  void requiredReferencesDatesAndServicesFailWithDomainErrors() {
    assertThrows(InvalidReservationException.class, () -> builder().client(null).build());
    assertThrows(InvalidReservationException.class, () -> builder().room(null).build());
    assertThrows(InvalidReservationException.class, () -> builder().checkIn(null).build());
    assertThrows(InvalidReservationException.class, () -> builder().checkOut(null).build());
    assertThrows(InvalidReservationException.class, () -> builder().checkOut(from).build());
    assertThrows(InvalidReservationException.class, () -> builder().checkOut(from.minusDays(1)).build());
    assertThrows(HotelException.class, () -> builder().additionalService(null));
    AdditionalService spa = new AdditionalService("Spa", 10);
    spa.setAvailable(false);
    assertThrows(InvalidReservationException.class, () -> builder().additionalService(spa).build());
    room.setActive(false);
    assertThrows(InvalidReservationException.class, () -> builder().build());
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 0, 3})
  void guestCountMustFitCapacity(int guests) {
    assertThrows(InvalidReservationException.class, () -> builder().guests(guests).build());
  }

  @Test
  void overlapsAreHalfOpenAndCancellationDoesNotBlockDates() {
    Reservation reservation = builder().guests(2).build();
    assertTrue(reservation.overlaps(from, from.plusDays(1)));
    assertTrue(reservation.overlaps(from.minusDays(1), from.plusDays(1)));
    assertFalse(reservation.overlaps(from.plusDays(2), from.plusDays(3)));
    assertFalse(reservation.overlaps(from.minusDays(1), from));
    reservation.setStatus(ReservationStatus.CANCELLED);
    assertFalse(reservation.overlaps(from, from.plusDays(2)));
    assertThrows(InvalidReservationException.class, () -> reservation.overlaps(null, from));
    assertThrows(InvalidReservationException.class, () -> reservation.overlaps(from, from));
  }

  @Test
  void nullStateDoesNotCorruptReservationAndBuilderCanBeReused() {
    Reservation.Builder builder = builder();
    Reservation first = builder.build();
    assertThrows(HotelException.class, () -> first.setStatus(null));
    assertEquals(ReservationStatus.PENDING, first.getStatus());
    Reservation second = builder.build();
    assertNotEquals(first.getId(), second.getId());
    assertSame(client, first.getClient());
    assertSame(room, first.getRoom());
  }
}
