package edu.hoteluq.state;

import edu.hoteluq.enums.ReservationStatus;
import edu.hoteluq.model.Reservation;
import edu.hoteluq.strategy.RefundPolicy;

/** State only controls transitions; the aggregate owns availability and money atomically. */
public final class PendingState implements ReservationState {
  @Override public ReservationStatus getStatus() { return ReservationStatus.PENDING; }
  @Override public void pay(Reservation reservation) { reservation.setState(new ConfirmedState()); }
  @Override public void cancel(Reservation reservation, RefundPolicy policy) { reservation.setState(new CancelledState()); }
}
