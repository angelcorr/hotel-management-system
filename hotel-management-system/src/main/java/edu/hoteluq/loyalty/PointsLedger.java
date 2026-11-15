package edu.hoteluq.loyalty;

import edu.hoteluq.config.HotelTime;
import edu.hoteluq.exceptions.HotelException;
import edu.hoteluq.model.BookingRecords.*;
import java.time.*;
import java.util.*;

/** Owned exclusively by the Hotel aggregate. All access shares its authorization and transaction monitor.
 * Immutable entries make checkpoints exact. Debt is reserved against live credits before spending;
 * it never prevents a cash refund, nor produces a negative spendable balance.
 */
public final class PointsLedger {
  public record Lot(UUID paymentId, UUID reservationId, String clientId, long earned, long entitlement,
      long used, Instant obtainedAt, Instant expiresAt, LoyaltyPolicy policy, boolean cancelled) {}
  public record Allocation(UUID paymentId, long points) {}
  public record Redemption(UUID id, String key, UUID reservationId, String clientId, long expectedVersion,
      Benefit benefit, LoyaltyPolicy policy, List<Allocation> allocations, Instant at, boolean restored) {
    public Redemption { allocations = List.copyOf(allocations); }
  }
  public record Event(UUID operationId, String clientId, String type, UUID sourceId, Instant effectiveAt, Instant recordedAt, long points) {}
  public record Account(String clientId, long available, long debt, List<Lot> lots, List<Redemption> redemptions, List<Event> history) {
    public Account { lots = List.copyOf(lots); redemptions = List.copyOf(redemptions); history = List.copyOf(history); }
  }
  public record Deferred(Payment payment, String clientId, boolean cancelled) {}
  private Map<UUID, Lot> lots = new LinkedHashMap<>();
  private Map<UUID, Redemption> redemptions = new LinkedHashMap<>();
  private Map<UUID, Deferred> deferred = new LinkedHashMap<>();
  private Set<UUID> refunds = new HashSet<>(), expired = new HashSet<>();
  private List<Event> history = new ArrayList<>();
  private Map<String, LoyaltyPolicy> policies = new LinkedHashMap<>();
  private LoyaltyPolicy active;
  private record Snapshot(Map<UUID,Lot> lots, Map<UUID,Redemption> redemptions, Map<UUID,Deferred> deferred,
      Set<UUID> refunds, Set<UUID> expired, List<Event> history) {}
  public Object checkpoint() { return new Snapshot(new LinkedHashMap<>(lots),new LinkedHashMap<>(redemptions),new LinkedHashMap<>(deferred),new HashSet<>(refunds),new HashSet<>(expired),new ArrayList<>(history)); }
  public void restore(Object value) {
    Snapshot s = (Snapshot)value; lots=s.lots; redemptions=s.redemptions; deferred=s.deferred;
    refunds=s.refunds; expired=s.expired; history=s.history;
  }
  public void configure(LoyaltyPolicy policy, String actor, Instant at) {
    Objects.requireNonNull(policy); String key=policy.id()+"/"+policy.version();
    if (policies.containsKey(key) && !policies.get(key).equals(policy)) throw new HotelException("Policy version is immutable");
    policies.put(key,policy); active=policy;
    event(actor,"POLICY_CONFIGURED",null,at,at,0);
  }
  public LoyaltyPolicy policy() { return active; }
  public static Instant expiry(Instant obtained) { return obtained.atZone(HotelTime.ZONE).plusYears(1).toInstant(); }
  private void event(String client, String type, UUID source, Instant effective, Instant observed, long points) {
    history.add(new Event(UUID.randomUUID(),client,type,source,effective,observed,points));
  }
  public void paid(String client, Payment payment) {
    if (lots.containsKey(payment.id()) || deferred.containsKey(payment.id())) return;
    if (active == null) {
      deferred.put(payment.id(),new Deferred(payment,client,false));
      event(client,"ACCRUAL_DEFERRED",payment.id(),payment.at(),payment.at(),0); return;
    }
    credit(client,payment,active,payment.at());
  }
  private void credit(String client, Payment payment, LoyaltyPolicy policy, Instant observed) {
    long earned=policy.points(payment.amount());
    lots.put(payment.id(),new Lot(payment.id(),payment.reservationId(),client,earned,earned,0,payment.at(),expiry(payment.at()),policy,false));
    event(client,"ACCRUED",payment.id(),payment.at(),observed,earned);
    account(client,observed); // Detect balance overflow before the aggregate commits money.
  }
  /** Explicit settlement of deferred accrual; original payment date and anniversary are preserved. */
  public void settle(UUID paymentId, Instant now) {
    if (active==null) throw new HotelException("Accrual policy not configured");
    if (lots.containsKey(paymentId)) return;
    Deferred pending=deferred.get(paymentId);
    if (pending==null || pending.cancelled()) throw new HotelException("No eligible deferred payment");
    credit(pending.clientId(),pending.payment(),active,now); deferred.remove(paymentId);
  }
  private List<Lot> ordered(String client, Instant now) {
    return lots.values().stream().filter(l -> l.clientId().equals(client) && !now.isBefore(l.obtainedAt()) && now.isBefore(l.expiresAt()))
        .sorted(Comparator.comparing(Lot::expiresAt).thenComparing(Lot::obtainedAt).thenComparing(l -> l.paymentId().toString())).toList();
  }
  private long grossDebt(String client) {
    long debt=0;
    for (Lot l:lots.values()) if (l.clientId().equals(client)) debt=Math.addExact(debt,Math.max(0,l.used()-l.entitlement()));
    return debt;
  }
  private Map<UUID,Long> spendable(String client, Instant now) {
    long debt=grossDebt(client); Map<UUID,Long> result=new LinkedHashMap<>();
    for (Lot l:ordered(client,now)) {
      long remaining=Math.max(0,l.entitlement()-l.used()); long held=Math.min(debt,remaining);
      debt-=held; result.put(l.paymentId(),remaining-held);
    }
    return result;
  }
  public Account account(String client, Instant now) {
    for (Lot l:lots.values()) if (l.clientId().equals(client) && !now.isBefore(l.expiresAt()) && expired.add(l.paymentId())) {
      event(client,"EXPIRED",l.paymentId(),l.expiresAt(),now,Math.max(0,l.entitlement()-l.used()));
    }
    return snapshotAccount(client,now);
  }
  /** Read-only report projection: expiry observations must not mutate the operational ledger. */
  public Account snapshotAccount(String client, Instant now) {
    long available=0,liveSurplus=0;
    for (long value:spendable(client,now).values()) available=Math.addExact(available,value);
    for (Lot l:ordered(client,now)) liveSurplus=Math.addExact(liveSurplus,Math.max(0,l.entitlement()-l.used()));
    return new Account(client,available,Math.max(0,grossDebt(client)-liveSurplus),
        lots.values().stream().filter(l -> l.clientId().equals(client)).toList(),
        redemptions.values().stream().filter(r -> r.clientId().equals(client)).toList(),
        history.stream().filter(e -> e.clientId().equals(client)).toList());
  }
  public Redemption redemption(UUID reservation) { return redemptions.get(reservation); }
  public Redemption redeem(String client, UUID reservation, long version, String key, Benefit benefit, Instant now) {
    if (key==null || key.isBlank()) throw new HotelException("Redemption key required");
    Redemption previous=redemptions.get(reservation);
    if (previous!=null) {
      if (!previous.key().equals(key) || !previous.benefit().id().equals(benefit.id()) || previous.expectedVersion()!=version) throw new HotelException("Conflicting redemption retry");
      return previous;
    }
    if (active==null) throw new HotelException("Loyalty and refund configuration required");
    if (redemptions.values().stream().anyMatch(r -> r.clientId().equals(client) && r.key().equals(key))) throw new HotelException("Redemption key already used");
    if (account(client,now).available()<benefit.cost()) throw new HotelException("Insufficient non-expired points");
    long needed=benefit.cost(); List<Allocation> allocations=new ArrayList<>();
    for (var entry:spendable(client,now).entrySet()) {
      long used=Math.min(needed,entry.getValue()); if (used==0) continue;
      Lot l=lots.get(entry.getKey()); lots.put(l.paymentId(),used(l,Math.addExact(l.used(),used)));
      allocations.add(new Allocation(l.paymentId(),used)); needed-=used; if (needed==0) break;
    }
    Redemption r=new Redemption(UUID.randomUUID(),key,reservation,client,version,benefit,active,allocations,now,false);
    redemptions.put(reservation,r); event(client,"REDEEMED",r.id(),now,now,benefit.cost()); return r;
  }
  private Lot used(Lot l,long used) { return new Lot(l.paymentId(),l.reservationId(),l.clientId(),l.earned(),l.entitlement(),used,l.obtainedAt(),l.expiresAt(),l.policy(),l.cancelled()); }
  public void refunded(String client, Refund refund, Payment payment) {
    if (!refunds.add(refund.id())) return;
    if (payment!=null) {
      Deferred d=deferred.get(payment.id());
      if (d!=null) deferred.put(payment.id(),new Deferred(d.payment(),client,true));
      Lot l=lots.get(payment.id());
      if (l!=null && !l.cancelled()) {
        long entitlement=l.policy().refundTreatment()==LoyaltyPolicy.RefundTreatment.REVOKE_ALL_ON_CANCELLATION ? 0 : l.policy().points(payment.amount().subtract(refund.amount()));
        lots.put(l.paymentId(),new Lot(l.paymentId(),l.reservationId(),l.clientId(),l.earned(),entitlement,l.used(),l.obtainedAt(),l.expiresAt(),l.policy(),true));
        event(client,"REFUND_ADJUSTMENT",refund.id(),refund.at(),refund.at(),l.entitlement()-entitlement);
      }
    }
    Redemption r=redemptions.get(refund.reservationId());
    if (r!=null && !r.restored() && r.policy().redemptionCancellation()==LoyaltyPolicy.RedemptionCancellation.RESTORE_ORIGINAL_EXPIRY) {
      for (Allocation a:r.allocations()) {
        Lot l=lots.get(a.paymentId()); lots.put(l.paymentId(),used(l,Math.subtractExact(l.used(),a.points())));
      }
      redemptions.put(r.reservationId(),new Redemption(r.id(),r.key(),r.reservationId(),r.clientId(),r.expectedVersion(),r.benefit(),r.policy(),r.allocations(),r.at(),true));
      event(client,"REDEMPTION_RESTORED",refund.id(),refund.at(),refund.at(),r.benefit().cost());
    }
    account(client,refund.at());
  }
  public List<Deferred> deferred(String client) { return deferred.values().stream().filter(d -> d.clientId().equals(client)).toList(); }
}
