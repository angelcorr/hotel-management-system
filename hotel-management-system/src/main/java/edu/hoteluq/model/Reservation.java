package edu.hoteluq.model;

import edu.hoteluq.enums.ReservationStatus;
import edu.hoteluq.promotion.*;
import edu.hoteluq.exceptions.InvalidReservationException;
import edu.hoteluq.security.*;
import edu.hoteluq.state.*;
import edu.hoteluq.strategy.RefundPolicy;
import edu.hoteluq.validation.DomainValidation;
import edu.hoteluq.model.BookingRecords.*;
import java.math.BigDecimal;
import edu.hoteluq.money.CopMoney;
import edu.hoteluq.config.HotelTime;
import edu.hoteluq.exceptions.HotelException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Reservation extends ManagedEntity {
  private final UUID id = UUID.randomUUID();
  private final Client client;
  private Terms terms;
  private ReservationState state = new PendingState();
  private Payment payment;
  private Refund refund;
  private long version;
  private Schedule schedule;
  private Checkout checkout;
  private final List<AuditEvent> history = new ArrayList<>();
  private record Terms(Room room, LocalDate checkIn, LocalDate checkOut, int guests,
      List<AdditionalService> services, Promotion promotion, Quote quote, PromotionDefinition definition, List<UUID> packageIds) {}
  private record Checkpoint(Terms terms, ReservationState state, Payment payment, Refund refund, long version, Schedule schedule, Checkout checkout, List<AuditEvent> history) {}

  private Reservation(Builder b, Quote quote) {
    client = b.client;
    terms = new Terms(b.room, b.checkIn, b.checkOut, b.guests, List.copyOf(b.services), b.promotion, quote, b.definition, List.copyOf(b.packageIds));
  }

  public UUID getId() { return readProtected(() -> id); }
  public Client getClient() { return readProtected(() -> client); }
  public Room getRoom() { return readProtected(() -> terms.room()); }
  public LocalDate getCheckIn() { return readProtected(() -> terms.checkIn()); }
  public LocalDate getCheckOut() { return readProtected(() -> terms.checkOut()); }
  public int getGuests() { return readProtected(() -> terms.guests()); }
  public List<AdditionalService> getServices() { return readProtected(() -> terms.services()); }
  public Promotion getPromotion() { return readProtected(() -> terms.promotion()); }
  public PromotionDefinition getPromotionDefinition() { return readProtected(() -> terms.definition()); }
  public List<UUID> getPackageIds() { return readProtected(() -> terms.packageIds()); }
  public double getSubtotal() { return readProtected(() -> terms.quote().subtotal().doubleValue()); }
  public double getDiscount() { return readProtected(() -> terms.quote().discount().doubleValue()); }
  public double getTotal() { return readProtected(() -> terms.quote().total().doubleValue()); }
  public BigDecimal getTotalAmount() { return readProtected(() -> terms.quote().total()); }
  public Quote getQuote() { return readProtected(() -> terms.quote()); }
  public Payment getPayment() { return readProtected(() -> payment); }
  public Refund getRefund() { return readProtected(() -> refund); }
  public long getVersion() { return readProtected(() -> version); }
  public BookingView view() { return readProtected(() -> new BookingView(id, version, state.getStatus(), terms.quote(), payment, refund, schedule, checkout, history, terms.definition(), terms.packageIds())); }
  public long getNights() { return readProtected(() -> ChronoUnit.DAYS.between(terms.checkIn(), terms.checkOut())); }
  public ReservationStatus getStatus() { return readProtected(() -> state.getStatus()); }
  public ReservationState getState() { return readProtected(() -> state); }

  public boolean overlaps(LocalDate from, LocalDate to) {
    DomainValidation.period(from, to);
    return readProtected(() -> state.getStatus() != ReservationStatus.CANCELLED
        && terms.checkIn().isBefore(to) && from.isBefore(terms.checkOut()));
  }

  /** Legacy assembly API for detached objects. Managed transitions require the aggregate transaction. */
  public void setStatus(ReservationStatus status) {
    DomainValidation.required(status, "Reservation status");
    setState(switch (status) {
      case PENDING -> new PendingState(); case CONFIRMED -> new ConfirmedState();
      case CANCELLED -> new CancelledState(); case COMPLETED -> new CompletedState();
    });
  }
  public void setState(ReservationState next) {
    change(Action.RESERVATION_STATE, next, () -> state = DomainValidation.required(next, "Reservation state"));
  }
  public void pay() { change(Action.RESERVATION_STATE, null, () -> state.pay(this)); }
  public void cancel(RefundPolicy policy) {
    change(Action.RESERVATION_STATE, null, () -> state.cancel(this, DomainValidation.required(policy, "Refund policy")));
  }
  public void complete() { change(Action.RESERVATION_STATE, null, () -> state.complete(this)); }

  /** Legacy local time; the service uses explicitly configured BookingRules instead. */
  public LocalDateTime getCheckInDateTime() { return readProtected(() -> {
    if (schedule == null) throw new HotelException("Registered contractual schedule required");
    return HotelTime.local(schedule.checkIn());
  }); }
  public Schedule getSchedule() { return readProtected(() -> schedule); }
  public Checkout getCheckout() { return readProtected(() -> checkout); }
  private void managedTransaction() { if (!isManaged()) throw new HotelException("Accounting changes require a managed reservation"); }
  public void recordSchedule(Schedule schedule) {
    managedTransaction(); change(Action.BOOKING_TRANSACTION, null, () -> this.schedule = schedule);
  }
  public void audit(AuditEvent event) {
    managedTransaction(); change(Action.BOOKING_TRANSACTION, null, () -> history.add(event));
  }
  public void recordCheckout(Checkout event) {
    managedTransaction(); change(Action.BOOKING_TRANSACTION, null, () -> {
      if (checkout != null) throw new HotelException("Checkout already recorded");
      state.complete(this); checkout = event; version++;
    });
  }

  public Object checkpoint() {
    return readProtected(() -> new Checkpoint(terms, state, payment, refund, version, schedule, checkout, List.copyOf(history)));
  }
  public void restore(Object checkpoint) {
    managedTransaction();
    change(Action.BOOKING_TRANSACTION, null, () -> {
      Checkpoint c = (Checkpoint) checkpoint;
      terms = c.terms(); state = c.state(); payment = c.payment(); refund = c.refund(); version = c.version(); schedule = c.schedule(); checkout = c.checkout(); history.clear(); history.addAll(c.history());
    });
  }
  public void recordPayment(Payment paid) {
    managedTransaction();
    change(Action.BOOKING_TRANSACTION, null, () -> {
      if (payment != null) throw new InvalidReservationException("Reservation already paid");
      payment = DomainValidation.required(paid, "Payment"); version++;
    });
  }
  public void recordRefund(Refund returned) {
    managedTransaction();
    change(Action.BOOKING_TRANSACTION, null, () -> {
      if (refund != null) throw new InvalidReservationException("Cancellation already recorded");
      refund = DomainValidation.required(returned, "Refund"); version++;
    });
  }
  public void replacePending(Reservation replacement) {
    managedTransaction();
    change(Action.BOOKING_TRANSACTION, null, () -> {
      if (state.getStatus() != ReservationStatus.PENDING || payment != null || client != replacement.client) {
        throw new InvalidReservationException("Only pending reservations can be modified");
      }
      terms = replacement.terms; version++;
    });
  }
  /** Applies a points benefit only within the existing aggregate transaction. Contract snapshots remain immutable. */
  public void applyPointsBenefit(List<AdditionalService> included, BigDecimal monetaryDiscount) {
    applyPointsBenefit(included, monetaryDiscount, List.of());
  }
  public void applyPointsBenefit(List<AdditionalService> included, BigDecimal monetaryDiscount, List<UUID> includedPackages) {
    managedTransaction();
    change(Action.BOOKING_TRANSACTION, null, () -> { terms=benefitTerms(included,monetaryDiscount,includedPackages); version++; });
  }
  public Quote previewPointsBenefit(List<AdditionalService> included,BigDecimal discount,List<UUID> packages) {
    return readProtected(()->benefitTerms(included,discount,packages).quote());
  }
  private Terms benefitTerms(List<AdditionalService> included,BigDecimal monetaryDiscount,List<UUID> includedPackages) {
      if (state.getStatus() != ReservationStatus.PENDING || payment != null)
        throw new InvalidReservationException("Benefit requires an unpaid pending reservation");
      if (terms.promotion()!=null && (terms.definition()==null || terms.definition().settings().combination()==PromotionDefinition.Combination.EXCLUSIVE))
        throw new InvalidReservationException("Promotion does not authorize points combinations");
      List<AdditionalService> combined = new ArrayList<>(terms.services());
      for (AdditionalService service : included) {
        if (!service.isAvailable()) throw new InvalidReservationException("Benefit service unavailable");
        if (!combined.contains(service)) combined.add(service);
      }
      Builder builder = new Builder().client(client).room(terms.room()).checkIn(terms.checkIn()).checkOut(terms.checkOut()).guests(terms.guests());
      combined.forEach(builder::additionalService);
      LinkedHashSet<UUID> packages=new LinkedHashSet<>(terms.packageIds()); packages.addAll(includedPackages);
      builder.packages(List.copyOf(packages));
      if(terms.promotion()!=null) builder.promotion(terms.promotion()).promotionDefinition(terms.definition());
      Quote base = builder.build().getQuote();
      BigDecimal reduction = monetaryDiscount;
      for (AdditionalService service : included) reduction = reduction.add(service.getPriceAmount());
      BigDecimal percent=terms.definition()==null?BigDecimal.ZERO:terms.definition().settings().percent();
      BigDecimal rawNet=terms.definition()==null?new PriceComponent.Fixed(new PriceComponent.Base(base.rawSubtotal()),reduction).amount()
          :PriceComponent.net(base.rawSubtotal(),percent,reduction,terms.definition().settings().combination());
      BigDecimal total = CopMoney.positive(rawNet);
      Quote q = new Quote(base.clientId(),base.clientName(),base.clientEmail(),base.roomNumber(),base.category(),base.typeName(),base.description(),base.features(),
          base.nightlyPrice(),base.checkIn(),base.checkOut(),base.guests(),base.services(),base.promotionId(),base.promotionName(),percent,
          base.subtotal(),base.subtotal().subtract(total),total,base.rawSubtotal(),terms.definition()==null?reduction:base.rawSubtotal().subtract(rawNet),terms.definition());
    return new Terms(terms.room(),terms.checkIn(),terms.checkOut(),terms.guests(),List.copyOf(combined),terms.promotion(),q,terms.definition(),List.copyOf(packages));
  }
  @Deprecated public void markCompleted() { throw new HotelException("Explicit check-out record required"); }
  @Override public String toString() { return readProtected(() -> id + ": " + terms.quote() + " [" + state.getStatus() + "]"); }

  public static class Builder {
    private Client client;
    private Room room;
    private LocalDate checkIn, checkOut;
    private int guests = 1;
    private final List<AdditionalService> services = new ArrayList<>();
    private Promotion promotion;
    private PromotionDefinition definition;
    private List<UUID> packageIds=List.of();
    public Builder client(Client value) { client = value; return this; }
    public Builder room(Room value) { room = value; return this; }
    public Builder checkIn(LocalDate value) { checkIn = value; return this; }
    public Builder checkOut(LocalDate value) { checkOut = value; return this; }
    public Builder guests(int value) { guests = value; return this; }
    public Builder additionalService(AdditionalService value) {
      services.add(DomainValidation.required(value, "Service")); return this;
    }
    public Builder promotion(Promotion value) { promotion = value; return this; }
    public Builder promotionDefinition(PromotionDefinition value) { definition=value;return this; }
    public Builder packages(List<UUID> values) {
      packageIds=List.copyOf(values); if(new HashSet<>(packageIds).size()!=packageIds.size()) throw new InvalidReservationException("Duplicate package");return this;
    }
    public Reservation build() {
      if(definition!=null && (promotion==null || !definition.id().equals(promotion.getId()))) throw new InvalidReservationException("Promotion identity mismatch");
      if (client == null || room == null) throw new InvalidReservationException("Reservation needs a client and room");
      DomainValidation.period(checkIn, checkOut);
      if (!room.isActive() || guests < 1 || guests > room.getCapacity()) {
        throw new InvalidReservationException("Room inactive or capacity exceeded");
      }
      if (new HashSet<>(services).size() != services.size()) throw new InvalidReservationException("Duplicate service");
      var lines = new ArrayList<ServiceLine>();
      BigDecimal nightly = room.getPricePerNightAmount();
      BigDecimal subtotal = nightly.multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(checkIn, checkOut)));
      for (AdditionalService service : services) {
        if (!service.isAvailable()) throw new InvalidReservationException("Service unavailable");
        BigDecimal price = service.getPriceAmount();
        lines.add(new ServiceLine(service.getId(), service.getName(), price)); subtotal = subtotal.add(price);
      }
      BigDecimal percent = promotion == null ? BigDecimal.ZERO : definition==null?promotion.getDiscountPercentAmount():definition.settings().percent();
      BigDecimal discount = subtotal.subtract(new PriceComponent.Percent(new PriceComponent.Base(subtotal),percent).amount());
      BigDecimal rawSubtotal = subtotal, rawDiscount = discount;
      BigDecimal total = CopMoney.positive(rawSubtotal.subtract(rawDiscount));
      subtotal = CopMoney.rounded(rawSubtotal);
      discount = subtotal.subtract(total); // reconciled final discount, no accumulated rounding mismatch
      Quote quote = new Quote(client.getId(), client.getName(), client.getEmail(), room.getNumber(),
          room.getType().getCategory(), room.getType().getName(), room.getType().getDescription(), room.getFeatures(),
          nightly, checkIn, checkOut, guests, lines, promotion == null ? null : promotion.getId(),
          promotion == null ? null : definition==null?promotion.getName():definition.settings().name(), percent, subtotal, discount, total, rawSubtotal, rawDiscount,definition);
      return new Reservation(this, quote);
    }
  }
}
