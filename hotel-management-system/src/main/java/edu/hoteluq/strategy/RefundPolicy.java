package edu.hoteluq.strategy;

public interface RefundPolicy {
  double calculateRefund(double amountPaid);

  /** Decimal API used by the transaction; legacy double API remains source compatible. */
  default java.math.BigDecimal calculateRefund(java.math.BigDecimal amountPaid) {
    throw new UnsupportedOperationException("An exact decimal implementation is required");
  }

  String getName();
}
