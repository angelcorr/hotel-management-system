package edu.hoteluq.strategy;

public class NoRefundPolicy implements RefundPolicy{
  @Override
  public double calculateRefund(double amountPaid) {
    return 0;
  }

  @Override
  public String getName() {
    return "No refund";
  }
}
