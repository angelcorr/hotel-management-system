package edu.hoteluq.ui;

import edu.hoteluq.promotion.*;
import edu.hoteluq.promotion.PromotionDefinition.*;
import edu.hoteluq.loyalty.*;
import edu.hoteluq.service.*;
import edu.hoteluq.service.Views.*;
import edu.hoteluq.enums.RoomCategory;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.*;
import java.util.*;

final class CommercialScreens {
  private final ShellController s;
  private final DesktopController c;
  CommercialScreens(ShellController shell){s=shell;c=s.controller;}
  record Data(List<PromotionDefinition> promotions,List<ServiceView> services,List<LoyaltyCatalog.PackageView> packages) {}
  private void approved(CheckBox box){if(!box.isSelected())throw new edu.hoteluq.exceptions.HotelException("Confirme que los valores comerciales cuentan con aprobación antes de guardarlos");}
  void promotions(){s.actions.run(()->new Data(c.promotions.list(c.token()),c.hotel.services(c.token()),c.loyalty.packages(c.token())),data->{
    var table=Ui.table(data.promotions(),new Ui.Column<PromotionDefinition>("Promoción",v->v.settings().name()),new Ui.Column<PromotionDefinition>("Descuento",v->v.settings().percent()+" %"),new Ui.Column<PromotionDefinition>("Activa",v->v.settings().active()),new Ui.Column<PromotionDefinition>("Versión",PromotionDefinition::version),new Ui.Column<PromotionDefinition>("Condiciones",v->v.settings().conditions()));
    var name=Ui.field("Nombre");var description=Ui.field("Descripción");var percent=Ui.field("Porcentaje");var active=new CheckBox("Activa");var from=Ui.field("AAAA-MM-DDTHH:mm");var until=Ui.field("AAAA-MM-DDTHH:mm");
    var stayFrom=Ui.date(null);var stayUntil=Ui.date(null);var nights=Ui.field("Noches mínimas (opcional)");var frequent=Ui.field("Reservas COMPLETED mínimas (opcional)");
    var types=Ui.choices(List.of(RoomCategory.values()));var services=Ui.choices(data.services());Ui.names(services,v->v.name());var packages=Ui.choices(data.packages());Ui.names(packages,v->v.name());
    var combine=Ui.combo(List.of(Combination.values()));var accrual=Ui.combo(List.of(PointsAccrual.values()));var approved=new CheckBox("Estos parámetros comerciales están aprobados por el responsable");
    table.getSelectionModel().selectedItemProperty().addListener((o,a,d)->{if(d!=null){var p=d.settings();name.setText(p.name());description.setText(p.description());percent.setText(p.percent().toPlainString());active.setSelected(p.active());from.setText(local(p.validFrom()));until.setText(local(p.validUntil()));combine.setValue(p.combination());accrual.setValue(p.pointsAccrual());nights.clear();frequent.clear();types.getSelectionModel().clearSelection();services.getSelectionModel().clearSelection();packages.getSelectionModel().clearSelection();stayFrom.setValue(null);stayUntil.setValue(null);approved.setSelected(false);
      for(var condition:p.conditions()){
        if(condition instanceof PromotionCondition.MinimumNights q)nights.setText(""+q.nights());
        if(condition instanceof PromotionCondition.FrequentClient q)frequent.setText(""+q.completed());
        if(condition instanceof PromotionCondition.StayWindow q){stayFrom.setValue(q.from());stayUntil.setValue(q.until());}
        if(condition instanceof PromotionCondition.RoomTypes q)q.types().forEach(v->types.getSelectionModel().select(v));
        if(condition instanceof PromotionCondition.Services q)data.services().stream().filter(v->q.ids().contains(v.id())).forEach(v->services.getSelectionModel().select(v));
        if(condition instanceof PromotionCondition.Packages q)data.packages().stream().filter(v->q.ids().contains(v.id())).forEach(v->packages.getSelectionModel().select(v));
      }
    }});
    java.util.function.Consumer<Boolean> save=creating->s.safe(()->{
      approved(approved);var previous=creating?null:Ui.selected(table);List<PromotionCondition> conditions=new ArrayList<>();
      if(!nights.getText().isBlank())conditions.add(new PromotionCondition.MinimumNights(FormInput.longValue(nights.getText(),"noches")));
      if(!frequent.getText().isBlank())conditions.add(new PromotionCondition.FrequentClient(FormInput.longValue(frequent.getText(),"frecuencia")));
      if(stayFrom.getValue()!=null || stayUntil.getValue()!=null)conditions.add(new PromotionCondition.StayWindow(stayFrom.getValue(),stayUntil.getValue()));
      if(!types.getSelectionModel().isEmpty())conditions.add(new PromotionCondition.RoomTypes(Set.copyOf(types.getSelectionModel().getSelectedItems())));
      if(!services.getSelectionModel().isEmpty())conditions.add(new PromotionCondition.Services(services.getSelectionModel().getSelectedItems().stream().map(ServiceView::id).collect(java.util.stream.Collectors.toSet())));
      if(!packages.getSelectionModel().isEmpty())conditions.add(new PromotionCondition.Packages(packages.getSelectionModel().getSelectedItems().stream().map(v->v.id()).collect(java.util.stream.Collectors.toSet())));
      var settings=new Settings(name.getText(),description.getText(),FormInput.decimal(percent.getText(),"descuento"),active.isSelected(),FormInput.instant(from.getText()),FormInput.instant(until.getText()),conditions,Ui.selected(combine,"combinación"),Ui.selected(accrual,"acumulación"));
      s.actions.run(()->creating?c.promotions.create(c.token(),settings):c.promotions.update(c.token(),previous.id(),previous.version(),settings),v->{promotions();s.message("Promoción guardada · v"+v.version());});
    });
    var toggle=Ui.button("Activar / desactivar seleccionado",()->s.safe(()->{var d=Ui.selected(table);if(Ui.confirm("Cambiar actividad",d.settings().name()+" · v"+d.version()+"\nLas ofertas PENDING deberán revisarse de nuevo."))s.actions.run(()->c.promotions.setActive(c.token(),d.id(),d.version(),!d.settings().active()),v->promotions());}));
    var history=Ui.button("Ver auditoría",()->s.actions.run(()->c.promotions.history(c.token()),list->{var t=Ui.table(list,new Ui.Column<PromotionProgram.Change>("Operación",PromotionProgram.Change::operationId),new Ui.Column<PromotionProgram.Change>("Responsable",PromotionProgram.Change::actorId),new Ui.Column<PromotionProgram.Change>("Fecha",v->UiText.at(v.at())),new Ui.Column<PromotionProgram.Change>("Versión",v->v.after().version()));Alert dialog=new Alert(Alert.AlertType.INFORMATION);dialog.setTitle("Auditoría de promociones");dialog.getDialogPane().setContent(t);dialog.showAndWait();}));
    s.display(Ui.scroll(Ui.page("Promociones condicionadas",table,Ui.row(name,description,percent,active),Ui.row(Ui.input("Vigencia de operación · Bogotá (opcional)",from),Ui.input("Hasta · extremo exclusivo",until)),Ui.row(Ui.input("Estadía desde",stayFrom),Ui.input("Salida máxima",stayUntil),nights,frequent),Ui.row(Ui.input("Tipos permitidos",types),Ui.input("Servicios requeridos",services),Ui.input("Paquetes requeridos",packages)),Ui.row(Ui.input("Combinación explícita",combine),Ui.input("Acumulación explícita",accrual)),approved,Ui.row(Ui.primary("Crear",()->save.accept(true)),Ui.button("Actualizar seleccionado",()->save.accept(false)),toggle,history),Ui.label("No se instalan porcentajes o condiciones de ejemplo. El motor valida todas las condiciones en la confirmación."))));
  });}
  private String local(Instant value){return value==null?"":value.atZone(edu.hoteluq.config.HotelTime.ZONE).toLocalDateTime().toString();}
  void parameters(){s.actions.run(()->c.bookings.configuration(c.token()),rules->{
    var in=Ui.field("HH:mm · entrada");var out=Ui.field("HH:mm · salida");var earliest=Ui.field("Minutos desde salida (puede ser negativo)");var latest=Ui.field("Minutos desde salida");var approved=new CheckBox("Horarios y ventana de check-out aprobados");
    if(rules!=null){in.setText(rules.checkInTime().toString());if(rules.completionTime()!=null)out.setText(rules.completionTime().toString());if(rules.checkoutWindow()!=null){earliest.setText(""+rules.checkoutWindow().earliestOffset().toMinutes());latest.setText(""+rules.checkoutWindow().latestOffset().toMinutes());}}
    java.util.function.Supplier<BookingRules.CheckoutWindow> window=()->new BookingRules.CheckoutWindow(Duration.ofMinutes(FormInput.longValue(earliest.getText(),"inicio de ventana")),Duration.ofMinutes(FormInput.longValue(latest.getText(),"fin de ventana")));
    var save=Ui.primary("Configurar horarios iniciales",()->s.safe(()->{approved(approved);var configured=new BookingRules(FormInput.time(in.getText(),"entrada"),FormInput.time(out.getText(),"salida"),window.get());s.actions.run(()->{c.bookings.configure(c.token(),configured);return true;},v->parameters());}));
    var changeWindow=Ui.button("Configurar solo ventana de check-out",()->s.safe(()->{approved(approved);var w=window.get();s.actions.run(()->{c.bookings.configureCheckoutWindow(c.token(),w);return true;},v->parameters());}));
    var points=new VBox(12);var benefits=new VBox(12);loyaltyParameters(points);benefitParameters(benefits);
    s.display(Ui.scroll(Ui.page("Parámetros comerciales",Ui.label("COP y America/Bogota son oficiales. Cancelación fija: >72 h 100 %, [24,72] h 80 %, <24 h 0 %; hotel 100 %. No son editables."),Ui.row(Ui.input("Entrada",in),Ui.input("Salida",out)),Ui.row(earliest,latest),approved,Ui.row(save,changeWindow),Ui.label("Los horarios no se cambian silenciosamente sobre reservas históricas; el servicio aplica las restricciones existentes."),points,benefits)));
  });}
  private void loyaltyParameters(VBox target){
    var id=Ui.field("ID de política");var version=Ui.field("Versión");var paid=Ui.field("COP por unidad");var count=Ui.field("Puntos por unidad");var refund=Ui.combo(List.of(LoyaltyPolicy.RefundTreatment.values()));var spent=Ui.combo(List.of(LoyaltyPolicy.SpentTreatment.values()));var restoration=Ui.combo(List.of(LoyaltyPolicy.RedemptionCancellation.values()));var approved=new CheckBox("Tasa y tratamiento de cancelaciones aprobados");
    var save=Ui.primary("Guardar política de puntos",()->s.safe(()->{approved(approved);var policy=new LoyaltyPolicy(id.getText(),FormInput.longValue(version.getText(),"versión"),FormInput.decimal(paid.getText(),"unidad COP"),FormInput.longValue(count.getText(),"unidad puntos"),Ui.selected(refund,"reversión"),Ui.selected(spent,"puntos gastados"),Ui.selected(restoration,"restauración"));s.actions.run(()->{c.loyalty.configure(c.token(),policy);return true;},v->{s.message("Política guardada; las acumulaciones históricas conservan su versión.");});}));
    target.getChildren().setAll(Ui.title("Puntos · configuración explícita"),Ui.row(id,version,paid,count),Ui.row(refund,spent,restoration),approved,save,Ui.label("Sin tasa: no se inventan puntos. La política de deuda es compensación dinámica, y la restauración conserva la caducidad original; revise el contrato H-02."));
    // No follow-up command here: avoid racing another UI load while the main screen is being constructed.
    var view=Ui.button("Consultar política actual",()->s.actions.run(()->c.loyalty.policy(c.token()),p->{if(p==null){s.message("No existe política de puntos configurada.");return;}id.setText(p.id());version.setText(""+p.version());paid.setText(p.paidUnit().toPlainString());count.setText(""+p.pointsPerUnit());refund.setValue(p.refundTreatment());spent.setValue(p.spentTreatment());restoration.setValue(p.redemptionCancellation());}));target.getChildren().add(view);
    var client=Ui.field("Cliente para acumulaciones diferidas");var deferred=new VBox(8);target.getChildren().add(Ui.row(client,Ui.button("Consultar diferidas",()->s.safe(()->{String owner=FormInput.required(client.getText(),"cliente");s.actions.run(()->c.loyalty.deferred(c.token(),owner),list->{var table=Ui.table(list,new Ui.Column<PointsLedger.Deferred>("Pago",v->v.payment().id()),new Ui.Column<PointsLedger.Deferred>("Fecha",v->UiText.at(v.payment().at())),new Ui.Column<PointsLedger.Deferred>("Cancelado",PointsLedger.Deferred::cancelled));deferred.getChildren().setAll(table,Ui.button("Liquidar seleccionado",()->s.safe(()->{var d=Ui.selected(table);if(Ui.confirm("Acreditación explícita",d.payment().id()+"\nConservará fecha y vencimiento originales. Requiere política aprobada."))s.actions.run(()->{c.loyalty.settle(c.token(),owner,d.payment().id());return true;},v->{s.message("Liquidación registrada; consulte la cuenta de puntos.");deferred.getChildren().clear();});})));});}))));target.getChildren().add(deferred);
  }
  private void benefitParameters(VBox target){
    var id=Ui.field("UUID existente (vacío para crear)");var version=Ui.field("Versión explícita");var name=Ui.field("Nombre");var cost=Ui.field("Puntos necesarios");var kind=Ui.combo(List.of(Benefit.Kind.values()));var discount=Ui.field("Descuento COP (solo descuento)");var targetId=Ui.field("UUID de servicio/paquete (según tipo)");var approved=new CheckBox("Equivalencia aprobada");var list=new VBox(10);
    var save=Ui.primary("Guardar beneficio",()->s.safe(()->{approved(approved);UUID identity=id.getText().isBlank()?UUID.randomUUID():UUID.fromString(id.getText().trim());var type=Ui.selected(kind,"tipo de beneficio");var b=new Benefit(identity,FormInput.longValue(version.getText(),"versión"),name.getText(),FormInput.longValue(cost.getText(),"puntos"),type,type==Benefit.Kind.RESERVATION_DISCOUNT?FormInput.decimal(discount.getText(),"descuento"):null,type==Benefit.Kind.RESERVATION_DISCOUNT?null:UUID.fromString(FormInput.required(targetId.getText(),"destino")));s.actions.run(()->{c.loyalty.benefit(c.token(),b);return true;},v->s.message("Equivalencia guardada. No se modifican canjes históricos."));}));
    var query=Ui.button("Consultar beneficios",()->s.actions.run(()->c.loyalty.benefits(c.token()),values->{var table=Ui.table(values,new Ui.Column<Benefit>("ID",Benefit::id),new Ui.Column<Benefit>("Nombre",Benefit::name),new Ui.Column<Benefit>("Coste",Benefit::cost),new Ui.Column<Benefit>("Tipo",Benefit::kind),new Ui.Column<Benefit>("Destino",Benefit::targetId));table.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{if(b!=null){id.setText(b.id().toString());version.setText(""+b.version());name.setText(b.name());cost.setText(""+b.cost());kind.setValue(b.kind());discount.setText(b.discount()==null?"":b.discount().toPlainString());targetId.setText(b.targetId()==null?"":b.targetId().toString());approved.setSelected(false);}});list.getChildren().setAll(table);}));
    var chooseTarget=Ui.button("Seleccionar servicio / paquete",()->s.safe(()->{
      var type=Ui.selected(kind,"tipo de beneficio");
      if(type==Benefit.Kind.RESERVATION_DISCOUNT){targetId.clear();s.message("Un descuento COP no referencia un servicio o paquete.");return;}
      if(type==Benefit.Kind.SERVICE)s.actions.run(()->c.hotel.services(c.token()),values->{ChoiceDialog<ServiceView> d=new ChoiceDialog<>(null,values);d.setHeaderText("Servicio de destino");d.showAndWait().ifPresent(v->targetId.setText(v.id().toString()));});
      else s.actions.run(()->c.loyalty.packages(c.token()),values->{ChoiceDialog<LoyaltyCatalog.PackageView> d=new ChoiceDialog<>(null,values);d.setHeaderText("Paquete de destino");d.showAndWait().ifPresent(v->targetId.setText(v.id().toString()));});
    }));
    target.getChildren().setAll(Ui.title("Beneficios · sin equivalencias predeterminadas"),Ui.row(id,version,name,cost),Ui.row(kind,discount,targetId,chooseTarget),approved,Ui.row(save,query),list);
  }
}
