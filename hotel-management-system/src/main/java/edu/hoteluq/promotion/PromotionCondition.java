package edu.hoteluq.promotion;

import edu.hoteluq.enums.RoomCategory;
import edu.hoteluq.exceptions.HotelException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Specification: immutable business predicates, composed with AND, against trusted booking facts. */
public sealed interface PromotionCondition permits PromotionCondition.StayWindow, PromotionCondition.RoomTypes,
    PromotionCondition.MinimumNights, PromotionCondition.FrequentClient, PromotionCondition.Services,
    PromotionCondition.Packages, PromotionCondition.All {
  record Context(LocalDate checkIn, LocalDate checkOut, RoomCategory roomType, Set<UUID> services,
      Set<UUID> packages, long completedReservations) {
    public Context { services=Set.copyOf(services); packages=Set.copyOf(packages); }
  }
  boolean satisfiedBy(Context context);
  default PromotionCondition and(PromotionCondition other) { return new All(List.of(this,Objects.requireNonNull(other))); }
  record StayWindow(LocalDate from,LocalDate until) implements PromotionCondition {
    public StayWindow { if(from==null || until==null || !from.isBefore(until)) throw new HotelException("Invalid stay window"); }
    public boolean satisfiedBy(Context c) { return !c.checkIn().isBefore(from) && !c.checkOut().isAfter(until); }
  }
  record RoomTypes(Set<RoomCategory> types) implements PromotionCondition {
    public RoomTypes { types=nonempty(types); }
    public boolean satisfiedBy(Context c) { return types.contains(c.roomType()); }
  }
  record MinimumNights(long nights) implements PromotionCondition {
    public MinimumNights { positive(nights); }
    public boolean satisfiedBy(Context c) { return ChronoUnit.DAYS.between(c.checkIn(),c.checkOut())>=nights; }
  }
  /** Threshold is supplied explicitly; only completed, paid, non-cancelled reservations count. */
  record FrequentClient(long completed) implements PromotionCondition {
    public FrequentClient { positive(completed); }
    public boolean satisfiedBy(Context c) { return c.completedReservations()>=completed; }
  }
  record Services(Set<UUID> ids) implements PromotionCondition {
    public Services { ids=nonempty(ids); }
    public boolean satisfiedBy(Context c) { return c.services().containsAll(ids); }
  }
  record Packages(Set<UUID> ids) implements PromotionCondition {
    public Packages { ids=nonempty(ids); }
    public boolean satisfiedBy(Context c) { return c.packages().containsAll(ids); }
  }
  record All(List<PromotionCondition> conditions) implements PromotionCondition {
    public All { if(conditions==null || conditions.isEmpty()) throw new HotelException("Conditions required"); conditions=List.copyOf(conditions); }
    public boolean satisfiedBy(Context c) { return conditions.stream().allMatch(s -> s.satisfiedBy(c)); }
  }
  private static void positive(long value) { if(value<1) throw new HotelException("Positive condition threshold required"); }
  private static <T> Set<T> nonempty(Set<T> values) {
    if(values==null || values.isEmpty() || values.stream().anyMatch(Objects::isNull)) throw new HotelException("Nonempty condition values required"); return Set.copyOf(values);
  }
}
