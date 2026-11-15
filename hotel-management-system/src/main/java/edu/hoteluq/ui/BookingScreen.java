package edu.hoteluq.ui;

import edu.hoteluq.service.*;
import edu.hoteluq.service.Views.*;
import edu.hoteluq.model.BookingRecords.*;
import edu.hoteluq.promotion.PromotionDefinition;
import edu.hoteluq.loyalty.LoyaltyCatalog;
import edu.hoteluq.singleton.Hotel;
import javafx.scene.control.*;
import java.time.LocalDate;
import java.util.*;
import java.math.BigDecimal;

final class BookingScreen {
  private final ShellController shell;
  private final DesktopController c;
  private final BookingView editing;
  BookingScreen(ShellController shell) {this(shell,null);}
  BookingScreen(ShellController shell,BookingView editing) {this.shell=shell;this.c=shell.controller;this.editing=editing;}
  record Catalog(List<RoomView> rooms,List<RoomTypeView> types,List<ServiceView> services,List<LoyaltyCatalog.PackageView> packages,List<UserView> clients) {}
  void show() {
    String token=c.token();shell.actions.run(()->new Catalog(c.hotel.rooms(token),c.hotel.roomTypes(token),c.hotel.services(token),c.loyalty.packages(token),c.admin()?c.hotel.clients(token):List.of(c.user())),this::render);
  }
  private void render(Catalog data) {
    var from=Ui.date(editing==null?LocalDate.now(edu.hoteluq.config.HotelTime.ZONE).plusDays(1):editing.quote().checkIn());
    var until=Ui.date(editing==null?from.getValue().plusDays(1):editing.quote().checkOut());
    var guests=Ui.field("Huéspedes");guests.setText(editing==null?"1":String.valueOf(editing.quote().guests()));
    var client=Ui.combo(data.clients());Ui.names(client,v->v.id()+" · "+v.name());
    if(!c.admin())client.setValue(c.user());if(editing!=null){client.setValue(data.clients().stream().filter(v->v.id().equals(editing.quote().clientId())).findFirst().orElse(null));client.setDisable(true);}
    var rooms=Ui.table(data.rooms(),new Ui.Column<RoomView>("Número",RoomView::number),new Ui.Column<RoomView>("Tipo",RoomView::category),new Ui.Column<RoomView>("Piso",RoomView::floor),new Ui.Column<RoomView>("Activa",RoomView::active));
    var detail=Ui.label("Seleccione una habitación para ver tarifa, capacidad y características.");
    rooms.getSelectionModel().selectedItemProperty().addListener((o,a,r)->{if(r!=null){var type=data.types().stream().filter(t->t.category()==r.category()).findFirst().orElseThrow();detail.setText(type.name()+" · "+UiText.cop(type.priceAmount())+" / noche · Capacidad "+type.capacity()+"\n"+type.description()+"\n"+r.features());}});
    var services=Ui.choices(data.services().stream().filter(ServiceView::available).toList());Ui.names(services,v->v.name()+" · "+UiText.cop(v.priceAmount()));
    var packages=Ui.choices(data.packages().stream().filter(LoyaltyCatalog.PackageView::available).toList());Ui.names(packages,v->v.name()+" · "+UiText.cop(v.price()));
    var promotions=Ui.<PromotionDefinition>combo(List.of());Ui.names(promotions,v->v.settings().name()+" · "+v.settings().percent()+" % · v"+v.version());
    var offers=Ui.<Hotel.PromotionOffer>table(List.of(),new Ui.Column<>("Promoción",v->v.promotion().settings().name()),new Ui.Column<>("Elegible",Hotel.PromotionOffer::eligible),new Ui.Column<>("Condiciones",v->v.promotion().settings().conditions()),new Ui.Column<>("Resultado",Hotel.PromotionOffer::message));offers.setPrefHeight(130);
    var summary=Ui.label("La cotización no crea reservas ni cargos. Crear borrador bloquea disponibilidad hasta su cancelación.");
    java.util.function.Supplier<BookingRequest> request=()->new BookingRequest(Ui.selected(rooms).number(),from.getValue(),until.getValue(),FormInput.integer(guests.getText(),"huéspedes"),services.getSelectionModel().getSelectedItems().stream().map(ServiceView::id).toList(),promotions.getValue()==null?null:promotions.getValue().id(),packages.getSelectionModel().getSelectedItems().stream().map(LoyaltyCatalog.PackageView::id).toList());
    if(editing!=null) {
      rooms.getItems().stream().filter(r->r.number()==editing.quote().roomNumber()).findFirst().ifPresent(r->rooms.getSelectionModel().select(r));
      for(var p:data.packages())if(editing.packageIds().contains(p.id()))packages.getSelectionModel().select(p);
      Set<UUID> packageLeaves=new HashSet<>();data.packages().stream().filter(p->editing.packageIds().contains(p.id())).forEach(p->p.services().forEach(s->packageLeaves.add(s.id())));
      for(var service:data.services())if(!packageLeaves.contains(service.id()) && editing.quote().services().stream().anyMatch(s->s.id().equals(service.id())))services.getSelectionModel().select(service);
      if(editing.promotion()!=null){promotions.getItems().add(editing.promotion());promotions.setValue(editing.promotion());}
      summary.setText(Ui.quote(editing.quote()));
    }
    String creationKey=UUID.randomUUID().toString();
    var search=Ui.button("Buscar disponibilidad",()->shell.safe(()->{LocalDate f=from.getValue(),t=until.getValue();shell.actions.run(()->c.hotel.availableRooms(c.token(),f,t),list->{rooms.getItems().setAll(list);detail.setText("Seleccione una habitación disponible. En edición puede volver a abrir el formulario para conservar la habitación actual.");});}));
    var eligible=Ui.button("Consultar promociones",()->shell.safe(()->{var r=request.get();String id=Ui.selected(client,"cliente").id();shell.actions.run(()->editing==null?c.bookings.offers(c.token(),id,r):c.bookings.offersModification(c.token(),id,editing.id(),editing.version(),r),list->{offers.getItems().setAll(list);promotions.getItems().setAll(list.stream().filter(Hotel.PromotionOffer::eligible).map(Hotel.PromotionOffer::promotion).toList());promotions.setValue(null);shell.message(list.isEmpty()?"No hay promociones configuradas. Puede reservar sin promoción.":"Seleccione una promoción elegible si desea aplicarla.");});}));
    var preview=Ui.primary("Cotizar",()->shell.safe(()->{var r=request.get();String id=Ui.selected(client,"cliente").id();shell.actions.run(()->editing==null?c.bookings.preview(c.token(),id,r):c.bookings.previewModification(c.token(),editing.id(),editing.version(),r),q->summary.setText(Ui.quote(q)));}));
    var save=Ui.button(editing==null?"Crear borrador":"Guardar cambios PENDING",()->shell.safe(()->{var r=request.get();String id=Ui.selected(client,"cliente").id();if(!Ui.confirm("Guardar reserva pendiente","Esta acción conserva un borrador y bloquea disponibilidad. Todavía no se cobra."))return;shell.actions.run(()->editing==null?c.bookings.create(c.token(),id,creationKey,r):c.bookings.modify(c.token(),editing.id(),editing.version(),r),v->new ClientScreens(shell).openBooking(v));}));
    shell.display(Ui.scroll(Ui.page(editing==null?"Encuentre su próxima estadía":"Modificar reserva pendiente",Ui.row(Ui.input("Cliente",client),Ui.input("Entrada",from),Ui.input("Salida",until),Ui.input("Huéspedes",guests),search),rooms,detail,new TitledPane("Tipos y tarifas",Ui.table(data.types(),new Ui.Column<RoomTypeView>("Tipo",RoomTypeView::name),new Ui.Column<RoomTypeView>("Tarifa / noche",v->UiText.cop(v.priceAmount())),new Ui.Column<RoomTypeView>("Capacidad",RoomTypeView::capacity),new Ui.Column<RoomTypeView>("Descripción",RoomTypeView::description))),Ui.row(Ui.input("Servicios · selección múltiple",services),Ui.input("Paquetes · no duplique sus servicios",packages)),eligible,offers,Ui.row(Ui.input("Promoción opcional",promotions),Ui.button("Sin promoción",()->promotions.setValue(null)),preview,save),summary)));
  }
}
