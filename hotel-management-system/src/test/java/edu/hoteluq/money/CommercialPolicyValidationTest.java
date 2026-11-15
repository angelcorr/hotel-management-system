package edu.hoteluq.money;

import edu.hoteluq.enums.*;
import edu.hoteluq.exceptions.*;
import edu.hoteluq.model.*;
import edu.hoteluq.strategy.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CommercialPolicyValidationTest {
  BigDecimal bd(String value) { return new BigDecimal(value); }
  CancellationSchedule.Window window(Duration min,Duration max,String percent) { return new CancellationSchedule.Window(min,max,bd(percent)); }
  @ParameterizedTest @CsvSource({"1,1","1.49,1","1.5,2","2.5,3","0.5,1","123.456789,123"})
  void finalCopUsesHalfUpOnce(String input,String expected) { assertEquals(bd(expected),CopMoney.positive(bd(input))); }
  @ParameterizedTest @ValueSource(strings={"0","0.49","-0.1","-1"})
  void zeroAfterRoundingAndNegativeFinalsAreRejected(String input) { assertThrows(HotelException.class,() -> CopMoney.positive(bd(input))); }
  @ParameterizedTest @ValueSource(strings={"-1","100.00001","101"})
  void invalidRefundPercentagesAreRejected(String percent) { assertThrows(HotelException.class,() -> window(null,null,percent)); }
  @ParameterizedTest @ValueSource(strings={"0","100"})
  void constantSchedulesCannotReplaceOfficialRules(String percent) {
    var candidate=new CancellationSchedule("nonofficial",1,List.of(window(null,null,percent)));
    assertThrows(HotelException.class,candidate::requireOfficial);
  }
  @Test void missingContradictoryUnsortedOrIncompletePolicyFailsBeforeActivation() {
    assertThrows(HotelException.class,() -> new CancellationSchedule("p",1,List.of()));
    assertThrows(HotelException.class,() -> new CancellationSchedule(" ",1,List.of(window(null,null,"50"))));
    assertThrows(HotelException.class,() -> new CancellationSchedule("p",0,List.of(window(null,null,"50"))));
    assertThrows(HotelException.class,() -> new CancellationSchedule("p",1,List.of(window(Duration.ZERO,null,"50"))));
    assertThrows(HotelException.class,() -> new CancellationSchedule("p",1,List.of(window(null,Duration.ZERO,"50"))));
    assertThrows(HotelException.class,() -> new CancellationSchedule("p",1,List.of(window(null,Duration.ZERO,"50"),window(Duration.ofSeconds(1),null,"80"))));
    assertThrows(HotelException.class,() -> new CancellationSchedule("p",1,List.of(window(null,Duration.ofSeconds(1),"50"),window(Duration.ZERO,null,"80"))));
    assertThrows(HotelException.class,() -> window(Duration.ZERO,Duration.ZERO,"50"));
    assertThrows(HotelException.class,() -> window(Duration.ofHours(2),Duration.ofHours(1),"50"));
    var nulls=new ArrayList<CancellationSchedule.Window>(); nulls.add(null);
    assertThrows(HotelException.class,() -> new CancellationSchedule("p",1,nulls));
  }
  @Test void exactNanosecondWindowBoundaryHasOneOwnerAndPolicyIsImmutable() {
    var source=new ArrayList<CancellationSchedule.Window>();
    source.add(window(null,Duration.ZERO,"25"));source.add(window(Duration.ZERO,null,"80"));
    var policy=new CancellationSchedule("p",1,source);source.clear();
    assertEquals(bd("25"),policy.select(Duration.ofNanos(-1)).calculateRefund(bd("100")));
    assertEquals(bd("80"),policy.select(Duration.ZERO).calculateRefund(bd("100")));
    assertThrows(UnsupportedOperationException.class,() -> policy.windows().clear());
  }
  Reservation.Builder builder(String nightly) {
    var type=new RoomType(RoomCategory.STANDARD,bd(nightly),"Standard",2,"Description");
    return new Reservation.Builder().client(new Client("c","Ana","c@example.com","secret")).room(new StandardRoom(101,1,type))
        .checkIn(LocalDate.of(2027,1,1)).checkOut(LocalDate.of(2027,1,2));
  }
  @Test void servicesAndPercentagePreserveRawPrecisionAndReconcileFinalAmounts() {
    var reservation=builder("1.25").additionalService(new AdditionalService("Spa",bd("0.25")))
        .promotion(new Promotion("Half",bd("50"))).build();
    var quote=reservation.getQuote();
    assertEquals(0,bd("1.50").compareTo(quote.rawSubtotal()));
    assertEquals(0,bd("0.75").compareTo(quote.rawDiscount()));
    assertEquals(bd("2"),quote.subtotal());assertEquals(bd("1"),quote.discount());assertEquals(bd("1"),quote.total());
    assertEquals(quote.total(),quote.subtotal().subtract(quote.discount()));
    assertEquals("COP",quote.currency());
  }
  @Test void zeroNegativeFullDiscountAndSubPesoDiscountNeverProduceAReservation() {
    assertThrows(HotelException.class,() -> builder("0").build());
    assertThrows(HotelException.class,() -> builder("-1").build());
    assertThrows(HotelException.class,() -> builder("100").promotion(new Promotion("Full",100)).build());
    assertThrows(HotelException.class,() -> builder("0.90").promotion(new Promotion("Half",50)).build());
    assertThrows(HotelException.class,() -> new Promotion("Invalid",bd("101")));
  }
  @Test void legacyNonFiniteInputsAndForeignCurrenciesRemainRejected() {
    assertThrows(HotelException.class,() -> CopMoney.input(Double.NaN));
    assertThrows(HotelException.class,() -> CopMoney.input(Double.POSITIVE_INFINITY));
    assertThrows(HotelException.class,() -> CopMoney.currency("USD"));
    assertThrows(HotelException.class,() -> CopMoney.currency(null));
    assertThrows(HotelException.class,() -> CopMoney.integral(bd("1.5")));
  }
}
