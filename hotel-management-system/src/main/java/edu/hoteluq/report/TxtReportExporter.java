package edu.hoteluq.report;

import edu.hoteluq.config.HotelTime;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

public final class TxtReportExporter implements ReportExporter {
  public String extension(){return "txt";}
  private String clean(Object value){return String.valueOf(value).replace("\\","\\\\").replace("\r","\\r").replace("\n","\\n").replace("\t","\\t");}
  public byte[] render(ReportResult report){
    StringBuilder out=new StringBuilder("HOTELUQ — ").append(report.query().kind().title).append('\n');
    out.append("Generación: ").append(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(report.generatedAt().atZone(HotelTime.ZONE))).append("\nZona: America/Bogota\n");
    out.append("Periodo inclusivo: ").append(report.query().from()).append(" a ").append(report.query().through()).append('\n');
    var q=report.query();out.append("Filtros: cliente=").append(clean(q.clientId())).append("; categoría=").append(q.category()).append("; habitación=").append(q.roomNumber()).append("; estado al corte=").append(q.status()).append("; servicio=").append(q.serviceId()).append("; comparar anterior=").append(q.comparePrevious()).append("\nValores null: sin filtro.\n");
    for(String criterion:report.criteria())out.append("Criterio: ").append(clean(criterion)).append('\n');
    for(var section:report.sections()){
      out.append('\n').append(clean(section.title())).append('\n');out.append(String.join("\t",section.columns().stream().map(this::clean).toList())).append('\n');
      if(section.rows().isEmpty())out.append("Sin registros.\n");
      for(var row:section.rows())out.append(String.join("\t",row.cells().stream().map(c->clean(c.display())).toList())).append('\n');
      out.append("Registros: ").append(section.rows().size()).append('\n');
    }
    out.append("\nTOTALES\n");for(var metric:report.totals())out.append(clean(metric.label())).append(": ").append(metric.value()==null?"N/A":metric.value().toPlainString()).append(' ').append(metric.unit().label).append('\n');
    return out.toString().getBytes(StandardCharsets.UTF_8);
  }
}
