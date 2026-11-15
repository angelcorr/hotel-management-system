package edu.hoteluq.money;

import java.math.*;
import edu.hoteluq.exceptions.HotelException;

/** One rounding boundary per payable/refundable amount. Inputs and intermediate prices retain precision. */
public final class CopMoney {
  public static final String CURRENCY = "COP";
  public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
  private static final BigDecimal MAX = BigDecimal.valueOf(Double.MAX_VALUE);
  private CopMoney() {}
  public static void currency(String currency) {
    if (!CURRENCY.equals(currency)) throw new HotelException("Only COP is supported");
  }
  public static BigDecimal input(BigDecimal amount) {
    if (amount == null || amount.signum() < 0 || amount.compareTo(MAX) > 0) throw new HotelException("Invalid COP amount");
    return amount;
  }
  /** Compatibility conversion only; all arithmetic uses BigDecimal. */
  public static BigDecimal input(double amount) {
    if (!Double.isFinite(amount)) throw new HotelException("Amount must be finite");
    return input(BigDecimal.valueOf(amount));
  }
  public static BigDecimal rounded(BigDecimal amount) { return input(amount).setScale(0, ROUNDING); }
  public static BigDecimal positive(BigDecimal amount) {
    BigDecimal result = rounded(amount);
    if (result.signum() <= 0) throw new HotelException("Final COP amount must be greater than zero");
    return result;
  }
  public static BigDecimal integral(BigDecimal amount) {
    input(amount);
    try { return amount.setScale(0, RoundingMode.UNNECESSARY); }
    catch (ArithmeticException e) { throw new HotelException("Final COP amount must be integral"); }
  }
}
