package edu.hoteluq.ui;

import edu.hoteluq.report.ReportResult.*;
import edu.hoteluq.exceptions.HotelException;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.collections.FXCollections;
import java.util.*;

/** Adapter from toolkit-free analytical series to JavaFX's incompatible chart data contracts. */
public final class ReportChartAdapter implements ReportChartView {
  public Node render(ChartSpec spec,Type type){
    if(type==Type.PIE){
      if(spec.series().size()!=1)throw new HotelException("El pastel necesita una única serie. Seleccione otra distribución o use barras/líneas.");
      if(spec.series().getFirst().points().stream().anyMatch(p->p.value().signum()<0))throw new HotelException("El pastel no representa importes negativos. Use barras o líneas.");
      var values=spec.series().getFirst().points().stream().filter(p->p.value().signum()>0).map(p->new PieChart.Data(p.label()+" · "+p.value().toPlainString()+" "+spec.unit().label,p.value().doubleValue())).toList();
      PieChart chart=new PieChart(FXCollections.observableArrayList(values));chart.setTitle(spec.title());chart.setAnimated(false);chart.setPrefHeight(360);return chart;
    }
    var x=new CategoryAxis();x.setLabel("Fecha / categoría");var y=new NumberAxis();y.setLabel(spec.unit().label);y.setForceZeroInRange(true);
    XYChart<String,Number> chart=type==Type.LINE?new LineChart<>(x,y):new BarChart<>(x,y);chart.setTitle(spec.title());chart.setAnimated(false);chart.setPrefHeight(360);
    for(var source:spec.series()){
      XYChart.Series<String,Number> series=new XYChart.Series<>();series.setName(source.name());
      source.points().forEach(point->series.getData().add(new XYChart.Data<>(point.label(),point.value())));chart.getData().add(series);
    }
    return chart;
  }
}
