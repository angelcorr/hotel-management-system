package edu.hoteluq.service;

import java.time.*;
import edu.hoteluq.config.HotelTime;
import edu.hoteluq.exceptions.HotelException;
import edu.hoteluq.validation.DomainValidation;

/** Official zone and zero-price prohibition cannot be overridden by callers. Hours still need operator input. */
public record BookingRules(LocalTime checkInTime, LocalTime completionTime, CheckoutWindow checkoutWindow) {
  public BookingRules { DomainValidation.required(checkInTime, "Check-in time"); }
  public ZoneId zone() { return HotelTime.ZONE; }
  public boolean allowZeroTotal() { return false; }
  public boolean allowPendingModification() { return true; }
  /** No policy is inferred from this legacy flag. Cancellation requires a versioned schedule. */
  @Deprecated public boolean allowLateCancellation() { throw new HotelException("Use the explicit versioned cancellation schedule"); }
  @Deprecated public BookingRules(ZoneId zone, LocalTime checkIn, boolean late, boolean zero,
      boolean modify, LocalTime checkOut) {
    this(checkIn, checkOut, null);
    if (!HotelTime.ZONE.equals(zone) || zero || !modify) throw new HotelException("Rules contradict phase 3B business policy");
  }
  /** Inclusive authorized interval relative to the scheduled checkout instant; absent means blocked. */
  public record CheckoutWindow(Duration earliestOffset, Duration latestOffset) {
    public CheckoutWindow {
      DomainValidation.required(earliestOffset, "Earliest checkout offset");
      DomainValidation.required(latestOffset, "Latest checkout offset");
      if (earliestOffset.compareTo(latestOffset) > 0) throw new HotelException("Invalid checkout window");
    }
    public boolean allows(Instant scheduled, Instant actual) {
      return !actual.isBefore(scheduled.plus(earliestOffset)) && !actual.isAfter(scheduled.plus(latestOffset));
    }
  }
}
