package edu.hoteluq.state;

import edu.hoteluq.enums.ReservationStatus;
import edu.hoteluq.model.Reservation;
import edu.hoteluq.strategy.RefundPolicy;

public final class ConfirmedState implements ReservationState {
  @Override public ReservationStatus getStatus() { return ReservationStatus.CONFIRMED; }
  @Override public void cancel(Reservation reservation, RefundPolicy policy) { reservation.setState(new CancelledState()); }
  @Override public void complete(Reservation reservation) { reservation.setState(new CompletedState()); }
}
