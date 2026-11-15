package edu.hoteluq.report;

import edu.hoteluq.enums.*;
import edu.hoteluq.loyalty.PointsLedger;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Minimal read model copied atomically by the aggregate, never exposes mutable business entities. */
public record ReportSnapshot(Instant at,List<Room> rooms,List<Booking> bookings,List<Recharge> recharges,List<PointsLedger.Account> points) {
  public record Room(int number,RoomCategory category,boolean active){}
  public record Service(UUID id,String name,BigDecimal rawPrice){}
  public record Payment(UUID id,BigDecimal amount,Instant at){}
  public record Refund(UUID id,UUID paymentId,BigDecimal amount,BigDecimal percent,boolean byHotel,Instant at){}
  public record Booking(UUID id,String clientId,int room,RoomCategory category,String typeName,ReservationStatus status,
      LocalDate checkIn,LocalDate checkOut,Instant createdAt,BigDecimal roomRaw,List<Service> services,Set<UUID> waivedServices,Payment payment,Refund refund){
    public Booking{services=List.copyOf(services);waivedServices=Set.copyOf(waivedServices);}
  }
  public record Recharge(UUID id,String clientId,BigDecimal amount,Instant at){}
  public ReportSnapshot{rooms=List.copyOf(rooms);bookings=List.copyOf(bookings);recharges=List.copyOf(recharges);points=List.copyOf(points);}
}
