package edu.hoteluq.state;

import edu.hoteluq.enums.ReservationStatus;
import edu.hoteluq.exceptions.RoomNotAvailableException;
import edu.hoteluq.model.Reservation;
import edu.hoteluq.singleton.Hotel;
import edu.hoteluq.strategy.RefundPolicy;

public class PendingState implements ReservationState {
  @Override
  public ReservationStatus getStatus() {
    return ReservationStatus.PENDING;
  }

  @Override
  public void pay(Reservation reservation) {
    if (!Hotel.getInstance().isAvailable(reservation.getRoom(), reservation.getCheckIn(), reservation.getCheckOut())) {
      throw new RoomNotAvailableException(reservation.getRoom() + " is not available from " + reservation.getCheckIn() + " to " + reservation.getCheckOut());
    }

    reservation.getClient().getCard().charge(reservation.getTotal(), "Payment of reservation " + reservation.getId());
    reservation.setState(new ConfirmedState());
  }

  @Override
  public void cancel(Reservation reservation, RefundPolicy policy) {
    reservation.setState(new CancelledState());
  }
}
