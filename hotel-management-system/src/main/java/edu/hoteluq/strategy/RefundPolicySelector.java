package edu.hoteluq.strategy;

import java.time.Duration;

/** Strategy selector sharing the official schedule boundaries, including 24h and 72h at 80%. */
public class RefundPolicySelector {
  private RefundPolicySelector() {}

  // How much time is left before check-in
  public static RefundPolicy forTimeLeft(Duration timeLeft) {
    if (timeLeft.compareTo(CancellationSchedule.FULL_REFUND_START) >= 0) {
      return new FullRefundPolicy();
    }

    if (timeLeft.compareTo(CancellationSchedule.PARTIAL_REFUND_START) >= 0) {
      return new PartialRefundPolicy();
    }

    return new NoRefundPolicy();
  }
}
