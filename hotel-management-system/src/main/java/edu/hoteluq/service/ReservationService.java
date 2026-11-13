package edu.hoteluq.service;

import edu.hoteluq.enums.ReservationStatus;
import edu.hoteluq.exceptions.HotelException;
import edu.hoteluq.exceptions.RoomNotAvailableException;
import edu.hoteluq.model.Reservation;
import edu.hoteluq.model.VirtualCard;
import edu.hoteluq.singleton.Hotel;

public class ReservationService {
  private final Hotel hotel = Hotel.getInstance();

  public void confirm(Reservation reservation) {
    if (reservation.getStatus() != ReservationStatus.PENDING) {
      throw new HotelException("Only a pending reservation can be paid");
    }

    if (!hotel.isAvailable(reservation.getRoom(), reservation.getCheckIn(), reservation.getCheckOut())) {
      throw new RoomNotAvailableException(reservation.getRoom() + " is not available from " + reservation.getCheckIn() + " to " + reservation.getCheckOut());
    }

    VirtualCard card = reservation.getClient().getCard();
    card.charge(reservation.getTotal(), "Payment of reservation " + reservation.getId());

    reservation.setStatus(ReservationStatus.CONFIRMED);
    hotel.addReservation(reservation);
  }

}
