package edu.hoteluq.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/** Transportable immutable report. Contains no JavaFX objects, entities, email, cards or credentials. */
public record ReportResult(ReportQuery query,Instant generatedAt,List<String> criteria,List<Section> sections,
    List<Metric> totals,List<ChartSpec> charts) {
  public enum Unit { COUNT("cantidad"),COP("COP"),POINTS("puntos"),PERCENT("%"),NIGHTS("habitación-noche");
    public final String label;Unit(String label){this.label=label;}
  }
  public record Cell(String text,BigDecimal number) {
    public static Cell text(Object value){return new Cell(value instanceof Instant at?java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(at.atZone(edu.hoteluq.config.HotelTime.ZONE)):Objects.toString(value,"—"),null);}
    public static Cell number(BigDecimal value){return new Cell(null,Objects.requireNonNull(value));}
    public String display(){return number==null?text:number.toPlainString();}
  }
  public record Row(List<Cell> cells){public Row{cells=List.copyOf(cells);}}
  public record Section(String title,List<String> columns,List<Row> rows){public Section{columns=List.copyOf(columns);rows=List.copyOf(rows);for(var row:rows)if(row.cells().size()!=columns.size())throw new IllegalArgumentException("Report row/column mismatch");}}
  public record Metric(String key,String label,BigDecimal value,Unit unit){}
  public record Point(String label,BigDecimal value){public Point{Objects.requireNonNull(label);Objects.requireNonNull(value);}}
  public record Series(String name,List<Point> points){public Series{points=List.copyOf(points);}}
  public record ChartSpec(String title,Unit unit,List<Series> series){public ChartSpec{series=List.copyOf(series);}public String toString(){return title;}}
  public ReportResult{criteria=List.copyOf(criteria);sections=List.copyOf(sections);totals=List.copyOf(totals);charts=List.copyOf(charts);}
  public BigDecimal total(String key){return totals.stream().filter(m->m.key().equals(key)).findFirst().orElseThrow().value();}
  public static Row row(Object... values){return new Row(Arrays.stream(values).map(v->v instanceof BigDecimal n?Cell.number(n):Cell.text(v)).toList());}
}
