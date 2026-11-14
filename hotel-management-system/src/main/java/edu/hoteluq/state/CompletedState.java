package edu.hoteluq.state;

import edu.hoteluq.enums.ReservationStatus;

public class CompletedState implements ReservationState {
  @Override
  public ReservationStatus getStatus() {
    return ReservationStatus.COMPLETED;
  }
}
