package edu.hoteluq.validation;

import edu.hoteluq.exceptions.HotelException;
import edu.hoteluq.exceptions.InvalidReservationException;
import java.time.LocalDate;

/** Shared input checks; does not choose commercial policies. */
public final class DomainValidation {
  private DomainValidation() {}

  public static <T> T required(T value, String field) {
    if (value == null) throw new HotelException(field + " is required");
    return value;
  }

  public static String text(String value, String field) {
    if (value == null || value.isBlank()) throw new HotelException(field + " is required");
    return value.strip();
  }

  public static String email(String value) {
    String email = text(value, "Email");
    if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
      throw new HotelException("Invalid email");
    }
    return email;
  }

  public static int positive(int value, String field) {
    if (value <= 0) throw new HotelException(field + " must be greater than zero");
    return value;
  }

  public static double nonNegative(double value, String field) {
    if (!Double.isFinite(value) || value < 0) {
      throw new HotelException(field + " must be finite and non-negative");
    }
    return value;
  }

  public static double positive(double value, String field) {
    nonNegative(value, field);
    if (value == 0) throw new HotelException(field + " must be greater than zero");
    return value;
  }

  public static void period(LocalDate from, LocalDate to) {
    if (from == null || to == null || !to.isAfter(from)) {
      throw new InvalidReservationException("Check-out must be after check-in; both dates are required");
    }
  }
}
