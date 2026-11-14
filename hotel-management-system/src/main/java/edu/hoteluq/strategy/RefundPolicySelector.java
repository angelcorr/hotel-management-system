package edu.hoteluq.strategy;

import java.time.Duration;

public class RefundPolicySelector {
  private RefundPolicySelector() {}

  // How much time is left before check-in
  public static RefundPolicy forTimeLeft(Duration timeLeft) {
    if (timeLeft.compareTo(Duration.ofHours(72)) > 0) {
      return new FullRefundPolicy();
    }

    if (timeLeft.compareTo(Duration.ofHours(24)) >= 0) {
      return new PartialRefundPolicy();
    }

    return new NoRefundPolicy();
  }
}
