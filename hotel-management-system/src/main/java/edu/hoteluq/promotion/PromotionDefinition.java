package edu.hoteluq.promotion;

import edu.hoteluq.exceptions.HotelException;
import edu.hoteluq.validation.DomainValidation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/** Immutable commercial contract. No example thresholds, rates or stacking rules are activated. */
public record PromotionDefinition(UUID id,long version,Settings settings) {
  public enum Combination { EXCLUSIVE, PROMOTION_THEN_POINTS, POINTS_THEN_PROMOTION }
  public enum PointsAccrual { EARN_ON_NET_PAYMENT, EXCLUDED }
  public PromotionDefinition { if(id==null || version<1 || settings==null) throw new HotelException("Invalid promotion version"); }
  public record Settings(String name,String description,BigDecimal percent,boolean active,
      Instant validFrom,Instant validUntil,List<PromotionCondition> conditions,
      Combination combination,PointsAccrual pointsAccrual) {
    public Settings {
      name=DomainValidation.text(name,"Promotion name"); description=DomainValidation.text(description,"Promotion description");
      if(percent==null || percent.signum()<0 || percent.compareTo(new BigDecimal("100"))>0) throw new HotelException("Invalid discount percent");
      if((validFrom==null)!=(validUntil==null) || (validFrom!=null && !validFrom.isBefore(validUntil))) throw new HotelException("Invalid promotion validity");
      if(conditions==null || conditions.isEmpty() || combination==null || pointsAccrual==null) throw new HotelException("Explicit conditions, stacking and accrual decisions required");
      conditions=List.copyOf(conditions);
      // One predicate per family avoids contradictory duplicate constraints; conjunction is explicit.
      Set<Class<?>> families=new HashSet<>();
      for(var c:conditions) if(c instanceof PromotionCondition.All || !families.add(c.getClass())) throw new HotelException("Configure each condition family once");
    }
    public Settings withActive(boolean value) { return new Settings(name,description,percent,value,validFrom,validUntil,conditions,combination,pointsAccrual); }
  }
  public void requireEligible(PromotionCondition.Context context,Instant now) {
    if(!settings.active() || (settings.validFrom()!=null && (now.isBefore(settings.validFrom()) || !now.isBefore(settings.validUntil())))) throw new HotelException("Promotion inactive or outside validity");
    if(!new PromotionCondition.All(settings.conditions()).satisfiedBy(context)) throw new HotelException("Promotion conditions not satisfied");
  }
}
