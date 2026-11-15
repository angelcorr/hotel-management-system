package edu.hoteluq.ui;

import edu.hoteluq.singleton.DemoDataFactory;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.Optional;

/** Opt-in isolated fixtures with user-supplied secrets; never displayed in screenshots/logs. */
final class DemoSetupDialog {
  private DemoSetupDialog(){}
  static Optional<DemoDataFactory.Credentials> ask(){
    Dialog<DemoDataFactory.Credentials> dialog=new Dialog<>();dialog.setTitle("HotelUQ · demostración aislada");
    var email=Ui.field("Correo del administrador demo");var admin=new PasswordField();admin.setPromptText("Contraseña administrador (mínimo 12)");var client=new PasswordField();client.setPromptText("Contraseña cliente demo distinta (mínimo 12)");var consent=new CheckBox("Entiendo que son fixtures académicos, no reglas comerciales aprobadas");var error=Ui.label("");
    dialog.getDialogPane().setContent(new VBox(12,Ui.title("Solo demostración"),Ui.label("Se crea otra instancia en memoria, sin tocar el hotel normal. Se reproducen pagos y check-out con un reloj de fixtures y luego continúa el reloj real. Cliente: cliente-demo@hoteluq.invalid.\nValores ilustrativos: 100 puntos/100.000 COP; descuento demo 10 %; beneficios 50/100 puntos. Reiniciar --demo restablece todo. No existen contraseñas predefinidas."),email,admin,client,consent,error));
    dialog.getDialogPane().setPrefWidth(620);dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);
    dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION,event->{try{if(!consent.isSelected())throw new IllegalArgumentException("Confirme que leyó la advertencia");new DemoDataFactory.Credentials(FormInput.required(email.getText(),"correo"),admin.getText(),client.getText());}catch(RuntimeException ex){error.setText(ex.getMessage());event.consume();}});
    dialog.setResultConverter(button->button==ButtonType.OK?new DemoDataFactory.Credentials(email.getText(),admin.getText(),client.getText()):null);return dialog.showAndWait();
  }
}
