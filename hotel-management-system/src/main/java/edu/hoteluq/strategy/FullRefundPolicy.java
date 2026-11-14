package edu.hoteluq.strategy;

public class FullRefundPolicy implements RefundPolicy {
  @Override
  public double calculateRefund(double amountPaid) {
    return amountPaid;
  }

  @Override
  public String getName() {
    return "100% refund";
  }
}
