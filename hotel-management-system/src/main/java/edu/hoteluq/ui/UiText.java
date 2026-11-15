package edu.hoteluq.ui;

import edu.hoteluq.exceptions.*;
import edu.hoteluq.config.HotelTime;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class UiText {
  private UiText() {}
  public static String cop(BigDecimal value) {NumberFormat f=NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO"));f.setMaximumFractionDigits(Math.max(0,value.stripTrailingZeros().scale()));return f.format(value)+" COP";}
  public static String at(Instant value) {return value==null?"—":DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(HotelTime.ZONE).format(value)+" · Bogotá";}
  public static String error(Throwable ex) {
    if(ex instanceof AuthenticationException)return "Sesión inválida o vencida. Inicie sesión de nuevo; revise sus credenciales.";
    if(ex instanceof AuthorizationException)return "Su usuario no tiene permiso para realizar esta operación.";
    if(ex instanceof HotelException) {
      String m=ex.getMessage()==null?"Operación rechazada por las reglas del hotel":ex.getMessage();
      if(m.contains("Stale") || m.contains("changed"))return "Los datos cambiaron. Actualice la pantalla y revise la nueva cotización. ("+m+")";
      if(m.contains("configured") || m.contains("configuration") || m.contains("rules"))return "Falta configurar o revisar una política comercial. Solicite ayuda al administrador. ("+m+")";
      if(m.toLowerCase().contains("insufficient"))return "Saldo insuficiente de tarjeta o puntos vigentes. ("+m+")";
      return "No se pudo completar: "+m;
    }
    if(ex instanceof IllegalArgumentException)return "Revise los campos, selecciones y formatos del formulario.";
    return "Ocurrió un error inesperado. La operación no se ha dado por completada; actualice antes de reintentar.";
  }
}
