package edu.hoteluq.singleton;

import edu.hoteluq.service.BookingRules;
import edu.hoteluq.strategy.CancellationSchedule;
import java.time.*;

/** Booking hours are test fixtures only; cancellation always follows the official academic rules. */
final class TestCommercialPolicies {
  static BookingRules rules() {
    return new BookingRules(LocalTime.of(15, 0), LocalTime.NOON,
        new BookingRules.CheckoutWindow(Duration.ZERO, Duration.ofDays(1)));
  }
  static CancellationSchedule cancellation() {
    return new CancellationSchedule("TEST-ONLY", 1, CancellationSchedule.official().windows());
  }
}
