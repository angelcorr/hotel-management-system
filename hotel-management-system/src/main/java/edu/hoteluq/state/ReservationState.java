package edu.hoteluq.state;

import edu.hoteluq.enums.ReservationStatus;
import edu.hoteluq.exceptions.InvalidReservationException;
import edu.hoteluq.model.Reservation;
import edu.hoteluq.strategy.RefundPolicy;

public interface ReservationState {
  ReservationStatus getStatus();

  default void pay(Reservation reservation) {
    throw new InvalidReservationException("A " + getStatus() + " reservation cannot be paid");
  }

  default void cancel(Reservation reservation, RefundPolicy policy) {
    throw new InvalidReservationException("A " + getStatus() + " reservation cannot be cancelled");
  }

  default void complete(Reservation reservation) {
    throw new InvalidReservationException("A " + getStatus() + " reservation cannot be completed");
  }
}
