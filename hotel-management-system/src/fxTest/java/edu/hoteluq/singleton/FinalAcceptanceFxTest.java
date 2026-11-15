package edu.hoteluq.singleton;

import edu.hoteluq.ui.*;
import edu.hoteluq.demo.DemoFixtures;
import edu.hoteluq.service.*;
import edu.hoteluq.service.Views.*;
import edu.hoteluq.loyalty.*;
import edu.hoteluq.promotion.*;
import edu.hoteluq.model.BookingRecords.*;
import edu.hoteluq.enums.*;
import edu.hoteluq.report.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** Acceptance through real controls, confirmations and results; services only prepare fixtures/assert effects. */
class FinalAcceptanceFxTest extends Phase2Fixture {
  private Stage stage;private Parent root;private DesktopController desktop;private ShellController shell;
  @BeforeAll static void startToolkit() throws Exception {var ready=new CountDownLatch(1);Platform.startup(()->{Platform.setImplicitExit(false);ready.countDown();});assertTrue(ready.await(15,TimeUnit.SECONDS));}
  @AfterAll static void endToolkit(){Platform.exit();}
  private static <T>T fx(Callable<T> body) throws Exception {var task=new FutureTask<>(body);Platform.runLater(task);return task.get(15,TimeUnit.SECONDS);}
  private void await(Callable<Boolean> body) throws Exception {long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(25);while(System.nanoTime()<end){if(fx(body))return;Thread.sleep(40);}fail("Acceptance condition timed out");}
  private void ready() throws Exception {await(()->!shell.actions.busyProperty().get());}
  private void open(Hotel aggregate,String email,String password) throws Exception {
    desktop=new DesktopController(aggregate);fx(()->{var loader=new FXMLLoader(HotelApplication.class.getResource("shell.fxml"));loader.setControllerFactory(t->new ShellController(desktop));root=loader.load();shell=loader.getController();var scene=new Scene(root,1450,1000);scene.getStylesheets().add(HotelApplication.class.getResource("hotel.css").toExternalForm());stage=new Stage();stage.setScene(scene);stage.show();((TextField)root.lookup("#email")).setText(email);((PasswordField)root.lookup("#password")).setText(password);((Button)root.lookup("#submit")).fire();return null;});await(()->desktop.authenticated() && !shell.actions.busyProperty().get() && !root.lookupAll(".table-view").isEmpty());
  }
  @AfterEach void close() throws Exception {if(stage!=null)fx(()->{shell.close();stage.close();return null;});}
  private Button button(String name){return root.lookupAll(".button").stream().filter(n->n instanceof Button b && b.getText().equals(name)).map(n->(Button)n).findFirst().orElseThrow(()->new AssertionError("Missing button "+name));}
  private void click(String name) throws Exception {fx(()->{button(name).fire();return null;});ready();}
  private void field(String prompt,String text) throws Exception {fx(()->{var value=root.lookupAll(".text-field").stream().filter(n->n instanceof TextField f && prompt.equals(f.getPromptText())).map(n->(TextField)n).findFirst().orElseThrow(()->new AssertionError("Missing field "+prompt));value.setText(text);return null;});}
  @SuppressWarnings("unchecked") private <T> TableView<T> table(Class<T> type){
    return (TableView<T>)root.lookupAll(".table-view").stream().filter(
            n->n instanceof TableView<?> t && !t.getItems().isEmpty() && type.isInstance(t.getItems().getFirst())).
            findFirst().orElseThrow();}

  @SuppressWarnings("unchecked")
  private TableView<UserView> clientTable() {
    var node = root.lookup("#adminClientsTable");

    if (!(node instanceof TableView<?>)) {
      throw new AssertionError(
              "No se encontró la tabla de clientes"
      );
    }
    return (TableView<UserView>) node;
  }
  private void awaitClientTable() throws Exception {
    await(() ->
            root.lookup("#adminClientsTable") instanceof TableView<?>
    );
  }
  private <T> void select(Class<T> type,java.util.function.Predicate<T> condition) throws Exception {fx(()->{var t=table(type);t.getSelectionModel().select(t.getItems().stream().filter(condition).findFirst().orElseThrow());return null;});}
  @SuppressWarnings("unchecked") private <T> ComboBox<T> combo(Class<T> type){return (ComboBox<T>)root.lookupAll(".combo-box").stream().filter(n->n instanceof ComboBox<?> c && !c.getItems().isEmpty() && type.isInstance(c.getItems().getFirst())).findFirst().orElseThrow();}
  @SuppressWarnings("unchecked") private <T> ListView<T> list(Class<T> type){return (ListView<T>)root.lookupAll(".list-view").stream().filter(n->n instanceof ListView<?> l && !l.getItems().isEmpty() && type.isInstance(l.getItems().getFirst())).findFirst().orElseThrow();}
  private void check(String label,boolean selected) throws Exception {fx(()->{((CheckBox)root.lookupAll(".check-box").stream().filter(n->n instanceof CheckBox b && b.getText().equals(label)).findFirst().orElseThrow()).setSelected(selected);return null;});}
  private void navigate(DesktopController.Screen screen) throws Exception {fx(()->{button(screen.title).fire();return null;});ready();assertEquals(screen,desktop.screen());}
  private DialogPane dialog(String header){return Window.getWindows().stream().filter(w->w!=stage && w.isShowing()).map(w->w.getScene().getRoot()).filter(n->n instanceof DialogPane d && (header==null || Objects.toString(d.getHeaderText(),"").startsWith(header))).map(n->(DialogPane)n).findFirst().orElse(null);}
  private void accept(String header) throws Exception {await(()->dialog(header)!=null);fx(()->{((Button)dialog(header).lookupButton(ButtonType.OK)).fire();return null;});}
  private void confirmedClick(String button,String header) throws Exception {Platform.runLater(()->button(button).fire());accept(header);ready();}
  private void capture(String name) throws Exception {fx(()->{root.applyCss();root.layout();var image=stage.getScene().snapshot(null);var out=new BufferedImage((int)image.getWidth(),(int)image.getHeight(),BufferedImage.TYPE_INT_ARGB);for(int y=0;y<out.getHeight();y++)for(int x=0;x<out.getWidth();x++)out.setRGB(x,y,image.getPixelReader().getArgb(x,y));Path p=Path.of("build/h06");Files.createDirectories(p);ImageIO.write(out,"png",p.resolve(name+".png").toFile());return null;});}
  @ParameterizedTest @EnumSource(Benefit.Kind.class)
  void clientPackagesPromotionsAndEachPointBenefitAreRealUiOperations(Benefit.Kind kind) throws Exception {
    var input=new DemoDataFactory.Credentials("operador@hoteluq.invalid",UUID.randomUUID().toString(),UUID.randomUUID().toString());var demo=DemoDataFactory.create(input,Clock.fixed(clock.instant(),ZoneOffset.UTC));
    awaitOpenDemo(demo,input);var dates=fx(()->root.lookupAll(".date-picker").stream().map(n->(DatePicker)n).sorted(Comparator.comparing(DatePicker::getValue)).toList());
    fx(()->{dates.get(0).setValue(demo.nextStay());dates.get(1).setValue(demo.nextStay().plusDays(2));table(RoomView.class).getSelectionModel().select(table(RoomView.class).getItems().stream().filter(r->r.number()==105).findFirst().orElseThrow());list(LoyaltyCatalog.PackageView.class).getSelectionModel().selectFirst();return null;});
    await(()->!shell.actions.busyProperty().get());click("Buscar disponibilidad");select(RoomView.class,r->r.number()==105);click("Consultar promociones");assertTrue(fx(()->table(Hotel.PromotionOffer.class).getItems().stream().anyMatch(Hotel.PromotionOffer::eligible)));
    fx(()->{combo(PromotionDefinition.class).getSelectionModel().selectFirst();return null;});click("Cotizar");assertTrue(fx(()->root.lookupAll(".label").stream().anyMatch(n->n instanceof Label l && l.getText().contains("225.000 COP"))));
    confirmedClick("Crear borrador","Guardar reserva pendiente");var pending=fx(()->table(BookingView.class).getSelectionModel().getSelectedItem());assertEquals(ReservationStatus.PENDING,pending.status());
    Platform.runLater(()->button("Redimir puntos y pagar").fire());await(()->dialog("Seleccione un beneficio")!=null);fx(()->{var d=dialog("Seleccione un beneficio");@SuppressWarnings("unchecked")ComboBox<Benefit> values=(ComboBox<Benefit>)d.lookupAll(".combo-box").stream().filter(n->n instanceof ComboBox<?>).findFirst().orElseThrow();values.setValue(values.getItems().stream().filter(b->b.kind()==kind).findFirst().orElseThrow());((Button)d.lookupButton(ButtonType.OK)).fire();return null;});accept("Autorizar puntos y cargo");ready();
    var paid=fx(()->table(BookingView.class).getSelectionModel().getSelectedItem());assertEquals(ReservationStatus.CONFIRMED,paid.status());assertEquals(2,paid.quote().services().size());assertNotNull(paid.promotion());assertEquals(switch(kind){case RESERVATION_DISCOUNT->"207000";case SERVICE->"202500";case PACKAGE->"180000";},paid.payment().amount().toPlainString());capture("cliente-"+kind.name().toLowerCase()+"-pago");
    confirmedClick("Solicitar cancelación","Revisar devolución oficial");var cancelled=fx(()->table(BookingView.class).getSelectionModel().getSelectedItem());assertEquals(paid.payment().amount(),cancelled.refund().amount());assertEquals(ReservationStatus.CANCELLED,cancelled.status());capture("cliente-"+kind.name().toLowerCase()+"-reembolso");
    navigate(DesktopController.Screen.POINTS);assertTrue(fx(()->root.lookupAll(".label").stream().anyMatch(n->n instanceof Label l && l.getText().equals("427 puntos disponibles"))));navigate(DesktopController.Screen.CARD);assertTrue(fx(()->!table(edu.hoteluq.model.CardMovement.class).getItems().isEmpty()));
  }
  private void awaitOpenDemo(DemoDataFactory.Context demo,DemoDataFactory.Credentials input) throws Exception {open(demo.hotel(),demo.clientEmail(),input.clientPassword());}
  @Test void administratorCrudAndReferenceProtectionAreExercisedThroughForms() throws Exception {

    open(hotel, "admin@example.com", adminPassword);

    navigate(DesktopController.Screen.CLIENTS);
    awaitClientTable();

    // Crear cliente
    field("ID", "ui-client");
    field("Nombre", "Cliente UI");
    field("Correo", "ui@hoteluq.invalid");
    field("Contraseña nueva · alta/restablecer", UUID.randomUUID().toString());

    click("Crear");

    // Esperar a que el cliente aparezca en la tabla
    await(() -> {
      var node = root.lookup("#adminClientsTable");

      return node instanceof TableView<?> table
              && table.getItems().stream().anyMatch(
              item -> item instanceof UserView user
                      && user.id().equals("ui-client")
      );
    });

    // Actualizar cliente
    select(UserView.class, user -> user.id().equals("ui-client"));
    field("Nombre", "Cliente editado");

    click("Actualizar seleccionado");

    await(() -> {
      var node = root.lookup("#adminClientsTable");

      return node instanceof TableView<?> table
              && table.getItems().stream().anyMatch(
              item -> item instanceof UserView user
                      && user.id().equals("ui-client")
                      && user.name().equals("Cliente editado")
      );
    });

    // Eliminar cliente
    select(UserView.class, user -> user.id().equals("ui-client"));

    confirmedClick("Eliminar", "Eliminar cliente");

    await(() ->
            clientTable().getItems().stream()
                    .noneMatch(u -> u.id().equals("ui-client"))
    );
    navigate(DesktopController.Screen.TYPES);fx(()->{combo(RoomCategory.class).setValue(RoomCategory.FAMILY);return null;});field("Nombre","Tipo UI");field("COP por noche","100.50");field("Capacidad","2");field("Descripción","Tipo de aceptación");click("Crear");select(RoomTypeView.class,v->v.category()==RoomCategory.FAMILY);field("Nombre","Tipo UI editado");click("Actualizar seleccionado");assertTrue(fx(()->table(RoomTypeView.class).getItems().stream().anyMatch(v->v.name().equals("Tipo UI editado"))));
    navigate(DesktopController.Screen.ROOMS);field("Número","201");field("Piso","2");fx(()->{combo(RoomCategory.class).setValue(RoomCategory.FAMILY);return null;});click("Crear");select(RoomView.class,v->v.number()==201);field("Piso","3");click("Actualizar seleccionado");assertEquals(3,fx(()->table(RoomView.class).getItems().getFirst().floor()));select(RoomView.class,v->v.number()==201);confirmedClick("Eliminar","Eliminar habitación");
    navigate(DesktopController.Screen.TYPES);select(RoomTypeView.class,v->v.category()==RoomCategory.FAMILY);confirmedClick("Eliminar","Eliminar tipo");assertEquals(1,fx(()->table(RoomTypeView.class).getItems().size()));
    navigate(DesktopController.Screen.SERVICES);field("Nombre","Servicio UI");field("Importe COP","25.50");click("Crear");select(ServiceView.class,v->true);field("Nombre","Servicio UI editado");field("Importe COP","30.50");click("Actualizar seleccionado");check("Disponible",false);select(ServiceView.class,v->true);check("Disponible",false);click("Guardar disponibilidad");assertFalse(fx(()->table(ServiceView.class).getItems().getFirst().available()));select(ServiceView.class,v->true);check("Disponible",true);click("Guardar disponibilidad");
    field("Nombre de paquete","Paquete UI");field("Descripción","Composición verificada en interfaz");fx(()->{list(ServiceView.class).getSelectionModel().selectFirst();return null;});click("Crear paquete");assertEquals("Paquete UI",fx(()->table(LoyaltyCatalog.PackageView.class).getItems().getFirst().name()));select(ServiceView.class,v->true);confirmedClick("Eliminar","Eliminar servicio");assertEquals(1,fx(()->table(ServiceView.class).getItems().size()));capture("admin-servicios-paquetes-protegidos");
  }
  @Test void administratorCreatesUpdatesAndDeactivatesPromotionInUi() throws Exception {
    open(hotel,"admin@example.com",adminPassword);navigate(DesktopController.Screen.PROMOTIONS);field("Nombre","Promoción UI");field("Descripción","Fixture gráfico, no aprobación comercial");field("Porcentaje","10");field("Noches mínimas (opcional)","2");check("Activa",true);check("Estos parámetros comerciales están aprobados por el responsable",true);
    fx(()->{combo(PromotionDefinition.Combination.class).setValue(PromotionDefinition.Combination.EXCLUSIVE);combo(PromotionDefinition.PointsAccrual.class).setValue(PromotionDefinition.PointsAccrual.EXCLUDED);return null;});click("Crear");select(PromotionDefinition.class,v->true);field("Porcentaje","15");check("Estos parámetros comerciales están aprobados por el responsable",true);click("Actualizar seleccionado");assertEquals(2,fx(()->table(PromotionDefinition.class).getItems().getFirst().version()));select(PromotionDefinition.class,v->true);confirmedClick("Activar / desactivar seleccionado","Cambiar actividad");assertFalse(fx(()->table(PromotionDefinition.class).getItems().getFirst().settings().active()));capture("admin-promocion-versionada");
  }
  @Test void administratorRechargesChecksOutCancelsAndExportsUsingButtons() throws Exception {
    room(101);room(102);var bookings=new ReservationService(hotel);service.recharge(admin,"client-1",1000,"Fixture");var one=bookings.create(owner,"client-1","out",new BookingRequest(101,FROM,FROM.plusDays(2),1,List.of(),null));bookings.confirm(owner,one.id());var two=bookings.create(owner,"client-1","cancel",new BookingRequest(102,FROM.plusDays(5),FROM.plusDays(7),1,List.of(),null));bookings.confirm(owner,two.id());clock.advance(Duration.between(clock.instant(),one.schedule().checkOut()));open(hotel,"admin@example.com",adminPassword);
    navigate(DesktopController.Screen.CARD);field("Identificación del cliente","client-1");click("Consultar");field("Importe COP","100");field("Descripción","Recarga desde aceptación UI");confirmedClick("Recargar","Autorizar recarga");assertTrue(fx(()->root.lookupAll(".label").stream().anyMatch(n->n instanceof Label l && l.getText().equals("700 COP"))));
    navigate(DesktopController.Screen.BOOKINGS);select(BookingView.class,v->v.id().equals(one.id()));confirmedClick("Registrar check-out","Registrar salida");assertEquals(ReservationStatus.COMPLETED,fx(()->table(BookingView.class).getItems().stream().filter(v->v.id().equals(one.id())).findFirst().orElseThrow().status()));select(BookingView.class,v->v.id().equals(two.id()));confirmedClick("Cancelar por el hotel","Revisar devolución oficial");assertEquals(new java.math.BigDecimal("200"),fx(()->table(BookingView.class).getItems().stream().filter(v->v.id().equals(two.id())).findFirst().orElseThrow().refund().amount()));capture("admin-checkout-cancelacion");
    navigate(DesktopController.Screen.REPORTS);fx(()->{combo(ReportQuery.Kind.class).setValue(ReportQuery.Kind.CANCELLATIONS);((DatePicker)root.lookup("#reportFrom")).setValue(FROM.plusDays(2));((DatePicker)root.lookup("#reportThrough")).setValue(FROM.plusDays(2));return null;});click("Generar reporte");Path output=Path.of("build/h06/aceptacion-ui-"+UUID.randomUUID()+".txt").toAbsolutePath();Files.createDirectories(output.getParent());fx(()->{((TextField)root.lookup("#reportExportPath")).setText(output.toString());return null;});click("Exportar TXT");assertTrue(Files.exists(output));assertTrue(Files.readString(output).contains("Reembolsado: 200 COP"));Files.copy(output,Path.of("build/h06/exportacion-ui.txt"),StandardCopyOption.REPLACE_EXISTING);capture("admin-exportacion-txt");
  }
}
