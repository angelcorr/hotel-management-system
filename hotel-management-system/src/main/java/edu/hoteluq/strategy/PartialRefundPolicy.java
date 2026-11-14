package edu.hoteluq.strategy;

public class PartialRefundPolicy implements RefundPolicy {
  @Override
  public double calculateRefund(double amountPaid) {
    return amountPaid * 0.8;
  }

  @Override
  public String getName() {
    return "80% refund";
  }
}
