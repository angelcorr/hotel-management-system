package edu.hoteluq.service;

import edu.hoteluq.model.Reservation;
import edu.hoteluq.singleton.Hotel;
import edu.hoteluq.strategy.FullRefundPolicy;
import edu.hoteluq.strategy.RefundPolicySelector;

import java.time.Duration;
import java.time.LocalDateTime;

public class ReservationService {
  private final Hotel hotel = Hotel.getInstance();

  public void confirm(Reservation reservation) {
    reservation.pay(); // the state decides if it is allowed or not
    hotel.addReservation(reservation);
  }

  public void cancelByClient(Reservation reservation, LocalDateTime when) {
    Duration timeLeft = Duration.between(when, reservation.getCheckInDateTime());
    reservation.cancel(RefundPolicySelector.forTimeLeft(timeLeft));
  }

  public void cancelByClient(Reservation reservation) {
    cancelByClient(reservation, LocalDateTime.now());
  }

  public void cancelByHotel(Reservation reservation) {
    reservation.cancel(new FullRefundPolicy());
  }

}
