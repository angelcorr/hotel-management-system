package edu.hoteluq.report;

import edu.hoteluq.enums.*;
import edu.hoteluq.loyalty.*;
import edu.hoteluq.exceptions.HotelException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static edu.hoteluq.report.ReportQuery.Kind.*;

class ReportEngineTest {
  private final LocalDate day=LocalDate.of(2026,11,13);
  private final Instant at=Instant.parse("2026-11-13T17:00:00Z");
  private final UUID s=uuid(30);
  static UUID uuid(int i){return UUID.fromString("00000000-0000-0000-0000-"+String.format("%012d",i));}
  private BigDecimal n(String value){return new BigDecimal(value);}
  private ReportSnapshot.Booking booking(int id,int room,ReservationStatus status,Instant created,ReportSnapshot.Payment pay,ReportSnapshot.Refund refund){
    return new ReportSnapshot.Booking(uuid(id),"c1",room,RoomCategory.STANDARD,"Estándar",status,day.plusDays(1),day.plusDays(3),created,n(room==1?"80":"50"),room==1?List.of(new ReportSnapshot.Service(s,"Desayuno",n("20"))):List.of(),Set.of(),pay,refund);
  }
  private ReportSnapshot snapshot(){
    var one=booking(1,1,ReservationStatus.CONFIRMED,at,new ReportSnapshot.Payment(uuid(11),n("100"),at),null);
    var two=booking(2,2,ReservationStatus.PENDING,at,null,null);
    var three=booking(3,3,ReservationStatus.CANCELLED,at.minusSeconds(86400),new ReportSnapshot.Payment(uuid(13),n("50"),at.minusSeconds(86400)),new ReportSnapshot.Refund(uuid(23),uuid(13),n("40"),n("80"),false,at));
    return new ReportSnapshot(at,List.of(new ReportSnapshot.Room(1,RoomCategory.STANDARD,true),new ReportSnapshot.Room(2,RoomCategory.STANDARD,true),new ReportSnapshot.Room(3,RoomCategory.STANDARD,false)),List.of(three,two,one),List.of(new ReportSnapshot.Recharge(uuid(41),"c1",n("1000"),at)),List.of());
  }
  private ReportResult run(ReportQuery.Kind kind){return new ReportEngine().generate(snapshot(),new ReportQuery(kind,day,day));}
  @ParameterizedTest @EnumSource(ReportQuery.Kind.class) void everyFamilyHandlesEmptyData(ReportQuery.Kind kind){
    var r=new ReportEngine().generate(new ReportSnapshot(at,List.of(),List.of(),List.of(),List.of()),new ReportQuery(kind,day,day));assertFalse(r.sections().isEmpty());assertEquals(kind,r.query().kind());if(kind==OCCUPANCY)assertNull(r.total("occupancyPercent"));else assertTrue(r.sections().getFirst().rows().isEmpty());
  }
  @Test void reservationsUseCreationDateAndCurrentStateWithExplicitComparison(){
    var r=new ReportEngine().generate(snapshot(),new ReportQuery(RESERVATIONS,day,day,null,null,null,null,null,true));assertEquals(n("2"),r.total("reservations"));assertEquals(n("1"),r.total("previous"));assertEquals(n("1"),r.total("difference"));assertEquals(2,r.sections().getFirst().rows().size());
  }
  @Test void rankingTiesAreStableAndUseCreationCohort(){var r=run(ROOM_RANKING);assertEquals("1",r.sections().getFirst().rows().getFirst().cells().get(1).display());assertEquals("2",r.sections().getFirst().rows().getLast().cells().get(1).display());}
  @Test void occupancyClipsNightsAndDistinguishesPendingFromOccupied(){
    var r=new ReportEngine().generate(snapshot(),new ReportQuery(OCCUPANCY,day.plusDays(1),day.plusDays(3)));assertEquals(n("6"),r.total("capacityNights"));assertEquals(n("2"),r.total("occupiedNights"));assertEquals(n("2"),r.total("pendingNights"));assertEquals(n("2"),r.total("availableNights"));assertEquals(n("33.33"),r.total("occupancyPercent"));assertEquals("0",r.sections().getFirst().rows().getLast().cells().get(2).display());
  }
  @Test void committedInactiveRoomIsIncludedInNightCapacity(){
    var b=booking(1,1,ReservationStatus.CONFIRMED,at,new ReportSnapshot.Payment(uuid(11),n("100"),at),null);
    var data=new ReportSnapshot(at,List.of(new ReportSnapshot.Room(1,RoomCategory.STANDARD,false)),List.of(b),List.of(),List.of());var r=new ReportEngine().generate(data,new ReportQuery(OCCUPANCY,day.plusDays(1),day.plusDays(1)));assertEquals(n("100.00"),r.total("occupancyPercent"));assertEquals(n("1"),r.total("capacityNights"));
  }
  @Test void revenueExcludesRechargesAndIncludesRefundForPaymentOutsideRange(){var r=run(REVENUE);assertEquals(n("100"),r.total("payments"));assertEquals(n("80"),r.total("roomRevenue"));assertEquals(n("20"),r.total("serviceRevenue"));assertEquals(n("40"),r.total("refunds"));assertEquals(n("60"),r.total("net"));}
  @Test void serviceQuantitiesUsePaidContractAndAllocatedNetAmounts(){var r=run(SERVICES);assertEquals(n("1"),r.total("quantity"));assertEquals(n("20"),r.total("revenue"));assertEquals(n("20"),r.total("raw"));}
  @Test void rechargeHasItsOwnTotalAndDuplicateIdCannotMultiplyIt(){var original=snapshot();var duplicate=new ReportSnapshot(at,original.rooms(),original.bookings(),List.of(original.recharges().getFirst(),original.recharges().getFirst()),List.of());var r=new ReportEngine().generate(duplicate,new ReportQuery(RECHARGES,day,day));assertEquals(n("1"),r.total("count"));assertEquals(n("1000"),r.total("recharged"));}
  @Test void cancellationsIncludeZeroWithoutPretendingItIsMoney(){var data=snapshot();var unpaid=booking(4,2,ReservationStatus.CANCELLED,at,null,new ReportSnapshot.Refund(uuid(24),null,BigDecimal.ZERO,n("100"),true,at));var list=new ArrayList<>(data.bookings());list.add(unpaid);var r=new ReportEngine().generate(new ReportSnapshot(at,data.rooms(),list,data.recharges(),List.of()),new ReportQuery(CANCELLATIONS,day,day));assertEquals(n("2"),r.total("count"));assertEquals(n("1"),r.total("hotel"));assertEquals(n("1"),r.total("client"));assertEquals(n("40"),r.total("refunds"));}
  @Test void filtersApplyToBookingFamiliesAndNeverInventClients(){var r=new ReportEngine().generate(snapshot(),new ReportQuery(REVENUE,day,day,"other",null,null,null,null,false));assertEquals(BigDecimal.ZERO,r.total("payments"));assertTrue(r.sections().getFirst().rows().isEmpty());}
  @Test void statusAndRoomFiltersAreExplicit(){var r=new ReportEngine().generate(snapshot(),new ReportQuery(RESERVATIONS,day,day,"c1",RoomCategory.STANDARD,2,ReservationStatus.PENDING,null,false));assertEquals(n("1"),r.total("reservations"));}
  @Test void paymentAllocationReconcilesDiscountsWithNoDecimalDrift(){var weights=new LinkedHashMap<String,BigDecimal>();weights.put("a",n("0.1"));weights.put("b",n("0.1"));weights.put("c",n("0.1"));var r=MoneyAllocation.allocate(BigDecimal.ONE,weights);assertEquals(BigDecimal.ONE,r.get("a"));assertEquals(BigDecimal.ZERO,r.get("b"));assertEquals(BigDecimal.ONE,r.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add));}
  @Test void freeServiceReceivesNoCashAttribution(){var b=new ReportSnapshot.Booking(uuid(1),"c1",1,RoomCategory.STANDARD,"Standard",ReservationStatus.CONFIRMED,day,day.plusDays(1),at,n("100"),List.of(new ReportSnapshot.Service(s,"Gift",n("50"))),Set.of(s),new ReportSnapshot.Payment(uuid(11),n("90"),at),null);var allocation=MoneyAllocation.payment(b);assertEquals(n("90"),allocation.get("ROOM"));assertEquals(BigDecimal.ZERO,allocation.get(s.toString()));}
  @Test void identicalSnapshotAndQueryProduceIdenticalResultsAndTxt(){var query=new ReportQuery(REVENUE,day,day);var a=new ReportEngine().generate(snapshot(),query);var b=new ReportEngine().generate(snapshot(),query);assertEquals(a,b);assertArrayEquals(new TxtReportExporter().render(a),new TxtReportExporter().render(b));}
  @Test void queryRejectsIncompleteReversedAndUnsupportedFilters(){assertThrows(HotelException.class,()->new ReportQuery(REVENUE,null,day));assertThrows(HotelException.class,()->new ReportQuery(REVENUE,day.plusDays(1),day));assertThrows(HotelException.class,()->new ReportQuery(RECHARGES,day,day,null,RoomCategory.STANDARD,null,null,null,false));assertThrows(HotelException.class,()->new ReportQuery(REVENUE,day,day,null,null,null,null,null,true));assertThrows(HotelException.class,()->new ReportQuery(REVENUE,day,day,null,null,null,null,s,false));}
  @Test void dateBoundariesUseBogotaRegardlessOfServerZone(){var q=new ReportQuery(REVENUE,day,day);assertEquals(Instant.parse("2026-11-13T05:00:00Z"),q.start());assertTrue(q.contains(Instant.parse("2026-11-14T04:59:59.999999999Z")));assertFalse(q.contains(Instant.parse("2026-11-14T05:00:00Z")));assertFalse(q.contains(Instant.parse("2026-11-13T04:59:59Z")));}
  @Test void pointExpiryIsReconstructedRatherThanTrustingLazyZeroObservation(){
    var lot=new PointsLedger.Lot(uuid(51),uuid(50),"c1",100,100,40,at.minus(Duration.ofDays(365)),at,policy(),false);
    var redemption=new PointsLedger.Redemption(uuid(60),"key",uuid(61),"c1",0,new Benefit(uuid(62),1,"Fixture",40,Benefit.Kind.RESERVATION_DISCOUNT,n("10"),null),policy(),List.of(new PointsLedger.Allocation(lot.paymentId(),40)),at.minusSeconds(86400),false);
    var events=List.of(new PointsLedger.Event(uuid(70),"c1","ACCRUED",lot.paymentId(),lot.obtainedAt(),lot.obtainedAt(),100),new PointsLedger.Event(uuid(71),"c1","REDEEMED",redemption.id(),redemption.at(),redemption.at(),40),new PointsLedger.Event(uuid(72),"c1","EXPIRED",lot.paymentId(),at,at,0));
    var account=new PointsLedger.Account("c1",0,0,List.of(lot),List.of(redemption),events);var snap=new ReportSnapshot(at,List.of(),List.of(),List.of(),List.of(account));var r=new ReportEngine().generate(snap,new ReportQuery(POINTS,day.minusYears(1),day));assertEquals(n("100"),r.total("earned"));assertEquals(n("40"),r.total("redeemed"));assertEquals(n("60"),r.total("expired"));assertEquals(BigDecimal.ZERO,r.total("available"));assertEquals(3,account.history().size());
  }
  private LoyaltyPolicy policy(){return new LoyaltyPolicy("FIXTURE",1,BigDecimal.ONE,1,LoyaltyPolicy.RefundTreatment.RECALCULATE_RETAINED_PAYMENT,LoyaltyPolicy.SpentTreatment.CARRY_FORWARD_DEBT,LoyaltyPolicy.RedemptionCancellation.RESTORE_ORIGINAL_EXPIRY);}
  @Test void serviceRefundInAnotherPeriodDoesNotCreateAnotherPurchase(){
    var original=booking(1,1,ReservationStatus.CANCELLED,at,new ReportSnapshot.Payment(uuid(11),n("100"),at),new ReportSnapshot.Refund(uuid(21),uuid(11),n("100"),n("100"),true,at.plusSeconds(86400)));
    var snap=new ReportSnapshot(at.plusSeconds(86400),snapshot().rooms(),List.of(original),List.of(),List.of());var r=new ReportEngine().generate(snap,new ReportQuery(SERVICES,day.plusDays(1),day.plusDays(1)));assertEquals(BigDecimal.ZERO,r.total("quantity"));assertEquals(BigDecimal.ZERO,r.total("revenue"));assertEquals(n("20"),r.total("refunds"));assertEquals(n("-20"),r.total("net"));
  }
  @Test void utf8TxtEscapesUntrustedLineBreaksAndKeepsTheSameColumns(){
    var report=new ReportResult(new ReportQuery(SERVICES,day,day),at,List.of("Criterion"),List.of(new ReportResult.Section("Servicios",List.of("Nombre"),List.of(ReportResult.row("Spa\nTOTAL\tunsafe")))),List.of(),List.of());String text=new String(new TxtReportExporter().render(report),java.nio.charset.StandardCharsets.UTF_8);assertTrue(text.contains("Spa\\nTOTAL\\tunsafe"));assertFalse(text.contains("Spa\nTOTAL"));
  }
  @Test void rowInstantsShowOfficialZoneWithoutChangingTheInstant(){assertEquals("2026-11-13T12:00:00-05:00",ReportResult.row(at).cells().getFirst().display());}
  @Test void invalidAllocationCannotInventCashWithoutAWeight(){assertThrows(HotelException.class,()->MoneyAllocation.allocate(BigDecimal.ONE,Map.of("a",BigDecimal.ZERO)));assertThrows(HotelException.class,()->MoneyAllocation.allocate(n("0.5"),Map.of("a",BigDecimal.ONE)));}
  @ParameterizedTest @ValueSource(booleans={false,true})
  void restorationAfterExpiryCountsOnlyNonRevokedEntitlement(boolean revokeSource){
    Instant restoredAt=at.plusSeconds(86400);UUID original=uuid(51),redemptionReservation=uuid(61),refundId=uuid(81),sourceRefundId=uuid(82);
    var lot=new PointsLedger.Lot(original,uuid(50),"c1",100,revokeSource?0:100,0,at.minus(Duration.ofDays(365)),at,policy(),revokeSource);
    var redemption=new PointsLedger.Redemption(uuid(60),"key",redemptionReservation,"c1",0,new Benefit(uuid(62),1,"Fixture",40,Benefit.Kind.RESERVATION_DISCOUNT,n("10"),null),policy(),List.of(new PointsLedger.Allocation(original,40)),at.minusSeconds(86400),true);
    var events=new ArrayList<PointsLedger.Event>();events.add(new PointsLedger.Event(uuid(70),"c1","ACCRUED",original,lot.obtainedAt(),lot.obtainedAt(),100));events.add(new PointsLedger.Event(uuid(71),"c1","REDEEMED",redemption.id(),redemption.at(),redemption.at(),40));
    var spending=new ReportSnapshot.Booking(redemptionReservation,"c1",1,RoomCategory.STANDARD,"Standard",ReservationStatus.CANCELLED,day,day.plusDays(1),at,n("10"),List.of(),Set.of(),new ReportSnapshot.Payment(uuid(91),n("10"),at),new ReportSnapshot.Refund(refundId,uuid(91),n("10"),n("100"),true,restoredAt));
    List<ReportSnapshot.Booking> bookings=new ArrayList<>(List.of(spending));
    if(revokeSource){events.add(new PointsLedger.Event(uuid(73),"c1","REFUND_ADJUSTMENT",sourceRefundId,at.plusSeconds(1),at.plusSeconds(1),100));bookings.add(new ReportSnapshot.Booking(uuid(50),"c1",2,RoomCategory.STANDARD,"Standard",ReservationStatus.CANCELLED,day,day.plusDays(1),lot.obtainedAt(),n("100"),List.of(),Set.of(),new ReportSnapshot.Payment(original,n("100"),lot.obtainedAt()),new ReportSnapshot.Refund(sourceRefundId,original,n("100"),n("100"),true,at.plusSeconds(1))));}
    events.add(new PointsLedger.Event(uuid(74),"c1","REDEMPTION_RESTORED",refundId,restoredAt,restoredAt,40));var account=new PointsLedger.Account("c1",0,0,List.of(lot),List.of(redemption),events);var snap=new ReportSnapshot(restoredAt,List.of(),bookings,List.of(),List.of(account));
    var r=new ReportEngine().generate(snap,new ReportQuery(POINTS,day,day.plusDays(1)));assertEquals(n(revokeSource?"60":"100"),r.total("expired"));assertEquals(BigDecimal.ZERO,r.total("available"));
  }

}
