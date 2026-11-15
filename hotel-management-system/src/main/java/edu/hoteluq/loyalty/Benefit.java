package edu.hoteluq.loyalty;

import java.math.BigDecimal;
import java.util.UUID;
import edu.hoteluq.money.CopMoney;
import edu.hoteluq.exceptions.HotelException;

/** Versioned equivalence; a monetary discount or a service/package entitlement. */
public record Benefit(UUID id, long version, String name, long cost, Kind kind, BigDecimal discount, UUID targetId) {
  public enum Kind { RESERVATION_DISCOUNT, SERVICE, PACKAGE }
  public Benefit {
    if (id == null || version < 1 || name == null || name.isBlank() || cost < 1 || kind == null) throw new HotelException("Invalid benefit");
    if (kind == Kind.RESERVATION_DISCOUNT) {
      discount = CopMoney.positive(discount);
      if (targetId != null) throw new HotelException("Discount cannot reference a service");
    } else if (targetId == null || discount != null) throw new HotelException("Service/package benefit requires only a target");
  }
}
