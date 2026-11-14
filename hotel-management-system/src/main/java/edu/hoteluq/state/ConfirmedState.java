package edu.hoteluq.state;

import edu.hoteluq.enums.ReservationStatus;
import edu.hoteluq.model.Reservation;
import edu.hoteluq.strategy.RefundPolicy;

public class ConfirmedState implements  ReservationState {
  @Override
  public ReservationStatus getStatus() {
    return ReservationStatus.CONFIRMED;
  }

  @Override
  public void cancel(Reservation reservation, RefundPolicy policy) {
    double refund = policy.calculateRefund(reservation.getTotal());
    if (refund > 0) {
      reservation.getClient().getCard().refund(refund, "Refund of reservation " + reservation.getId() + "( " + policy.getName() + ")");
    }
    reservation.setState(new CancelledState());
  }

  @Override
  public void complete(Reservation reservation) {
    reservation.setState(new CompletedState());
  }
}
