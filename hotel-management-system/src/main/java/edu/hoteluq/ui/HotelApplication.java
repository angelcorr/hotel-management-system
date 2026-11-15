package edu.hoteluq.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import edu.hoteluq.singleton.Hotel;

public final class HotelApplication extends Application {
  private ShellController shell;
  @Override public void start(Stage stage) throws Exception {
    Hotel aggregate=Hotel.getInstance();boolean demo=getParameters().getRaw().contains("--demo");
    if(demo){var input=DemoSetupDialog.ask();if(input.isEmpty()){javafx.application.Platform.exit();return;}
      try{aggregate=edu.hoteluq.singleton.DemoDataFactory.create(input.get()).hotel();}
      catch(RuntimeException ex){new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,UiText.error(ex)).showAndWait();javafx.application.Platform.exit();return;}}
    Hotel selected=aggregate;FXMLLoader loader=new FXMLLoader(getClass().getResource("shell.fxml"));
    loader.setControllerFactory(type->new ShellController(new DesktopController(selected)));
    Scene scene=new Scene(loader.load(),1280,850);shell=loader.getController();scene.getStylesheets().add(getClass().getResource("hotel.css").toExternalForm());
    stage.setTitle(demo?"HotelUQ · DEMOSTRACIÓN AISLADA · valores ficticios":"HotelUQ · Gestión hotelera");stage.setMinWidth(1000);stage.setMinHeight(720);stage.setScene(scene);stage.show();
    if(demo)shell.message("DEMOSTRACIÓN AISLADA · fixtures no aprobados · se pierde/restablece al reiniciar --demo");
    if(getParameters().getRaw().contains("--smoke-ui")) {
      var wait=new javafx.animation.PauseTransition(javafx.util.Duration.millis(400));
      wait.setOnFinished(event->{try{FxCapture.save(scene,java.nio.file.Path.of("build/h04/lanzamiento.png"));System.out.println("HOTELUQ_UI_STARTED: JavaFX stage shown; FXML/CSS loaded; startup scene captured");}
        catch(java.io.IOException ex){throw new IllegalStateException(ex);}finally{javafx.application.Platform.exit();}});wait.play();
    }
  }
  @Override public void stop() {if(shell!=null)shell.close();}
}
