package edu.hoteluq.report;

import edu.hoteluq.config.HotelTime;
import edu.hoteluq.enums.*;
import edu.hoteluq.exceptions.HotelException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record ReportQuery(Kind kind,LocalDate from,LocalDate through,String clientId,RoomCategory category,
    Integer roomNumber,ReservationStatus status,UUID serviceId,boolean comparePrevious) {
  public enum Kind {
    RESERVATIONS("Reservas por periodo"),ROOM_RANKING("Habitaciones más reservadas"),OCCUPANCY("Ocupación contractual"),
    REVENUE("Ingresos por periodo"),SERVICES("Servicios contratados"),RECHARGES("Recargas de tarjetas"),
    CANCELLATIONS("Cancelaciones y reembolsos"),POINTS("Programa de puntos");
    public final String title;Kind(String title){this.title=title;}
    public String toString(){return title;}
  }
  public ReportQuery(Kind kind,LocalDate from,LocalDate through){this(kind,from,through,null,null,null,null,null,false);}
  public ReportQuery {
    if(kind==null || from==null || through==null || from.isAfter(through))throw new HotelException("Rango de reporte inválido");
    long days=ChronoUnit.DAYS.between(from,through)+1;
    if(days>36600 || through.equals(LocalDate.MAX))throw new HotelException("Rango técnico máximo: 36.600 días, con extremo final representable");
    clientId=clientId==null || clientId.isBlank()?null:clientId.trim();
    if(roomNumber!=null && roomNumber<1)throw new HotelException("Habitación inválida");
    if((kind==Kind.RECHARGES || kind==Kind.POINTS) && (category!=null || roomNumber!=null || status!=null || serviceId!=null))throw new HotelException("Este reporte solo admite filtro de cliente");
    if(serviceId!=null && kind!=Kind.SERVICES)throw new HotelException("El filtro de servicio solo aplica a servicios");
    if(comparePrevious && kind!=Kind.RESERVATIONS)throw new HotelException("Comparación disponible para reservas");
    try{through.plusDays(1);if(comparePrevious)from.minusDays(days);}catch(DateTimeException ex){throw new HotelException("Rango de comparación no representable");}
  }
  public long days(){return ChronoUnit.DAYS.between(from,through)+1;}
  public Instant start(){return from.atStartOfDay(HotelTime.ZONE).toInstant();}
  public Instant end(){return through.plusDays(1).atStartOfDay(HotelTime.ZONE).toInstant();}
  public boolean contains(Instant at){return at!=null && !at.isBefore(start()) && at.isBefore(end());}
  public boolean client(String id){return clientId==null || clientId.equals(id);}
}
