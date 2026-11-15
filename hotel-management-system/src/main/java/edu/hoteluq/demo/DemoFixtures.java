package edu.hoteluq.demo;

import edu.hoteluq.service.BookingRules;
import edu.hoteluq.loyalty.LoyaltyPolicy;
import java.math.BigDecimal;
import java.time.*;

/** Academic fixtures only. Referenced solely by the explicit isolated --demo bootstrap and tests. */
public final class DemoFixtures {
  private DemoFixtures(){}
  public static final String ADMIN_ID="demo-admin",CLIENT_ID="demo-client",CLIENT_EMAIL="cliente-demo@hoteluq.invalid";
  public static BookingRules rules(){return new BookingRules(LocalTime.of(15,0),LocalTime.NOON,new BookingRules.CheckoutWindow(Duration.ZERO,Duration.ofDays(1)));}
  public static LoyaltyPolicy points(){return new LoyaltyPolicy("ACADEMIC-DEMO-NOT-APPROVED",1,new BigDecimal("100000"),100,LoyaltyPolicy.RefundTreatment.RECALCULATE_RETAINED_PAYMENT,LoyaltyPolicy.SpentTreatment.CARRY_FORWARD_DEBT,LoyaltyPolicy.RedemptionCancellation.RESTORE_ORIGINAL_EXPIRY);}
  public static BigDecimal nightly(){return new BigDecimal("100000");}
  public static BigDecimal servicePrice(){return new BigDecimal("25000");}
  public static BigDecimal funds(){return new BigDecimal("5000000");}
}
