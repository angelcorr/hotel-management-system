package edu.hoteluq.strategy;

public class FullRefundPolicy implements RefundPolicy {
  @Override
  public double calculateRefund(double amountPaid) {
    return calculateRefund(edu.hoteluq.money.CopMoney.input(amountPaid)).doubleValue();
  }

  @Override
  public java.math.BigDecimal calculateRefund(java.math.BigDecimal amountPaid) { return amountPaid; }

  @Override
  public String getName() {
    return "100% refund";
  }
}
