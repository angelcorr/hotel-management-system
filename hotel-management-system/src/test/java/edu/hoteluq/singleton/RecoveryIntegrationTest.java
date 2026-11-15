package edu.hoteluq.singleton;

import edu.hoteluq.enums.ReservationStatus;
import edu.hoteluq.exceptions.*;
import edu.hoteluq.model.Reservation;
import edu.hoteluq.service.ReservationService;
import edu.hoteluq.state.*;
import edu.hoteluq.strategy.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Compatibility checks between recovered phases 1/2 and GitHub State/Strategy. */
class RecoveryIntegrationTest extends Phase2Fixture {
  @Test void legacyStatusAndStateUseTheSameRepresentation() {
    room(101);
    Reservation reservation = reserve("client-1", 101, null, null);
    hotel.withSession(admin, () -> {
      reservation.setStatus(ReservationStatus.CONFIRMED);
      assertInstanceOf(ConfirmedState.class, reservation.getState());
      reservation.setState(new CancelledState());
      assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
      assertTrue(hotel.isAvailable(hotel.findRoom(101), FROM, FROM.plusDays(2)));
      assertThrows(HotelException.class, () -> reservation.setState(null));
      assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
      return null;
    });
  }

  @Test void newStateEntrypointsRespectRegisteredEntityPermissions() {
    room(101);
    Reservation reservation = reserve("client-1", 101, null, null);
    assertThrows(AuthenticationException.class, reservation::getState);
    assertThrows(AuthenticationException.class, reservation::getCheckInDateTime);
    assertThrows(AuthenticationException.class, () -> reservation.setState(new ConfirmedState()));
    assertThrows(AuthenticationException.class, reservation::pay);
    hotel.withSession(owner, () -> {
      assertThrows(AuthorizationException.class, () -> reservation.setState(new ConfirmedState()));
      assertThrows(AuthorizationException.class, reservation::pay);
      assertThrows(AuthorizationException.class, () -> reservation.cancel(new FullRefundPolicy()));
      assertThrows(AuthorizationException.class, reservation::complete);
      assertEquals(ReservationStatus.PENDING, reservation.getStatus());
      return null;
    });
    hotel.withSession(other, () -> {
      assertThrows(AuthorizationException.class, reservation::getState);
      assertThrows(AuthorizationException.class, reservation::getCheckInDateTime);
      return null;
    });
  }

  @Test void preparedStateCannotBypassPendingRegistration() {
    room(101);
    hotel.withSession(admin, () -> {
      Reservation reservation = detached();
      reservation.setState(new ConfirmedState());
      assertThrows(InvalidReservationException.class, () -> hotel.addReservation(reservation));
      assertTrue(hotel.getReservations().isEmpty());
      return null;
    });
  }

  @Test void incompatibleConfirmationFailsBeforeAnyDebit() {
    room(101);
    service.recharge(admin, "client-1", 1000, "Initial balance");
    hotel.withSession(admin, () -> {
      Reservation reservation = detached();
      var before = service.card(admin, "client-1");
      assertThrows(InvalidReservationException.class, () -> new ReservationService().confirm(reservation));
      assertEquals(before, service.card(admin, "client-1"));
      assertEquals(ReservationStatus.PENDING, reservation.getStatus());
      assertTrue(hotel.getReservations().isEmpty());
      hotel.addReservation(reservation);
      assertThrows(InvalidReservationException.class, () -> new ReservationService().confirm(reservation));
      assertEquals(before, service.card(admin, "client-1"));
      assertEquals(ReservationStatus.PENDING, reservation.getStatus());
      assertEquals(1, hotel.getReservations().size());
      return null;
    });
  }

  @Test void githubRefundBoundariesArePreserved() {
    assertEquals(100, RefundPolicySelector.forTimeLeft(Duration.ofHours(72).plusNanos(1)).calculateRefund(100));
    assertEquals(80, RefundPolicySelector.forTimeLeft(Duration.ofHours(72)).calculateRefund(100));
    assertEquals(80, RefundPolicySelector.forTimeLeft(Duration.ofHours(24)).calculateRefund(100));
    assertEquals(0, RefundPolicySelector.forTimeLeft(Duration.ofHours(24).minusNanos(1)).calculateRefund(100));
    assertEquals(0, RefundPolicySelector.forTimeLeft(Duration.ofHours(-1)).calculateRefund(100));
  }

  private Reservation detached() {
    return new Reservation.Builder().client(rawClient("client-1")).room(hotel.findRoom(101))
        .checkIn(FROM).checkOut(FROM.plusDays(2)).build();
  }
}
