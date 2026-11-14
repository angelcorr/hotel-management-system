package edu.hoteluq.state;

import edu.hoteluq.enums.ReservationStatus;
import edu.hoteluq.model.Reservation;

public class CancelledState implements ReservationState {
  @Override
  public ReservationStatus getStatus() {
    return ReservationStatus.CANCELLED;
  }
}
