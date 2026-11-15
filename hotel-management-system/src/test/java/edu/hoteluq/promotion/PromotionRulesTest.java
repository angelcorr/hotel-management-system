package edu.hoteluq.promotion;

import edu.hoteluq.enums.RoomCategory;
import edu.hoteluq.exceptions.HotelException;
import edu.hoteluq.money.CopMoney;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static edu.hoteluq.promotion.PromotionDefinition.*;

class PromotionRulesTest {
  private final LocalDate day=LocalDate.of(2027,1,10);
  private final Instant at=Instant.parse("2026-11-13T17:00:00Z");
  private PromotionCondition.Context context(RoomCategory type,long nights,long completed,Set<UUID> services,Set<UUID> packages) {
    return new PromotionCondition.Context(day,day.plusDays(nights),type,services,packages,completed);
  }
  private Settings settings(String percent,List<PromotionCondition> conditions) {
    return new Settings("Fixture","Explicit test settings",new BigDecimal(percent),true,at,at.plusSeconds(10),conditions,Combination.EXCLUSIVE,PointsAccrual.EARN_ON_NET_PAYMENT);
  }
  @ParameterizedTest @ValueSource(strings={"-0.01","100.01","-100"}) void invalidDiscounts(String value) {
    assertThrows(HotelException.class,()->settings(value,List.of(new PromotionCondition.MinimumNights(1))));
  }
  @ParameterizedTest @ValueSource(longs={0,-1}) void invalidThresholds(long threshold) {
    assertThrows(HotelException.class,()->new PromotionCondition.MinimumNights(threshold));assertThrows(HotelException.class,()->new PromotionCondition.FrequentClient(threshold));
  }
  @Test void explicitConfigurationRequired() {
    assertThrows(HotelException.class,()->settings("10",List.of()));assertThrows(HotelException.class,()->new PromotionCondition.RoomTypes(Set.of()));
    assertThrows(HotelException.class,()->new Settings("x","x",BigDecimal.ONE,true,null,null,List.of(new PromotionCondition.MinimumNights(1)),null,PointsAccrual.EXCLUDED));
    assertThrows(HotelException.class,()->new Settings("x","x",BigDecimal.ONE,true,null,null,List.of(new PromotionCondition.MinimumNights(1)),Combination.EXCLUSIVE,null));
  }
  @Test void incompleteOrReversedValidityAndStayWindowsAreRejected() {
    var c=List.<PromotionCondition>of(new PromotionCondition.MinimumNights(1));
    assertThrows(HotelException.class,()->new Settings("x","x",BigDecimal.ONE,true,at,null,c,Combination.EXCLUSIVE,PointsAccrual.EXCLUDED));
    assertThrows(HotelException.class,()->new Settings("x","x",BigDecimal.ONE,true,at,at,c,Combination.EXCLUSIVE,PointsAccrual.EXCLUDED));
    assertThrows(HotelException.class,()->new PromotionCondition.StayWindow(day,day));
  }
  @Test void contradictoryDuplicateConditionFamiliesAreRejected() {
    assertThrows(HotelException.class,()->settings("10",List.of(new PromotionCondition.MinimumNights(1),new PromotionCondition.MinimumNights(2))));
    assertThrows(HotelException.class,()->settings("10",List.of(new PromotionCondition.All(List.of(new PromotionCondition.MinimumNights(1))))));
  }
  @Test void allOfComposesSpecificationsWithoutChangingPredicates() {
    var combined=new PromotionCondition.RoomTypes(Set.of(RoomCategory.STANDARD)).and(new PromotionCondition.MinimumNights(3));
    assertTrue(combined.satisfiedBy(context(RoomCategory.STANDARD,3,0,Set.of(),Set.of())));
    assertFalse(combined.satisfiedBy(context(RoomCategory.SUITE,3,0,Set.of(),Set.of())));
    assertFalse(combined.satisfiedBy(context(RoomCategory.STANDARD,2,0,Set.of(),Set.of())));
  }
  @Test void stayWindowCoversWholeStayAndCheckoutIsExclusive() {
    var condition=new PromotionCondition.StayWindow(day,day.plusDays(3));
    assertTrue(condition.satisfiedBy(context(RoomCategory.STANDARD,3,0,Set.of(),Set.of())));assertFalse(condition.satisfiedBy(context(RoomCategory.STANDARD,4,0,Set.of(),Set.of())));
  }
  @Test void frequentThresholdIsExplicit() {
    var s=new PromotionCondition.FrequentClient(2);assertTrue(s.satisfiedBy(context(RoomCategory.STANDARD,2,2,Set.of(),Set.of())));assertFalse(s.satisfiedBy(context(RoomCategory.STANDARD,2,1,Set.of(),Set.of())));
  }
  @Test void serviceAndPackageSetsMustAllBePresent() {
    var a=UUID.randomUUID();var b=UUID.randomUUID();var s=new PromotionCondition.Services(Set.of(a,b));var p=new PromotionCondition.Packages(Set.of(a));
    assertFalse(s.satisfiedBy(context(RoomCategory.STANDARD,2,0,Set.of(a),Set.of(a))));assertTrue(s.satisfiedBy(context(RoomCategory.STANDARD,2,0,Set.of(a,b),Set.of(a))));assertFalse(p.satisfiedBy(context(RoomCategory.STANDARD,2,0,Set.of(a,b),Set.of())));
  }
  @ParameterizedTest @CsvSource({"-1,false","0,true","9,true","10,false","11,false"})
  void operationValidityUsesHalfOpenInstants(long seconds,boolean valid) {
    var d=new PromotionDefinition(UUID.randomUUID(),1,settings("10",List.of(new PromotionCondition.MinimumNights(1))));
    if(valid) assertDoesNotThrow(()->d.requireEligible(context(RoomCategory.STANDARD,1,0,Set.of(),Set.of()),at.plusSeconds(seconds)));
    else assertThrows(HotelException.class,()->d.requireEligible(context(RoomCategory.STANDARD,1,0,Set.of(),Set.of()),at.plusSeconds(seconds)));
  }
  @Test void inactivePromotionCannotBeApplied() {
    var d=new PromotionDefinition(UUID.randomUUID(),1,settings("10",List.of(new PromotionCondition.MinimumNights(1))).withActive(false));assertThrows(HotelException.class,()->d.requireEligible(context(RoomCategory.STANDARD,1,0,Set.of(),Set.of()),at));
  }
  @ParameterizedTest @CsvSource({"PROMOTION_THEN_POINTS,160","POINTS_THEN_PROMOTION,162"})
  void explicitDecoratorOrderAffectsCash(Combination order,String expected) {
    assertEquals(0,new BigDecimal(expected).compareTo(PriceComponent.net(new BigDecimal("200"),new BigDecimal("10"),new BigDecimal("20"),order)));
  }
  @Test void undefinedOrExclusiveCombinationIsRejected() {
    assertThrows(HotelException.class,()->PriceComponent.net(new BigDecimal("200"),BigDecimal.TEN,BigDecimal.ONE,null));assertThrows(HotelException.class,()->PriceComponent.net(new BigDecimal("200"),BigDecimal.TEN,BigDecimal.ONE,Combination.EXCLUSIVE));
  }
  @Test void roundingHappensOnlyOnFinalObligation() {
    var raw=new PriceComponent.Percent(new PriceComponent.Base(new BigDecimal("200.5")),new BigDecimal("10")).amount();
    assertEquals(0,new BigDecimal("180.45").compareTo(raw));assertEquals(new BigDecimal("180"),CopMoney.positive(raw));assertThrows(HotelException.class,()->CopMoney.positive(new PriceComponent.Percent(new PriceComponent.Base(BigDecimal.TEN),new BigDecimal("100")).amount()));
  }
}
