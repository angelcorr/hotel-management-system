package edu.hoteluq.exceptions;

/** Correctly spelled subtype; existing catches of the legacy exception still work. */
public class InsufficientBalanceException extends InsuficientBalanceException {
  public InsufficientBalanceException(String message) { super(message); }
}
