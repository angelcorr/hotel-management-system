package edu.hoteluq.exceptions;
public class AuthorizationException extends HotelException {
  public AuthorizationException() { super("Operation not permitted"); }
}
