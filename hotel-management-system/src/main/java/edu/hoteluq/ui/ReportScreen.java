package edu.hoteluq.ui;

import edu.hoteluq.report.*;
import edu.hoteluq.report.ReportQuery.Kind;
import edu.hoteluq.enums.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/** Report filters/navigation only. Tables, charts and exports consume the very same immutable result. */
public final class ReportScreen {
  private final ShellController shell;
  private ReportResult displayed;
  private final ReportChartView chartView=new ReportChartAdapter();
  public ReportScreen(ShellController shell){this.shell=shell;}
  public void show(){
    var kind=Ui.combo(List.of(Kind.values()));kind.setId("reportKind");kind.setValue(Kind.RESERVATIONS);
    var from=Ui.date(LocalDate.now(edu.hoteluq.config.HotelTime.ZONE).withDayOfMonth(1));from.setId("reportFrom");var through=Ui.date(LocalDate.now(edu.hoteluq.config.HotelTime.ZONE));through.setId("reportThrough");
    var client=Ui.field("Cliente (opcional)");var room=Ui.field("Habitación (opcional)");var service=Ui.field("UUID servicio (solo servicios)");service.setDisable(true);var category=Ui.combo(List.of(RoomCategory.values()));var status=Ui.combo(List.of(ReservationStatus.values()));
    var compare=new CheckBox("Comparar reservas con periodo anterior");var results=new VBox(14);var chartArea=new VBox(10);var plot=Ui.combo(List.of(ReportChartView.Type.values()));plot.setValue(ReportChartView.Type.BARS);plot.setId("reportChartType");var dataset=Ui.<ReportResult.ChartSpec>combo(List.of());dataset.setId("reportDataset");
    Runnable draw=()->{chartArea.getChildren().clear();shell.safe(()->{if(dataset.getValue()!=null)chartArea.getChildren().setAll(chartView.render(dataset.getValue(),Ui.selected(plot,"tipo de gráfico")));});};
    dataset.setOnAction(e->draw.run());plot.setOnAction(e->draw.run());
    var destination=Ui.field("Destino TXT opcional; vacío abre selector");destination.setId("reportExportPath");
    var export=Ui.button("Exportar TXT",()->shell.safe(()->{
      if(displayed==null)throw new edu.hoteluq.exceptions.HotelException("Genere primero un reporte");
      FileChooser chooser=new FileChooser();chooser.setTitle("Exportar reporte UTF-8");chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Texto UTF-8","*.txt"));chooser.setInitialFileName("hoteluq-"+displayed.query().kind().name().toLowerCase(Locale.ROOT)+"-"+displayed.query().from()+".txt");
      Path path;
      if(destination.getText().isBlank()){var file=chooser.showSaveDialog(results.getScene().getWindow());if(file==null)return;path=file.toPath();}
      else path=Path.of(destination.getText().trim());
      boolean replace=Files.exists(path);
      if(replace && !Ui.confirm("Reemplazar archivo",path.toAbsolutePath().toString()))return;
      ReportResult accepted=displayed;shell.actions.run(()->{try{return shell.controller.reports.export(shell.controller.token(),accepted,new TxtReportExporter(),path,replace);}catch(java.io.IOException ex){throw new edu.hoteluq.exceptions.HotelException("No se pudo escribir el TXT: "+ex.getMessage());}},saved->shell.message("Reporte exportado: "+saved));
    }));export.setDisable(true);
    var generate=Ui.primary("Generar reporte",()->shell.safe(()->{
      var query=new ReportQuery(Ui.selected(kind,"reporte"),from.getValue(),through.getValue(),client.getText(),category.getValue(),room.getText().isBlank()?null:FormInput.integer(room.getText(),"habitación"),status.getValue(),service.getText().isBlank()?null:UUID.fromString(service.getText().trim()),compare.isSelected());
      shell.actions.run(()->shell.controller.reports.generate(shell.controller.token(),query),report->{
        displayed=report;results.getChildren().clear();results.getChildren().add(Ui.title(report.query().kind().title));results.getChildren().add(Ui.label("Corte: "+UiText.at(report.generatedAt())+" · Periodo: "+report.query().from()+" a "+report.query().through()+"\nFiltros aplicados: "+report.query()));
        for(var criterion:report.criteria())results.getChildren().add(Ui.label(criterion));
        for(var total:report.totals())results.getChildren().add(Ui.label(total.label()+": "+(total.value()==null?"N/A":total.value().toPlainString())+" "+total.unit().label));
        for(var section:report.sections()){
          TableView<ReportResult.Row> table=new TableView<>(javafx.collections.FXCollections.observableArrayList(section.rows()));table.setPrefHeight(230);table.setPlaceholder(Ui.label("Sin registros para este filtro"));
          for(int i=0;i<section.columns().size();i++){final int index=i;var column=new TableColumn<ReportResult.Row,String>(section.columns().get(i));column.setCellValueFactory(cell->new javafx.beans.property.ReadOnlyStringWrapper(cell.getValue().cells().get(index).display()));column.setComparator((a,b)->{try{return new java.math.BigDecimal(a).compareTo(new java.math.BigDecimal(b));}catch(NumberFormatException ex){return a.compareTo(b);}});column.setPrefWidth(165);table.getColumns().add(column);}
          results.getChildren().addAll(Ui.title(section.title()),table);
        }
        dataset.getItems().setAll(report.charts());dataset.setValue(report.charts().isEmpty()?null:report.charts().getFirst());plot.setValue(ReportChartView.Type.BARS);draw.run();export.setDisable(false);shell.message("Reporte generado con datos reales del corte mostrado. TXT y gráfico usan ese mismo resultado.");
      });
    }));generate.setId("reportGenerate");
    kind.setOnAction(e->{boolean clientOnly=kind.getValue()==Kind.POINTS || kind.getValue()==Kind.RECHARGES;category.setDisable(clientOnly);status.setDisable(clientOnly);room.setDisable(clientOnly);if(clientOnly){category.setValue(null);status.setValue(null);room.clear();}service.setDisable(kind.getValue()!=Kind.SERVICES);if(kind.getValue()!=Kind.SERVICES)service.clear();compare.setDisable(kind.getValue()!=Kind.RESERVATIONS);if(kind.getValue()!=Kind.RESERVATIONS)compare.setSelected(false);});
    var clear=Ui.button("Limpiar filtros opcionales",()->{client.clear();room.clear();service.clear();category.setValue(null);status.setValue(null);compare.setSelected(false);});
    shell.display(Ui.scroll(Ui.page("Reportes y estadísticas",Ui.label("Cambiar filtros requiere Generar reporte. La exportación conserva el último corte mostrado; no recalcula silenciosamente."),Ui.row(Ui.input("Familia",kind),Ui.input("Desde",from),Ui.input("Hasta (inclusive)",through)),Ui.row(client,room,Ui.input("Categoría opcional",category),Ui.input("Estado actual opcional",status)),Ui.row(service,compare,clear),Ui.row(generate,export,destination),Ui.row(Ui.input("Datos del gráfico",dataset),Ui.input("Visualización",plot)),chartArea,results)));
  }
}
