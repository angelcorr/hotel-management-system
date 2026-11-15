package edu.hoteluq.promotion;

import java.math.BigDecimal;
import edu.hoteluq.money.CopMoney;
import edu.hoteluq.exceptions.HotelException;

/** Decorator: ordered discounts wrap the same raw amount contract. Round only the final obligation. */
public sealed interface PriceComponent permits PriceComponent.Base,PriceComponent.Percent,PriceComponent.Fixed {
  BigDecimal amount();
  record Base(BigDecimal amount) implements PriceComponent { public Base { CopMoney.input(amount); } }
  record Percent(PriceComponent wrapped,BigDecimal percent) implements PriceComponent {
    public Percent { if(wrapped==null || percent==null || percent.signum()<0 || percent.compareTo(new BigDecimal("100"))>0) throw new HotelException("Invalid percentage component"); }
    public BigDecimal amount() { return wrapped.amount().multiply(BigDecimal.ONE.subtract(percent.movePointLeft(2))); }
  }
  record Fixed(PriceComponent wrapped,BigDecimal discount) implements PriceComponent {
    public Fixed { if(wrapped==null) throw new HotelException("Price component required"); CopMoney.input(discount); }
    public BigDecimal amount() { return wrapped.amount().subtract(discount); }
  }
  static BigDecimal net(BigDecimal subtotal,BigDecimal percent,BigDecimal points,PromotionDefinition.Combination combination) {
    PriceComponent base=new Base(subtotal);
    if(points.signum()==0) return new Percent(base,percent).amount();
    if(combination==null || combination==PromotionDefinition.Combination.EXCLUSIVE) throw new HotelException("Discount combination not authorized");
    return (combination==PromotionDefinition.Combination.PROMOTION_THEN_POINTS
        ? new Fixed(new Percent(base,percent),points) : new Percent(new Fixed(base,points),percent)).amount();
  }
}
