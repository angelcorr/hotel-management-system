package edu.hoteluq.report;

import edu.hoteluq.money.CopMoney;
import edu.hoteluq.exceptions.HotelException;
import java.math.*;
import java.util.*;

/** Analytical allocation, not a new tariff: largest remainders reconcile integer COP to the receipt. */
public final class MoneyAllocation {
  private MoneyAllocation(){}
  private record Remainder(String key,BigDecimal remainder){}
  public static Map<String,BigDecimal> allocate(BigDecimal amount,Map<String,BigDecimal> weights){
    amount=CopMoney.integral(amount);BigDecimal weight=BigDecimal.ZERO;
    for(var value:weights.values())weight=weight.add(CopMoney.input(value));
    if(amount.signum()>0 && weight.signum()==0)throw new HotelException("No hay base histórica para atribuir el pago");
    Map<String,BigDecimal> result=new TreeMap<>();List<Remainder> rest=new ArrayList<>();BigDecimal sum=BigDecimal.ZERO;
    for(var entry:new TreeMap<>(weights).entrySet()){
      BigDecimal product=amount.multiply(entry.getValue());BigDecimal part=weight.signum()==0?BigDecimal.ZERO:product.divide(weight,0,RoundingMode.DOWN);
      result.put(entry.getKey(),part);sum=sum.add(part);rest.add(new Remainder(entry.getKey(),product.subtract(part.multiply(weight))));
    }
    rest.sort(Comparator.comparing(Remainder::remainder).reversed().thenComparing(Remainder::key));
    int remaining=amount.subtract(sum).intValueExact();for(int i=0;i<remaining;i++)result.compute(rest.get(i).key(),(k,v)->v.add(BigDecimal.ONE));
    return Collections.unmodifiableMap(result);
  }
  public static Map<String,BigDecimal> payment(ReportSnapshot.Booking booking){
    Map<String,BigDecimal> weights=new TreeMap<>();weights.put("ROOM",booking.roomRaw());
    for(var s:booking.services())weights.put(s.id().toString(),booking.waivedServices().contains(s.id())?BigDecimal.ZERO:s.rawPrice());
    return allocate(booking.payment().amount(),weights);
  }
}
