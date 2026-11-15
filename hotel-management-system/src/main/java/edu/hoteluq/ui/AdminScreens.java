package edu.hoteluq.ui;

import edu.hoteluq.service.Views.*;
import edu.hoteluq.enums.RoomCategory;
import javafx.scene.control.*;
import java.math.BigDecimal;
import java.util.*;

final class AdminScreens {
  private final ShellController s;
  private final DesktopController c;
  AdminScreens(ShellController shell){s=shell;c=s.controller;}
  void show(DesktopController.Screen screen) {switch(screen){case CLIENTS->clients();case ROOMS->rooms();case TYPES->types();case SERVICES->services();default->throw new IllegalArgumentException("Unsupported admin screen");}}
  private void action(Runnable work,Runnable refresh){s.safe(()->s.actions.run(()->{work.run();return true;},v->{refresh.run();s.message("Operación completada. Datos actualizados.");}));}
  private void clients(){s.actions.run(()->c.hotel.clients(c.token()),list->{
    var table=Ui.table(list,new Ui.Column<UserView>("ID",UserView::id),new Ui.Column<UserView>("Nombre",UserView::name),new Ui.Column<UserView>("Correo",UserView::email));
    table.setId("adminClientsTable");
    var id=Ui.field("ID");var name=Ui.field("Nombre");var email=Ui.field("Correo");var password=new PasswordField();password.setPromptText("Contraseña nueva · alta/restablecer");
    table.getSelectionModel().selectedItemProperty().addListener((o,a,v)->{if(v!=null){id.setText(v.id());name.setText(v.name());email.setText(v.email());password.clear();}});
    var create=Ui.primary("Crear",()->{String i=id.getText(),n=name.getText(),e=email.getText(),p=password.getText();action(()->c.hotel.createClient(c.token(),i,n,e,p),this::clients);});
    var update=Ui.button("Actualizar seleccionado",()->s.safe(()->{var selected=Ui.selected(table);String n=name.getText(),e=email.getText();action(()->c.hotel.updateUser(c.token(),selected.id(),n,e),this::clients);}));
    var delete=Ui.button("Eliminar",()->s.safe(()->{var selected=Ui.selected(table);if(Ui.confirm("Eliminar cliente",selected.id()+" · "+selected.name()+"\nSe respetarán las referencias e historial financiero."))action(()->c.hotel.deleteClient(c.token(),selected.id()),this::clients);}));
    var reset=Ui.button("Restablecer contraseña",()->s.safe(()->{var selected=Ui.selected(table);String secret=password.getText();if(Ui.confirm("Restablecer acceso",selected.id()+" · Se revocarán sus sesiones."))action(()->c.hotel.resetClientPassword(c.token(),selected.id(),secret),this::clients);}));
    s.display(Ui.scroll(Ui.page("Gestión de clientes",table,Ui.row(id,name,email,password),Ui.row(create,update,reset,delete,Ui.button("Actualizar lista",this::clients)))));
  });}
  private void rooms(){s.actions.run(()->c.hotel.rooms(c.token()),list->{
    var table=Ui.table(list,new Ui.Column<RoomView>("Número",RoomView::number),new Ui.Column<RoomView>("Tipo",RoomView::category),new Ui.Column<RoomView>("Piso",RoomView::floor),new Ui.Column<RoomView>("Activa",RoomView::active),new Ui.Column<RoomView>("Características",RoomView::features));
    var number=Ui.field("Número");var floor=Ui.field("Piso");var category=Ui.combo(List.of(RoomCategory.values()));var active=new CheckBox("Activa");active.setSelected(true);
    table.getSelectionModel().selectedItemProperty().addListener((o,a,v)->{if(v!=null){number.setText(""+v.number());floor.setText(""+v.floor());category.setValue(v.category());active.setSelected(v.active());}});
    var create=Ui.primary("Crear",()->s.safe(()->{int n=FormInput.integer(number.getText(),"número"),f=FormInput.integer(floor.getText(),"piso");var type=Ui.selected(category,"tipo");action(()->c.hotel.createRoom(c.token(),type,n,f),this::rooms);}));
    var update=Ui.button("Actualizar seleccionado",()->s.safe(()->{var r=Ui.selected(table);int f=FormInput.integer(floor.getText(),"piso");boolean on=active.isSelected();action(()->c.hotel.updateRoom(c.token(),r.number(),f,on),this::rooms);}));
    var delete=Ui.button("Eliminar",()->s.safe(()->{var r=Ui.selected(table);if(Ui.confirm("Eliminar habitación","Habitación "+r.number()))action(()->c.hotel.deleteRoom(c.token(),r.number()),this::rooms);}));
    s.display(Ui.scroll(Ui.page("Habitaciones · administración",table,Ui.row(number,floor,category,active),Ui.row(create,update,delete,Ui.button("Actualizar lista",this::rooms)),Ui.label("El tipo se elige al crear. Las referencias históricas bloquean modificaciones incompatibles."))));
  });}
  private void types(){s.actions.run(()->c.hotel.roomTypes(c.token()),list->{
    var table=Ui.table(list,new Ui.Column<RoomTypeView>("Categoría",RoomTypeView::category),new Ui.Column<RoomTypeView>("Nombre",RoomTypeView::name),new Ui.Column<RoomTypeView>("Tarifa",v->UiText.cop(v.priceAmount())),new Ui.Column<RoomTypeView>("Capacidad",RoomTypeView::capacity),new Ui.Column<RoomTypeView>("Activo",RoomTypeView::active));
    var category=Ui.combo(List.of(RoomCategory.values()));var name=Ui.field("Nombre");var price=Ui.field("COP por noche");var capacity=Ui.field("Capacidad");var description=Ui.field("Descripción");var active=new CheckBox("Activo");active.setSelected(true);
    table.getSelectionModel().selectedItemProperty().addListener((o,a,v)->{if(v!=null){category.setValue(v.category());name.setText(v.name());price.setText(v.priceAmount().toPlainString());capacity.setText(""+v.capacity());description.setText(v.description());active.setSelected(v.active());}});
    java.util.function.Consumer<Boolean> save=creating->s.safe(()->{var type=creating?Ui.selected(category,"categoría"):Ui.selected(table).category();String n=name.getText(),d=description.getText();var amount=FormInput.decimal(price.getText(),"tarifa");int cap=FormInput.integer(capacity.getText(),"capacidad");action(()->{if(creating)c.hotel.createRoomType(c.token(),type,n,amount,"COP",cap,d);else c.hotel.updateRoomType(c.token(),type,n,amount,"COP",cap,d);},this::types);});
    var toggle=Ui.button("Guardar actividad",()->s.safe(()->{var selected=Ui.selected(table);boolean on=active.isSelected();action(()->c.hotel.setRoomTypeActive(c.token(),selected.category(),on),this::types);}));
    var delete=Ui.button("Eliminar",()->s.safe(()->{var selected=Ui.selected(table);if(Ui.confirm("Eliminar tipo",selected.name()))action(()->c.hotel.deleteRoomType(c.token(),selected.category()),this::types);}));
    s.display(Ui.scroll(Ui.page("Tipos y tarifas",table,Ui.row(category,name,price,capacity,active),description,Ui.row(Ui.primary("Crear",()->save.accept(true)),Ui.button("Actualizar seleccionado",()->save.accept(false)),toggle,delete))));
  });}
  private void services(){s.actions.run(()->c.hotel.services(c.token()),list->{
    var table=Ui.table(list,new Ui.Column<ServiceView>("ID",ServiceView::id),new Ui.Column<ServiceView>("Servicio",ServiceView::name),new Ui.Column<ServiceView>("Precio",v->UiText.cop(v.priceAmount())),new Ui.Column<ServiceView>("Disponible",ServiceView::available));
    var name=Ui.field("Nombre");var price=Ui.field("Importe COP");var available=new CheckBox("Disponible");available.setSelected(true);
    table.getSelectionModel().selectedItemProperty().addListener((o,a,v)->{if(v!=null){name.setText(v.name());price.setText(v.priceAmount().toPlainString());available.setSelected(v.available());}});
    java.util.function.Consumer<Boolean> save=creating->s.safe(()->{UUID id=creating?null:Ui.selected(table).id();String n=name.getText();var amount=FormInput.decimal(price.getText(),"precio");action(()->{if(creating)c.hotel.createService(c.token(),n,amount,"COP");else c.hotel.updateService(c.token(),id,n,amount,"COP");},this::services);});
    var toggle=Ui.button("Guardar disponibilidad",()->s.safe(()->{var selected=Ui.selected(table);boolean on=available.isSelected();action(()->c.hotel.setServiceAvailable(c.token(),selected.id(),on),this::services);}));
    var delete=Ui.button("Eliminar",()->s.safe(()->{var selected=Ui.selected(table);if(Ui.confirm("Eliminar servicio",selected.name()))action(()->c.hotel.deleteService(c.token(),selected.id()),this::services);}));
    var packArea=new javafx.scene.layout.VBox(12);String token=c.token();s.actions.run(()->c.loyalty.packages(token),packages->{
      var packTable=Ui.table(packages,new Ui.Column<edu.hoteluq.loyalty.LoyaltyCatalog.PackageView>("ID",v->v.id()),new Ui.Column<edu.hoteluq.loyalty.LoyaltyCatalog.PackageView>("Paquete",v->v.name()),new Ui.Column<edu.hoteluq.loyalty.LoyaltyCatalog.PackageView>("Precio",v->UiText.cop(v.price())),new Ui.Column<edu.hoteluq.loyalty.LoyaltyCatalog.PackageView>("Disponible",v->v.available()));
      var services=Ui.choices(list);Ui.names(services,ServiceView::name);var children=Ui.choices(packages);Ui.names(children,v->v.name());var packName=Ui.field("Nombre de paquete");var desc=Ui.field("Descripción");
      var create=Ui.primary("Crear paquete",()->s.safe(()->{String n=packName.getText(),d=desc.getText();var ids=services.getSelectionModel().getSelectedItems().stream().map(ServiceView::id).toList();var childIds=children.getSelectionModel().getSelectedItems().stream().map(v->v.id()).toList();action(()->c.loyalty.createPackage(c.token(),n,d,ids,childIds),this::services);}));
      packArea.getChildren().setAll(Ui.title("Paquetes · Composite"),packTable,Ui.row(packName,desc),Ui.row(Ui.input("Servicios",services),Ui.input("Paquetes hijos",children)),create,Ui.label("Paquetes inmutables: sin edición destructiva del historial ni servicios duplicados."));
    });
    s.display(Ui.scroll(Ui.page("Servicios adicionales",table,Ui.row(name,price,available),Ui.row(Ui.primary("Crear",()->save.accept(true)),Ui.button("Actualizar seleccionado",()->save.accept(false)),toggle,delete),packArea)));
  });}
}
