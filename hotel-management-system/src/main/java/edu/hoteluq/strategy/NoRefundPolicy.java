package edu.hoteluq.strategy;

public class NoRefundPolicy implements RefundPolicy{
  @Override
  public double calculateRefund(double amountPaid) {
    return calculateRefund(edu.hoteluq.money.CopMoney.input(amountPaid)).doubleValue();
  }

  @Override
  public java.math.BigDecimal calculateRefund(java.math.BigDecimal amountPaid) { return java.math.BigDecimal.ZERO; }

  @Override
  public String getName() {
    return "No refund";
  }
}
