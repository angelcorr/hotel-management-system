package edu.hoteluq.loyalty;

import edu.hoteluq.exceptions.HotelException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class BenefitValidationTest {
  @ParameterizedTest @ValueSource(longs={0,-1}) void invalidCost(long value) {
    assertThrows(HotelException.class,()->new Benefit(UUID.randomUUID(),1,"x",value,Benefit.Kind.RESERVATION_DISCOUNT,BigDecimal.ONE,null));
  }
  @ParameterizedTest @ValueSource(strings={"0","-1","0.4"}) void discountMustRemainPositiveAfterCopRounding(String value) {
    assertThrows(HotelException.class,()->new Benefit(UUID.randomUUID(),1,"x",1,Benefit.Kind.RESERVATION_DISCOUNT,new BigDecimal(value),null));
  }
  @Test void noMixedTargetsOrMissingReferences() {
    assertThrows(HotelException.class,()->new Benefit(UUID.randomUUID(),1,"x",1,Benefit.Kind.SERVICE,null,null));
    assertThrows(HotelException.class,()->new Benefit(UUID.randomUUID(),1,"x",1,Benefit.Kind.PACKAGE,BigDecimal.ONE,UUID.randomUUID()));
    assertThrows(HotelException.class,()->new Benefit(UUID.randomUUID(),1,"x",1,Benefit.Kind.RESERVATION_DISCOUNT,BigDecimal.ONE,UUID.randomUUID()));
  }
  @Test void currencyRoundingIsHalfUpAtFinalDiscountBoundary() {
    var b=new Benefit(UUID.randomUUID(),1,"x",1,Benefit.Kind.RESERVATION_DISCOUNT,new BigDecimal("10.5"),null);assertEquals(new BigDecimal("11"),b.discount());
  }
  @Test void refundsMustBeExplicitlyConfigured() {
    assertThrows(HotelException.class,()->new LoyaltyPolicy("x",1,BigDecimal.ONE,1,null,LoyaltyPolicy.SpentTreatment.CARRY_FORWARD_DEBT,LoyaltyPolicy.RedemptionCancellation.KEEP_SPENT));
  }
}
