package edu.hoteluq.loyalty;

import java.math.*;
import edu.hoteluq.money.CopMoney;
import edu.hoteluq.exceptions.HotelException;

/** Explicit commercial configuration. There is deliberately no production default. */
public record LoyaltyPolicy(String id, long version, BigDecimal paidUnit, long pointsPerUnit,
    RefundTreatment refundTreatment, SpentTreatment spentTreatment, RedemptionCancellation redemptionCancellation) {
  public enum RefundTreatment { REVOKE_ALL_ON_CANCELLATION, RECALCULATE_RETAINED_PAYMENT }
  public enum SpentTreatment { CARRY_FORWARD_DEBT }
  public enum RedemptionCancellation { KEEP_SPENT, RESTORE_ORIGINAL_EXPIRY }
  public LoyaltyPolicy {
    if (id == null || id.isBlank() || version < 1 || pointsPerUnit < 1) throw new HotelException("Invalid loyalty policy");
    paidUnit = CopMoney.input(paidUnit);
    if (paidUnit.signum() == 0) throw new HotelException("Positive accrual unit required");
    if (refundTreatment == null || spentTreatment == null || redemptionCancellation == null) throw new HotelException("Explicit refund and redemption cancellation rules required");
  }
  public long points(BigDecimal paid) {
    try { return CopMoney.input(paid).multiply(BigDecimal.valueOf(pointsPerUnit)).divide(paidUnit, 0, RoundingMode.DOWN).longValueExact(); }
    catch (ArithmeticException e) { throw new HotelException("Points overflow"); }
  }
}
