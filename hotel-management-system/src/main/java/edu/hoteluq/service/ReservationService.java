package edu.hoteluq.service;

import edu.hoteluq.model.Reservation;
import edu.hoteluq.exceptions.InvalidReservationException;
import edu.hoteluq.validation.DomainValidation;
import edu.hoteluq.strategy.FullRefundPolicy;
import edu.hoteluq.strategy.RefundPolicySelector;

import java.time.Duration;
import java.time.LocalDateTime;

public class ReservationService {
  /** Atomic confirmation is not integrated yet; never debit before a rejected registration. */
  public void confirm(Reservation reservation) {
    DomainValidation.required(reservation, "Reservation");
    // Phase 2 accepts only PENDING registrations. The GitHub sequence pay() ->
    // addReservation() would debit first, then reject the CONFIRMED registration.
    // Keep this entry point fail-fast until phase 3 supplies an atomic workflow.
    throw new InvalidReservationException("Atomic confirmation is pending phase 3");
  }

  public void cancelByClient(Reservation reservation, LocalDateTime when) {
    Duration timeLeft = Duration.between(when, reservation.getCheckInDateTime());
    reservation.cancel(RefundPolicySelector.forTimeLeft(timeLeft));
  }

  public void cancelByClient(Reservation reservation) {
    cancelByClient(reservation, LocalDateTime.now());
  }

  public void cancelByHotel(Reservation reservation) {
    reservation.cancel(new FullRefundPolicy());
  }

}
