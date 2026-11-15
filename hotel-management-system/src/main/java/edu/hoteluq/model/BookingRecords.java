package edu.hoteluq.model;

import edu.hoteluq.enums.*;
import edu.hoteluq.money.CopMoney;
import edu.hoteluq.config.HotelTime;
import edu.hoteluq.strategy.CancellationSchedule;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Immutable contract, audit and accounting records. Raw pricing and final COP obligations are distinct. */
public final class BookingRecords {
  private BookingRecords() {}
  public record ServiceLine(UUID id, String name, BigDecimal price) {}
  public record Quote(String clientId, String clientName, String clientEmail, int roomNumber,
      RoomCategory category, String typeName, String description, List<String> features,
      BigDecimal nightlyPrice, LocalDate checkIn, LocalDate checkOut, int guests,
      List<ServiceLine> services, UUID promotionId, String promotionName, BigDecimal percent,
      BigDecimal subtotal, BigDecimal discount, BigDecimal total, BigDecimal rawSubtotal, BigDecimal rawDiscount,
      edu.hoteluq.promotion.PromotionDefinition promotionDefinition) {
    public Quote { features = List.copyOf(features); services = List.copyOf(services); }
    public Quote(String clientId,String clientName,String clientEmail,int roomNumber,
        RoomCategory category,String typeName,String description,List<String> features,BigDecimal nightlyPrice,
        LocalDate checkIn,LocalDate checkOut,int guests,List<ServiceLine> services,UUID promotionId,
        String promotionName,BigDecimal percent,BigDecimal subtotal,BigDecimal discount,BigDecimal total,
        BigDecimal rawSubtotal,BigDecimal rawDiscount) {
      this(clientId,clientName,clientEmail,roomNumber,category,typeName,description,features,nightlyPrice,checkIn,checkOut,guests,
          services,promotionId,promotionName,percent,subtotal,discount,total,rawSubtotal,rawDiscount,null);
    }
    public String currency() { return CopMoney.CURRENCY; }
  }
  public record Schedule(Instant checkIn, Instant checkOut, ZoneId zone) {
    public Schedule { if (!HotelTime.ZONE.equals(zone)) throw new IllegalArgumentException("Official hotel zone required"); }
  }
  public record Payment(UUID id, UUID reservationId, String cardNumber, BigDecimal amount, Instant at, String currency) {
    public Payment { CopMoney.currency(currency); amount = CopMoney.integral(amount); if (amount.signum() <= 0) throw new IllegalArgumentException("Positive payment required"); }
    public Payment(UUID id, UUID reservationId, String cardNumber, BigDecimal amount, Instant at) { this(id, reservationId, cardNumber, amount, at, CopMoney.CURRENCY); }
  }
  public record Refund(UUID id, UUID reservationId, UUID paymentId, String cardNumber,
      BigDecimal amount, String policy, boolean byHotel, Instant at, String currency,
      String policyId, long policyVersion, BigDecimal percent, Duration timeLeft, CancellationSchedule policySnapshot) {
    public Refund { CopMoney.currency(currency); amount = CopMoney.integral(amount); }
  }
  public record Checkout(UUID operationId, String idempotencyKey, String actorId, Instant at, Instant scheduledAt, String zone, edu.hoteluq.service.BookingRules.CheckoutWindow authorizedWindow) {}
  public record AuditEvent(UUID operationId, String type, String actorId, Instant at, long version,
      String reason, Quote before, Quote after) {}
  public record ModificationProposal(UUID operationId, UUID reservationId, long expectedVersion,
      String actorId, Instant at, String reason, Quote before, Quote proposed, BigDecimal difference, String blockedReason) {}
  public record PolicyChange(UUID operationId, String actorId, Instant at, CancellationSchedule policy) {}
  public record BookingView(UUID id, long version, ReservationStatus status, Quote quote,
      Payment payment, Refund refund, Schedule schedule, Checkout checkout, List<AuditEvent> history,
      edu.hoteluq.promotion.PromotionDefinition promotion, List<UUID> packageIds) {
    public BookingView { history = List.copyOf(history); packageIds=List.copyOf(packageIds); }
    public BookingView(UUID id,long version,ReservationStatus status,Quote quote,Payment payment,Refund refund,Schedule schedule,Checkout checkout,List<AuditEvent> history) {
      this(id,version,status,quote,payment,refund,schedule,checkout,history,null,List.of());
    }
  }
}
