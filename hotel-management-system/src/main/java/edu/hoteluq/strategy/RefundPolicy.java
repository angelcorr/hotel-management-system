package edu.hoteluq.strategy;

public interface RefundPolicy {
  double calculateRefund(double amountPaid);

  String getName();
}
