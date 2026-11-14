package edu.hoteluq.state;

import edu.hoteluq.enums.ReservationStatus;
import edu.hoteluq.model.Reservation;

public class ConfirmedState implements  ReservationState {
  @Override
  public ReservationStatus getStatus() {
    return ReservationStatus.CONFIRMED;
  }

  @Override
  public void cancel(Reservation reservation) {
    reservation.setState(new CancelledState());
  }

  @Override
  public void complete(Reservation reservation) {
    reservation.setState(new CompletedState());
  }
}
