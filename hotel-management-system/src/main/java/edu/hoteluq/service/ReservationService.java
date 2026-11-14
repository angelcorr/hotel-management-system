package edu.hoteluq.service;

import edu.hoteluq.model.Reservation;
import edu.hoteluq.singleton.Hotel;

public class ReservationService {
  private final Hotel hotel = Hotel.getInstance();

  public void confirm(Reservation reservation) {
    reservation.pay(); // the state decides if it is allowed or not
    hotel.addReservation(reservation);
  }

}
