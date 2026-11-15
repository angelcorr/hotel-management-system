package edu.hoteluq.strategy;

public class PartialRefundPolicy implements RefundPolicy {
  @Override
  public double calculateRefund(double amountPaid) {
    return calculateRefund(edu.hoteluq.money.CopMoney.input(amountPaid)).doubleValue();
  }

  @Override
  public java.math.BigDecimal calculateRefund(java.math.BigDecimal amountPaid) { return amountPaid.multiply(new java.math.BigDecimal("0.8")); }

  @Override
  public String getName() {
    return "80% refund";
  }
}
