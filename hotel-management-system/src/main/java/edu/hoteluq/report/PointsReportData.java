package edu.hoteluq.report;

import edu.hoteluq.loyalty.PointsLedger.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/** Read-only expiry reconstruction. Lazy operational EXPIRED observations are not historical balances. */
final class PointsReportData {
  record Movement(String id,String client,String type,String source,Instant at,BigDecimal amount){}
  private record RefundOrigin(String reservation,UUID payment){}
  static List<Movement> movements(ReportSnapshot snapshot){
    Map<UUID,RefundOrigin> refunds=new HashMap<>();
    for(var b:snapshot.bookings())if(b.refund()!=null)refunds.put(b.refund().id(),new RefundOrigin(b.id().toString(),b.refund().paymentId()));
    List<Movement> result=new ArrayList<>();
    for(Account account:snapshot.points().stream().sorted(Comparator.comparing(Account::clientId)).toList()){
      Map<UUID,Redemption> byId=new HashMap<>();Map<String,Redemption> byReservation=new HashMap<>();
      for(var r:account.redemptions()){byId.put(r.id(),r);byReservation.put(r.reservationId().toString(),r);}
      for(Event e:account.history())if(!e.type().equals("EXPIRED"))result.add(new Movement(e.operationId().toString(),account.clientId(),e.type(),Objects.toString(e.sourceId(),"—"),e.effectiveAt(),BigDecimal.valueOf(e.points())));
      for(Lot lot:account.lots()){
        if(lot.expiresAt().isAfter(snapshot.at()))continue;
        long entitlement=lot.earned(),used=0;
        for(Event e:account.history())if(e.effectiveAt().isBefore(lot.expiresAt())){
          long[] change=change(e,lot,byId,byReservation,refunds);entitlement=Math.subtractExact(entitlement,change[0]);used=Math.addExact(used,change[1]);
        }
        long expired=Math.max(0,entitlement-used);
        if(expired>0)result.add(new Movement(lot.paymentId()+":expiry",account.clientId(),"EXPIRED_CALENDAR",lot.paymentId().toString(),lot.expiresAt(),BigDecimal.valueOf(expired)));
        // Restoring an already expired allocation cannot revive it. Count only the newly expired entitlement,
        // not revoked points/debt. Replay list order preserves causal order even for equal timestamps.
        entitlement=lot.earned();used=0;
        for(Event e:account.history()){
          long[] delta=change(e,lot,byId,byReservation,refunds);
          if(e.type().equals("REDEMPTION_RESTORED") && !e.effectiveAt().isBefore(lot.expiresAt())){
            long before=Math.max(0,entitlement-used);long after=Math.max(0,entitlement-(used+delta[1]));long newlyExpired=after-before;
            if(newlyExpired>0)result.add(new Movement(e.operationId()+":"+lot.paymentId(),account.clientId(),"RESTORED_EXPIRED",lot.paymentId().toString(),e.effectiveAt(),BigDecimal.valueOf(newlyExpired)));
          }
          entitlement=Math.subtractExact(entitlement,delta[0]);used=Math.addExact(used,delta[1]);
        }
      }
    }
    result.sort(Comparator.comparing(Movement::at).thenComparing(Movement::client));return List.copyOf(result);
  }
  private static long[] change(Event event,Lot lot,Map<UUID,Redemption> byId,Map<String,Redemption> byReservation,Map<UUID,RefundOrigin> refunds){
    if(event.type().equals("REFUND_ADJUSTMENT")){
      var source=refunds.get(event.sourceId());return new long[]{source!=null && lot.paymentId().equals(source.payment())?event.points():0,0};
    }
    Redemption redemption=null;int sign=1;
    if(event.type().equals("REDEEMED"))redemption=byId.get(event.sourceId());
    if(event.type().equals("REDEMPTION_RESTORED")){
      var source=refunds.get(event.sourceId());if(source!=null)redemption=byReservation.get(source.reservation());sign=-1;
    }
    long used=0;if(redemption!=null)for(var allocation:redemption.allocations())if(allocation.paymentId().equals(lot.paymentId()))used=Math.addExact(used,allocation.points());
    return new long[]{0,sign*used};
  }
}
