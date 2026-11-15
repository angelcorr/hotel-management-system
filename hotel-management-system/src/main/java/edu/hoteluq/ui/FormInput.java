package edu.hoteluq.ui;

import edu.hoteluq.exceptions.HotelException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Parsing only: domain rules remain in the services. Explicit formats avoid locale ambiguity. */
public final class FormInput {
  private FormInput() {}
  public static String required(String value,String label) {if(value==null || value.isBlank())throw new HotelException("Complete el campo: "+label);return value.trim();}
  public static int integer(String value,String label) {try{return Integer.parseInt(required(value,label));}catch(NumberFormatException ex){throw new HotelException(label+": escriba un número entero");}}
  public static long longValue(String value,String label) {try{return Long.parseLong(required(value,label));}catch(NumberFormatException ex){throw new HotelException(label+": escriba un número entero");}}
  public static BigDecimal decimal(String value,String label) {
    try {String text=required(value,label);if(!text.matches("[+-]?[0-9]+(?:\\.[0-9]+)?"))throw new NumberFormatException();return new BigDecimal(text);}
    catch(NumberFormatException ex){throw new HotelException(label+": use números sin separadores de miles y punto decimal");}
  }
  public static LocalTime time(String value,String label) {try{return LocalTime.parse(required(value,label));}catch(DateTimeException ex){throw new HotelException(label+": formato HH:mm");}}
  public static Instant instant(String value) {
    if(value==null || value.isBlank())return null;
    try {return LocalDateTime.parse(value.trim()).atZone(edu.hoteluq.config.HotelTime.ZONE).toInstant();}
    catch(DateTimeException ex){throw new HotelException("Fecha/hora: use AAAA-MM-DDTHH:mm, zona Bogotá");}
  }
  public static List<UUID> ids(String value) {
    if(value==null || value.isBlank())return List.of();
    try {return Arrays.stream(value.split(",")).map(String::trim).map(UUID::fromString).toList();}
    catch(IllegalArgumentException ex){throw new HotelException("Identificadores: UUID separados por comas");}
  }
}
