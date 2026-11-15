package edu.hoteluq.singleton;

import edu.hoteluq.ui.*;
import edu.hoteluq.report.*;
import edu.hoteluq.service.*;
import edu.hoteluq.loyalty.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.chart.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** Real JavaFX report navigation/filtering/chart adapters over real payments and cancellations. */
class ReportsJavaFxTest extends Phase2Fixture {
  private DesktopController desktop;
  private ShellController shell;
  private Stage stage;
  private Parent root;
  @BeforeAll static void toolkit() throws Exception {CountDownLatch ready=new CountDownLatch(1);Platform.startup(()->{Platform.setImplicitExit(false);ready.countDown();});assertTrue(ready.await(15,TimeUnit.SECONDS));}
  @AfterAll static void stopToolkit(){Platform.exit();}
  private static <T> T fx(Callable<T> action) throws Exception {FutureTask<T> task=new FutureTask<>(action);Platform.runLater(task);return task.get(15,TimeUnit.SECONDS);}
  private void await(Callable<Boolean> condition) throws Exception {long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);while(System.nanoTime()<end){if(fx(condition))return;Thread.sleep(40);}fail("Report UI timed out");}
  @BeforeEach void open() throws Exception {
    room(101);room(102);var bookings=new ReservationService(hotel);var loyalty=new LoyaltyService(hotel);loyalty.configure(admin,new LoyaltyPolicy("TEST-ONLY",1,BigDecimal.ONE,1,LoyaltyPolicy.RefundTreatment.RECALCULATE_RETAINED_PAYMENT,LoyaltyPolicy.SpentTreatment.CARRY_FORWARD_DEBT,LoyaltyPolicy.RedemptionCancellation.RESTORE_ORIGINAL_EXPIRY));
    var serviceLine=service.createService(admin,"Desayuno",new BigDecimal("50"),"COP");service.recharge(admin,"client-1",1000,"Fixture funds");var paid=bookings.create(owner,"client-1","paid",new BookingRequest(101,FROM,FROM.plusDays(2),1,List.of(serviceLine.id()),null));bookings.confirm(owner,paid.id());var cancelled=bookings.create(owner,"client-1","cancelled",new BookingRequest(102,FROM,FROM.plusDays(2),1,List.of(),null));bookings.cancelByHotel(admin,cancelled.id());
    desktop=new DesktopController(hotel);desktop.login("admin@example.com",adminPassword);
    fx(()->{var loader=new FXMLLoader(HotelApplication.class.getResource("shell.fxml"));loader.setControllerFactory(type->new ShellController(desktop));root=loader.load();shell=loader.getController();var scene=new Scene(root,1380,950);scene.getStylesheets().add(HotelApplication.class.getResource("hotel.css").toExternalForm());stage=new Stage();stage.setTitle("HotelUQ · H-05");stage.setScene(scene);stage.show();shell.showAuthenticated();return null;});
    await(()->!shell.actions.busyProperty().get());fx(()->{shell.navigate(DesktopController.Screen.REPORTS);return null;});await(()->!shell.actions.busyProperty().get() && root.lookup("#reportKind")!=null);
  }
  @AfterEach void close() throws Exception {fx(()->{if(shell!=null)shell.close();if(stage!=null)stage.close();return null;});}
  @SuppressWarnings("unchecked") private <T> ComboBox<T> combo(String id){return (ComboBox<T>)root.lookup(id);}
  private void capture(String name) throws Exception {fx(()->{
    root.applyCss();root.layout();var image=stage.getScene().snapshot(null);var out=new BufferedImage((int)image.getWidth(),(int)image.getHeight(),BufferedImage.TYPE_INT_ARGB);var pixels=image.getPixelReader();
    for(int y=0;y<out.getHeight();y++)for(int x=0;x<out.getWidth();x++)out.setRGB(x,y,pixels.getArgb(x,y));Path dir=Path.of("build/h05");Files.createDirectories(dir);ImageIO.write(out,"png",dir.resolve(name+".png").toFile());return null;
  });}
  @ParameterizedTest @EnumSource(ReportQuery.Kind.class)
  void allFamiliesNavigateRenderTablesAndAllThreeChartTypes(ReportQuery.Kind kind) throws Exception {
    fx(()->{this.<ReportQuery.Kind>combo("#reportKind").setValue(kind);LocalDate from=kind==ReportQuery.Kind.OCCUPANCY?FROM:clock.instant().atZone(edu.hoteluq.config.HotelTime.ZONE).toLocalDate();((DatePicker)root.lookup("#reportFrom")).setValue(from);((DatePicker)root.lookup("#reportThrough")).setValue(kind==ReportQuery.Kind.OCCUPANCY?from.plusDays(1):from);((Button)root.lookup("#reportGenerate")).fire();return null;});
    await(()->!shell.actions.busyProperty().get() && root.lookupAll(".chart").stream().anyMatch(n->n instanceof BarChart));
    assertTrue(fx(()->root.lookupAll(".table-view").stream().anyMatch(n->n instanceof TableView<?> t && !t.getItems().isEmpty())));
    fx(()->{((ScrollPane)root.lookup(".scroll-pane")).setVvalue(0.18);return null;});capture(kind.name().toLowerCase(Locale.ROOT)+"-barras");
    fx(()->{var datasets=this.<ReportResult.ChartSpec>combo("#reportDataset");if(kind==ReportQuery.Kind.OCCUPANCY)datasets.setValue(datasets.getItems().getLast());this.<ReportChartView.Type>combo("#reportChartType").setValue(ReportChartView.Type.PIE);return null;});
    assertTrue(fx(()->root.lookupAll(".chart").stream().anyMatch(n->n instanceof PieChart)));if(kind==ReportQuery.Kind.CANCELLATIONS)capture("cancelaciones-pastel");
    fx(()->{this.<ReportChartView.Type>combo("#reportChartType").setValue(ReportChartView.Type.LINE);return null;});assertTrue(fx(()->root.lookupAll(".chart").stream().anyMatch(n->n instanceof LineChart)));if(kind==ReportQuery.Kind.REVENUE)capture("ingresos-lineas");
    var query=new ReportQuery(kind,kind==ReportQuery.Kind.OCCUPANCY?FROM:clock.instant().atZone(edu.hoteluq.config.HotelTime.ZONE).toLocalDate(),kind==ReportQuery.Kind.OCCUPANCY?FROM.plusDays(1):clock.instant().atZone(edu.hoteluq.config.HotelTime.ZONE).toLocalDate());
    var report=desktop.reports.generate(desktop.token(),query);desktop.reports.export(desktop.token(),report,new TxtReportExporter(),Path.of("build/h05/"+kind.name().toLowerCase(Locale.ROOT)+".txt"),true);
  }
  @Test void negativePieIsRejectedRatherThanMisrepresented() throws Exception {
    var spec=new ReportResult.ChartSpec("Negative",ReportResult.Unit.COP,List.of(new ReportResult.Series("Net",List.of(new ReportResult.Point("date",BigDecimal.ONE.negate())))));
    assertThrows(ExecutionException.class,()->fx(()->new ReportChartAdapter().render(spec,ReportChartView.Type.PIE)));
    assertTrue(fx(()->new ReportChartAdapter().render(spec,ReportChartView.Type.BARS) instanceof BarChart));
  }
}
