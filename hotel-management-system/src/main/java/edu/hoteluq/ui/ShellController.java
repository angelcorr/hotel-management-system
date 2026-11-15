package edu.hoteluq.ui;

import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.io.IOException;
import java.util.function.*;

public final class ShellController {
  public final DesktopController controller;
  public FxActions actions;
  @FXML private BorderPane root;
  @FXML private VBox navigation;
  @FXML private StackPane content;
  @FXML private Label identity,status;
  @FXML private Button logout;
  public ShellController(DesktopController controller) {this.controller=controller;}
  @FXML private void initialize() {
    actions=new FxActions(controller,this::error);root.disableProperty().bind(actions.busyProperty());
    showLogin(controller.hotel.initialized()?LoginController.Mode.LOGIN:LoginController.Mode.SETUP);
  }
  void showLogin(LoginController.Mode mode) {
    navigation.getChildren().clear();identity.setText("Acceso seguro");logout.setVisible(false);
    try {FXMLLoader loader=new FXMLLoader(getClass().getResource("login.fxml"));loader.setControllerFactory(type->new LoginController(this,mode));content.getChildren().setAll((Node)loader.load());}
    catch(IOException ex){throw new IllegalStateException("Cannot load login.fxml",ex);}
  }
  public void showAuthenticated() {
    identity.setText(controller.user().name()+" · "+controller.user().role());logout.setVisible(true);navigation.getChildren().clear();
    for(var screen:controller.navigation())navigation.getChildren().add(Ui.button(screen.title,()->navigate(screen)));
    navigate(controller.screen());
  }
  public void navigate(DesktopController.Screen screen) {
    actions.run(()->{controller.navigate(screen);return screen;},selected->{
      message("Sesión activa · Datos exclusivamente en memoria");
      switch(selected) {
        case SEARCH -> new BookingScreen(this).show();
        case BOOKINGS -> new ClientScreens(this).bookings();
        case CARD -> new ClientScreens(this).card();
        case POINTS -> new ClientScreens(this).points();
        case PROMOTIONS -> new CommercialScreens(this).promotions();
        case REPORTS -> new ReportScreen(this).show();
        case PARAMETERS -> new CommercialScreens(this).parameters();
        default -> new AdminScreens(this).show(selected);
      }
    });
  }
  public void display(Node view) {content.getChildren().setAll(view);}
  public void safe(Runnable task) {try{task.run();}catch(RuntimeException ex){error(ex);}}
  public void message(String text) {status.getStyleClass().removeAll("error","success");status.getStyleClass().add("success");status.setText(text);}
  public void error(Throwable ex) {status.getStyleClass().removeAll("error","success");status.getStyleClass().add("error");status.setText(UiText.error(ex));if(ex instanceof edu.hoteluq.exceptions.AuthenticationException && !controller.authenticated() && controller.hotel.initialized())showLogin(LoginController.Mode.LOGIN);}
  @FXML public void logout() {actions.run(()->{controller.logout();return true;},v->{showLogin(LoginController.Mode.LOGIN);message("Sesión cerrada");});}
  public void close() {actions.close();controller.logout();}
}
