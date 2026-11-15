package edu.hoteluq.singleton;

import edu.hoteluq.ui.*;
import edu.hoteluq.model.BookingRecords.BookingView;
import edu.hoteluq.enums.ReservationStatus;
import edu.hoteluq.exceptions.AuthenticationException;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.*;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** Actual display/toolkit, FXML, events, async services and rendered screenshots. No mock financial logic. */
class JavaFxIntegrationTest extends Phase2Fixture {
  private Stage stage;
  private Parent root;
  private DesktopController desktop;
  private ShellController shell;
  @BeforeAll static void toolkit() throws Exception {
    CountDownLatch ready=new CountDownLatch(1);
    Platform.startup(()->{Platform.setImplicitExit(false);ready.countDown();});assertTrue(ready.await(15,TimeUnit.SECONDS));
  }
  @AfterAll static void stopToolkit(){Platform.exit();}
  private static <T> T fx(Callable<T> call) throws Exception {FutureTask<T> task=new FutureTask<>(call);Platform.runLater(task);return task.get(15,TimeUnit.SECONDS);}
  private void await(Callable<Boolean> condition) throws Exception {
    long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
    while(System.nanoTime()<end){if(fx(condition))return;Thread.sleep(40);}fail("JavaFX condition timed out");
  }
  @BeforeEach void open() throws Exception {
    room(101);service.recharge(admin,"client-1",1000,"FX fixture funds");desktop=new DesktopController(hotel);
    fx(()->{var loader=new FXMLLoader(HotelApplication.class.getResource("shell.fxml"));loader.setControllerFactory(type->new ShellController(desktop));root=loader.load();shell=loader.getController();var scene=new Scene(root,1280,850);scene.getStylesheets().add(HotelApplication.class.getResource("hotel.css").toExternalForm());stage=new Stage();stage.setTitle("HotelUQ · H-04 integración");stage.setScene(scene);stage.show();return null;});
  }
  @AfterEach void close() throws Exception {fx(()->{if(shell!=null)shell.close();if(stage!=null)stage.close();return null;});}
  private Button button(String text){return root.lookupAll(".button").stream().filter(n->n instanceof Button b && b.getText().equals(text)).map(n->(Button)n).findFirst().orElseThrow(()->new AssertionError("Button missing: "+text));}
  private void login(String email,String password) throws Exception {
    fx(()->{((TextField)root.lookup("#email")).setText(email);((PasswordField)root.lookup("#password")).setText(password);((Button)root.lookup("#submit")).fire();return null;});
    await(()->desktop.authenticated() && !shell.actions.busyProperty().get() && root.lookupAll(".table-view").size()>0);
  }
  @SuppressWarnings("unchecked") private TableView<Object> table(Class<?> type) {
    return root.lookupAll(".table-view").stream().filter(n->n instanceof TableView<?> t && !t.getItems().isEmpty() && type.isInstance(t.getItems().getFirst())).map(n->(TableView<Object>)n).findFirst().orElseThrow();
  }
  private BookingView booking(){return (BookingView)table(BookingView.class).getItems().getFirst();}
  private void acceptAction(String text) throws Exception {
    Platform.runLater(()->button(text).fire());
    await(()->Window.getWindows().stream().anyMatch(w->w!=stage && w.isShowing() && w.getScene()!=null && w.getScene().getRoot() instanceof DialogPane));
    fx(()->{for(Window w:new ArrayList<>(Window.getWindows()))if(w!=stage && w.isShowing() && w.getScene()!=null && w.getScene().getRoot() instanceof DialogPane pane){((Button)pane.lookupButton(ButtonType.OK)).fire();break;}return null;});
  }
  private void screenshot(String name) throws Exception {
    fx(()->{root.applyCss();root.layout();var image=stage.getScene().snapshot(null);var reader=image.getPixelReader();var out=new BufferedImage((int)image.getWidth(),(int)image.getHeight(),BufferedImage.TYPE_INT_ARGB);
      for(int y=0;y<out.getHeight();y++)for(int x=0;x<out.getWidth();x++)out.setRGB(x,y,reader.getArgb(x,y));Path dir=Path.of("build/h04");Files.createDirectories(dir);ImageIO.write(out,"png",dir.resolve(name+".png").toFile());return null;});
  }
  @Test void customerCanQuoteCreatePayCancelAndLogoutUsingRealViews() throws Exception {
    login("ana@example.com",ownerPassword);
    fx(()->{var dates=root.lookupAll(".date-picker").stream().map(n->(DatePicker)n).toList();dates.get(0).setValue(FROM);dates.get(1).setValue(FROM.plusDays(2));table(edu.hoteluq.service.Views.RoomView.class).getSelectionModel().selectFirst();button("Cotizar").fire();return null;});
    await(()->!shell.actions.busyProperty().get() && root.lookupAll(".label").stream().anyMatch(n->n instanceof Label l && l.getText().contains("TOTAL:")));fx(()->{((ScrollPane)root.lookup(".scroll-pane")).setVvalue(1);return null;});screenshot("cliente-cotizacion");
    acceptAction("Crear borrador");await(()->!shell.actions.busyProperty().get() && root.lookupAll(".table-view").stream().anyMatch(n->n instanceof TableView<?> t && !t.getItems().isEmpty() && t.getItems().getFirst() instanceof BookingView));
    assertEquals(ReservationStatus.PENDING,fx(()->booking().status()));acceptAction("Confirmar y pagar");await(()->!shell.actions.busyProperty().get() && booking().status()==ReservationStatus.CONFIRMED);screenshot("cliente-pago");
    acceptAction("Solicitar cancelación");await(()->!shell.actions.busyProperty().get() && booking().status()==ReservationStatus.CANCELLED);screenshot("cliente-reembolso");
    assertEquals(new java.math.BigDecimal("1000"),service.card(owner,"client-1").exactBalance());fx(()->{button("Tarjeta y movimientos").fire();return null;});await(()->!shell.actions.busyProperty().get() && root.lookupAll(".label").stream().anyMatch(n->n instanceof Label l && l.getText().contains("1.000 COP")));screenshot("cliente-tarjeta");
    fx(()->{button("Puntos").fire();return null;});await(()->!shell.actions.busyProperty().get() && root.lookupAll(".label").stream().anyMatch(n->n instanceof Label l && l.getText().contains("Tasa no configurada")));screenshot("cliente-puntos");String token=desktop.token();fx(()->{((Button)root.lookup("#logout")).fire();return null;});await(()->!desktop.authenticated());assertThrows(AuthenticationException.class,()->service.me(token));
  }
  @Test void administratorCanNavigateToParametersAndPromotionEditor() throws Exception {
    login("admin@example.com",adminPassword);fx(()->{button("Parámetros comerciales").fire();return null;});await(()->!shell.actions.busyProperty().get() && root.lookupAll(".label").stream().anyMatch(n->n instanceof Label l && l.getText().contains("Puntos · configuración")));screenshot("administrador-parametros");
    fx(()->{button("Promociones").fire();return null;});await(()->!shell.actions.busyProperty().get() && root.lookupAll(".label").stream().anyMatch(n->n instanceof Label l && l.getText().equals("Promociones condicionadas")));screenshot("administrador-promociones");
    assertTrue(desktop.admin());assertEquals(DesktopController.Screen.PROMOTIONS,desktop.screen());
  }
  @Test void registrationUsesRealFxmlAndCreatesOnlyAClient() throws Exception {
    fx(()->{((Button)root.lookup("#switchMode")).fire();((TextField)root.lookup("#id")).setText("fx-client");((TextField)root.lookup("#name")).setText("Cliente interfaz");((TextField)root.lookup("#email")).setText("fx@example.com");((PasswordField)root.lookup("#password")).setText(ownerPassword);((PasswordField)root.lookup("#repeat")).setText(ownerPassword);((Button)root.lookup("#submit")).fire();return null;});
    await(()->!shell.actions.busyProperty().get() && ((Label)root.lookup("#heading")).getText().equals("Iniciar sesión"));screenshot("acceso");login("fx@example.com",ownerPassword);assertEquals("fx-client",desktop.user().id());assertFalse(desktop.admin());
  }

}
