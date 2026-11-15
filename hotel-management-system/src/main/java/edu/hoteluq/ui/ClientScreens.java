package edu.hoteluq.ui;

import edu.hoteluq.model.BookingRecords.*;
import edu.hoteluq.enums.ReservationStatus;
import edu.hoteluq.loyalty.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;

final class ClientScreens {
  private final ShellController shell;
  private final DesktopController c;
  ClientScreens(ShellController shell){this.shell=shell;c=shell.controller;}
  void bookings(){bookings(null);}
  void openBooking(BookingView booking){bookings(booking.id());}
  private void bookings(UUID selected) {
    shell.actions.run(()->c.bookings.list(c.token()),list->{
      var table=Ui.table(list,new Ui.Column<BookingView>("Reserva",BookingView::id),new Ui.Column<BookingView>("Cliente",v->v.quote().clientName()),new Ui.Column<BookingView>("Habitación",v->v.quote().roomNumber()),new Ui.Column<BookingView>("Entrada",v->v.quote().checkIn()),new Ui.Column<BookingView>("Estado",BookingView::status),new Ui.Column<BookingView>("Total",v->UiText.cop(v.quote().total())),new Ui.Column<BookingView>("Versión",BookingView::version));
      var detail=Ui.label("Seleccione una reserva para revisar contrato, comprobantes e historial.");
      table.getSelectionModel().selectedItemProperty().addListener((o,a,v)->{if(v!=null)detail.setText(Ui.quote(v.quote())+"\nReserva: "+v.id()+" · v"+v.version()+" · "+v.status()+
          (v.payment()==null?"":"\nPago "+v.payment().id()+" · "+UiText.at(v.payment().at()))+(v.refund()==null?"":"\nReembolso "+v.refund().percent()+" % · "+UiText.cop(v.refund().amount())+" · "+UiText.at(v.refund().at()))+
          (v.checkout()==null?"":"\nCheck-out: "+UiText.at(v.checkout().at())+" · "+v.checkout().actorId())+"\nHistorial: "+v.history().stream().map(h->h.type()+" · "+UiText.at(h.at())+" · "+h.actorId()).toList());});
      if(selected!=null)table.getItems().stream().filter(v->v.id().equals(selected)).findFirst().ifPresent(v->table.getSelectionModel().select(v));
      var pay=Ui.primary("Confirmar y pagar",()->shell.safe(()->{var r=Ui.selected(table);if(!Ui.confirm("Autorizar cargo a tarjeta",Ui.quote(r.quote())+"\nSe cobrará el total mostrado. ¿Continuar?"))return;shell.actions.run(()->c.bookings.confirm(c.token(),r.id(),r.version()),v->{bookings(v.id());shell.message("Pago confirmado: "+UiText.cop(v.payment().amount()));});}));
      var cancel=Ui.button(c.admin()?"Cancelar por el hotel":"Solicitar cancelación",()->shell.safe(()->{var r=Ui.selected(table);shell.actions.run(()->c.bookings.cancellationQuote(c.token(),r.id(),c.admin()),quote->{if(Ui.confirm("Revisar devolución oficial",quote.percent()+" % · "+UiText.cop(quote.amount())+"\nCalculado: "+UiText.at(quote.at())+"\nSi la ventana o versión cambia, se pedirá revisar de nuevo."))shell.actions.run(()->c.bookings.cancel(c.token(),quote),v->{bookings(v.id());shell.message("Cancelación registrada: "+UiText.cop(v.refund().amount()));});});}));
      var modify=Ui.button("Modificar",()->shell.safe(()->{var r=Ui.selected(table);if(r.status()!=ReservationStatus.PENDING){shell.message("Solo PENDING admite edición directa. Las modificaciones confirmadas siguen bloqueadas hasta definir su liquidación.");return;}new BookingScreen(shell,r).show();}));
      var benefit=Ui.button("Redimir puntos y pagar",()->shell.safe(()->{var r=Ui.selected(table);shell.actions.run(()->c.loyalty.benefits(c.token()),benefits->{if(benefits.isEmpty()){shell.message("No hay equivalencias aprobadas/configuradas. No se puede canjear.");return;}ChoiceDialog<Benefit> dialog=new ChoiceDialog<>(null,benefits);dialog.setTitle("Beneficio");dialog.setHeaderText("Seleccione un beneficio: coste en puntos y tipo");dialog.setContentText("Beneficio");dialog.showAndWait().ifPresent(b->shell.actions.run(()->c.loyalty.preview(c.token(),r.id(),r.version(),b.id()),q->{if(Ui.confirm("Autorizar puntos y cargo",b.name()+" · "+b.cost()+" puntos\n"+Ui.quote(q)))shell.actions.run(()->c.loyalty.redeem(c.token(),r.id(),r.version(),"fx:"+r.id()+":"+r.version()+":"+b.id(),b.id()),v->bookings(v.id()));}));});}));
      HBox buttons=Ui.row(pay,modify,cancel,benefit,Ui.button("Actualizar",this::bookings));
      if(c.admin())buttons.getChildren().add(Ui.button("Registrar check-out",()->shell.safe(()->{var r=Ui.selected(table);if(Ui.confirm("Registrar salida",r.id()+"\nSe registrarán usuario e instante de check-out. Se aplicará la ventana configurada."))shell.actions.run(()->c.bookings.checkout(c.token(),r.id(),"fx-out:"+r.id()),v->bookings(v.id()));})));
      shell.display(Ui.scroll(Ui.page(c.admin()?"Administración de reservas":"Mis reservas",table,buttons,detail)));
    });
  }
  private TextField clientField(){var f=Ui.field("Identificación del cliente");if(!c.admin()){f.setText(c.user().id());f.setEditable(false);}return f;}
  void card() {card(null);}
  private void card(String selectedClient) {
    var id=clientField();if(selectedClient!=null)id.setText(selectedClient);var panel=new VBox(12);Runnable load=()->shell.safe(()->{String client=FormInput.required(id.getText(),"cliente");shell.actions.run(()->c.hotel.card(c.token(),client),card->{
      var movements=Ui.table(card.movements(),new Ui.Column<>("Tipo",v->v.getType()),new Ui.Column<>("Importe",v->UiText.cop(v.getExactAmount())),new Ui.Column<>("Fecha",v->UiText.at(v.getInstant())),new Ui.Column<>("Detalle",v->v.getDescription()));
      panel.getChildren().setAll(Ui.title(UiText.cop(card.exactBalance())),Ui.label(card.number()+" · "+(card.active()?"Activa":"Inactiva")),movements);
      if(c.admin()){
        var amount=Ui.field("Importe COP");var reason=Ui.field("Descripción");var active=new CheckBox("Tarjeta activa");active.setSelected(card.active());
        panel.getChildren().add(Ui.row(amount,reason,Ui.primary("Recargar",()->shell.safe(()->{var value=FormInput.decimal(amount.getText(),"importe");String text=reason.getText();if(Ui.confirm("Autorizar recarga",UiText.cop(value)+"\nCliente: "+client))shell.actions.run(()->c.hotel.recharge(c.token(),client,value,"COP",text),v->card(client));})),active,Ui.button("Guardar estado",()->{boolean value=active.isSelected();shell.actions.run(()->{c.hotel.setCardActive(c.token(),client,value);return true;},v->card(client));})));
      }
    });});
    shell.display(Ui.scroll(Ui.page("Tarjeta virtual y movimientos",Ui.row(id,Ui.button("Consultar",load)),panel)));if(!c.admin() || selectedClient!=null)load.run();
  }
  void points() {
    var id=clientField();var panel=new VBox(12);Runnable load=()->shell.safe(()->{String client=FormInput.required(id.getText(),"cliente");shell.actions.run(()->c.loyalty.account(c.token(),client),account->{
      var lots=Ui.table(account.lots(),new Ui.Column<>("Pago de origen",PointsLedger.Lot::paymentId),new Ui.Column<>("Obtenidos",PointsLedger.Lot::earned),new Ui.Column<>("Derecho vigente",PointsLedger.Lot::entitlement),new Ui.Column<>("Usados",PointsLedger.Lot::used),new Ui.Column<>("Obtención",v->UiText.at(v.obtainedAt())),new Ui.Column<>("Vencimiento",v->UiText.at(v.expiresAt())));
      var events=Ui.table(account.history(),new Ui.Column<>("Movimiento",PointsLedger.Event::type),new Ui.Column<>("Puntos",PointsLedger.Event::points),new Ui.Column<>("Origen",PointsLedger.Event::sourceId),new Ui.Column<>("Fecha efectiva",v->UiText.at(v.effectiveAt())));
      panel.getChildren().setAll(Ui.title(account.available()+" puntos disponibles"),Ui.label("Deuda: "+account.debt()+" · Los vencidos se excluyen automáticamente. Para canjear, abra una reserva PENDING en Reservas."),lots,events);
      shell.actions.run(()->c.loyalty.policy(c.token()),policy->panel.getChildren().add(Ui.label(policy==null?"Tasa no configurada: pagos elegibles quedan diferidos, sin inventar puntos.":"Política de acumulación: "+policy.id()+" v"+policy.version())));
    });});shell.display(Ui.scroll(Ui.page("Cuenta de puntos",Ui.row(id,Ui.button("Consultar",load)),panel)));if(!c.admin())load.run();
  }
}
