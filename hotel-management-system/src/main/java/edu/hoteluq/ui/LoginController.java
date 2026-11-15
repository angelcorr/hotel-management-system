package edu.hoteluq.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import edu.hoteluq.exceptions.HotelException;

public final class LoginController {
  enum Mode { SETUP,LOGIN,REGISTER }
  private final ShellController shell;
  private final Mode mode;
  @FXML private Label heading,explanation;
  @FXML private VBox profile;
  @FXML private TextField id,name,email;
  @FXML private PasswordField password,repeat;
  @FXML private Button submit,switchMode;
  LoginController(ShellController shell,Mode mode) {this.shell=shell;this.mode=mode;}
  @FXML private void initialize() {
    boolean login=mode==Mode.LOGIN;profile.setVisible(!login);profile.setManaged(!login);repeat.setVisible(!login);repeat.setManaged(!login);
    heading.setText(mode==Mode.SETUP?"Bienvenido a HotelUQ":login?"Iniciar sesión":"Crear cuenta de cliente");
    explanation.setText(mode==Mode.SETUP?"Primer arranque: defina el administrador local. Después configure horarios y catálogos antes de aceptar reservas.":"Use su identificación y correo propios. Los permisos se verifican en los servicios.");
    submit.setText(mode==Mode.SETUP?"Inicializar e ingresar":login?"Ingresar":"Registrarme");switchMode.setText(login?"Crear cuenta":"Volver al ingreso");switchMode.setVisible(mode!=Mode.SETUP);switchMode.setManaged(mode!=Mode.SETUP);
    password.setOnAction(e->submit());
  }
  @FXML private void switchMode() {shell.showLogin(mode==Mode.LOGIN?Mode.REGISTER:Mode.LOGIN);}
  @FXML private void submit() {shell.safe(()->{
    String mail=FormInput.required(email.getText(),"correo"),secret=password.getText(),client=id.getText(),full=name.getText();
    if(mode!=Mode.LOGIN && !secret.equals(repeat.getText()))throw new HotelException("Las contraseñas no coinciden");
    shell.actions.run(()->{if(mode==Mode.SETUP)shell.controller.setup(client,full,mail,secret);else if(mode==Mode.LOGIN)shell.controller.login(mail,secret);else shell.controller.hotel.register(client,full,mail,secret);return true;},v->{password.clear();repeat.clear();if(mode==Mode.REGISTER){shell.showLogin(Mode.LOGIN);shell.message("Cuenta creada. Ya puede iniciar sesión.");}else shell.showAuthenticated();});
  });}
}
