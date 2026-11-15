package edu.hoteluq.config;

import java.time.*;

/** Stay dates are LocalDate in the official hotel zone; operations are unambiguous Instants. */
public final class HotelTime {
  public static final ZoneId ZONE = ZoneId.of("America/Bogota");
  private HotelTime() {}
  public static Clock systemClock() { return Clock.system(ZONE); }
  public static Instant instant(LocalDate date, LocalTime time) { return date.atTime(time).atZone(ZONE).toInstant(); }
  public static LocalDateTime local(Instant at) { return LocalDateTime.ofInstant(at, ZONE); }
  public static Clock inHotelZone(Clock clock) { return new ZonedSource(clock, ZONE); }
  private static final class ZonedSource extends Clock {
    private final Clock source; private final ZoneId zone;
    ZonedSource(Clock source, ZoneId zone) { this.source = source; this.zone = zone; }
    @Override public ZoneId getZone() { return zone; }
    @Override public Clock withZone(ZoneId newZone) { return new ZonedSource(source, newZone); }
    @Override public Instant instant() { return source.instant(); }
  }
}
