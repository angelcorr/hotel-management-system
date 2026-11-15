package edu.hoteluq.strategy;

import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import edu.hoteluq.exceptions.HotelException;
import edu.hoteluq.validation.DomainValidation;
import edu.hoteluq.money.CopMoney;

/** Immutable policy snapshot. Only schedules equivalent to the official rules may be activated. */
public record CancellationSchedule(String id, long version, List<Window> windows) {
  public static final String OFFICIAL_ID = "HOTELUQ-ACADEMIC-CANCELLATION";
  public static final long OFFICIAL_VERSION = 1;
  public static final Duration PARTIAL_REFUND_START = Duration.ofHours(24);
  // Duration has nanosecond precision: exactly 72h still belongs to the 80% window.
  public static final Duration FULL_REFUND_START = Duration.ofHours(72).plusNanos(1);
  private static final List<Window> OFFICIAL_WINDOWS = List.of(
      new Window(null, PARTIAL_REFUND_START, BigDecimal.ZERO),
      new Window(PARTIAL_REFUND_START, FULL_REFUND_START, new BigDecimal("80")),
      new Window(FULL_REFUND_START, null, new BigDecimal("100")));

  public static CancellationSchedule official() {
    return new CancellationSchedule(OFFICIAL_ID, OFFICIAL_VERSION, OFFICIAL_WINDOWS);
  }

  public record Window(Duration lowerInclusive, Duration upperExclusive, BigDecimal percent) {
    public Window {
      if (percent == null || percent.signum() < 0 || percent.compareTo(new BigDecimal("100")) > 0) throw new HotelException("Refund percentage must be in [0,100]");
      if (lowerInclusive != null && upperExclusive != null && lowerInclusive.compareTo(upperExclusive) >= 0) throw new HotelException("Empty or inverted window");
    }
    boolean contains(Duration left) {
      return (lowerInclusive == null || left.compareTo(lowerInclusive) >= 0)
          && (upperExclusive == null || left.compareTo(upperExclusive) < 0);
    }
  }
  /** Validate the entire partition, including arbitrarily small or remote alternative windows.
   * Equivalent splits and decimal scales are accepted; historical nonofficial snapshots remain readable.
   */
  public void requireOfficial() {
    for (Window candidate : windows) {
      for (Window official : OFFICIAL_WINDOWS) {
        if (overlaps(candidate, official) && candidate.percent().compareTo(official.percent()) != 0) {
          throw new HotelException("Only the official client cancellation rules 100/80/0 may be activated");
        }
      }
    }
  }

  private static boolean overlaps(Window a, Window b) {
    Duration lower = a.lowerInclusive() == null ? b.lowerInclusive()
        : b.lowerInclusive() == null ? a.lowerInclusive()
        : a.lowerInclusive().compareTo(b.lowerInclusive()) >= 0 ? a.lowerInclusive() : b.lowerInclusive();
    Duration upper = a.upperExclusive() == null ? b.upperExclusive()
        : b.upperExclusive() == null ? a.upperExclusive()
        : a.upperExclusive().compareTo(b.upperExclusive()) <= 0 ? a.upperExclusive() : b.upperExclusive();
    return lower == null || upper == null || lower.compareTo(upper) < 0;
  }

  public CancellationSchedule {
    id = DomainValidation.text(id, "Policy ID");
    if (version < 1 || windows == null || windows.isEmpty()) throw new HotelException("Policy version and complete windows required");
    windows.forEach(w -> DomainValidation.required(w, "Cancellation window"));
    windows = List.copyOf(windows);
    if (windows.getFirst().lowerInclusive() != null || windows.getLast().upperExclusive() != null) throw new HotelException("Windows must cover all time-left values");
    for (int i = 1; i < windows.size(); i++) {
      Duration end = windows.get(i - 1).upperExclusive();
      if (end == null || windows.get(i).lowerInclusive() == null || !end.equals(windows.get(i).lowerInclusive())) throw new HotelException("Windows must be contiguous and ordered, without gaps or overlaps");
    }
  }
  public RefundPolicy select(Duration left) {
    Window window = windows.stream().filter(w -> w.contains(left)).findFirst().orElseThrow();
    return new ConfiguredRefundPolicy(id, version, window.percent());
  }
  public record ConfiguredRefundPolicy(String policyId, long version, BigDecimal percent) implements RefundPolicy {
    @Override public double calculateRefund(double paid) { return calculateRefund(CopMoney.input(paid)).doubleValue(); }
    @Override public BigDecimal calculateRefund(BigDecimal paid) { return CopMoney.rounded(paid.multiply(percent).movePointLeft(2)); }
    @Override public String getName() { return policyId + "/v" + version + "/" + percent.toPlainString() + "%"; }
  }
}
