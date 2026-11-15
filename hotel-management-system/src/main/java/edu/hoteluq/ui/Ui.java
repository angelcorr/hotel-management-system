package edu.hoteluq.ui;

import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import java.util.*;
import java.util.function.*;
import edu.hoteluq.exceptions.HotelException;
import edu.hoteluq.model.BookingRecords.Quote;

final class Ui {
  private Ui() {}
  record Column<T>(String title,Function<T,?> value) {}
  private static String display(Object value) {return value instanceof Boolean b?(b?"Sí":"No"):Objects.toString(value,"—");}
  static DatePicker date(java.time.LocalDate initial) {
    DatePicker picker=new DatePicker(initial);picker.setEditable(false);
    var format=java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT);
    picker.setConverter(new javafx.util.StringConverter<>() {
      public String toString(java.time.LocalDate value){return value==null?"":format.format(value);}
      public java.time.LocalDate fromString(String value){return value==null || value.isBlank()?null:java.time.LocalDate.parse(value,format);}
    });picker.setPromptText("DD/MM/AAAA");return picker;
  }
  static Label label(String text) {Label l=new Label(text);l.setWrapText(true);return l;}
  static Label title(String text) {Label l=label(text);l.getStyleClass().add("page-title");return l;}
  static TextField field(String prompt) {TextField f=new TextField();f.setPromptText(prompt);return f;}
  static Button button(String text,Runnable action) {Button b=new Button(text);b.setOnAction(e->action.run());return b;}
  static Button primary(String text,Runnable action) {Button b=button(text,action);b.getStyleClass().add("primary");return b;}
  static VBox page(String heading,Node... nodes) {VBox box=new VBox(14);box.getStyleClass().add("page");box.getChildren().add(title(heading));box.getChildren().addAll(nodes);return box;}
  static HBox row(Node... nodes) {HBox row=new HBox(10,nodes);row.getStyleClass().add("form-row");return row;}
  static VBox input(String label,Node field) {return new VBox(5,label(label),field);}
  static ScrollPane scroll(Node node) {ScrollPane s=new ScrollPane(node);s.setFitToWidth(true);return s;}
  static <T> ComboBox<T> combo(List<T> values) {return new ComboBox<>(FXCollections.observableArrayList(values));}
  static <T> ListView<T> choices(List<T> values) {ListView<T> l=new ListView<>(FXCollections.observableArrayList(values));l.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);l.setPrefHeight(115);return l;}
  @SafeVarargs static <T> TableView<T> table(List<T> items,Column<T>... columns) {
    TableView<T> t=new TableView<>(FXCollections.observableArrayList(items));t.setPrefHeight(260);t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    for(var c:columns) {TableColumn<T,String> column=new TableColumn<>(c.title());column.setCellValueFactory(cell->new ReadOnlyStringWrapper(display(c.value().apply(cell.getValue()))));t.getColumns().add(column);}
    t.setPlaceholder(label("No hay datos para mostrar"));return t;
  }
  static <T> T selected(TableView<T> table) {T value=table.getSelectionModel().getSelectedItem();if(value==null)throw new HotelException("Seleccione una fila primero");return value;}
  static <T> T selected(ComboBox<T> combo,String name) {T value=combo.getValue();if(value==null)throw new HotelException("Seleccione: "+name);return value;}
  static <T> void names(ComboBox<T> combo,Function<T,String> name) {
    combo.setConverter(new javafx.util.StringConverter<>(){public String toString(T value){return value==null?"":name.apply(value);}public T fromString(String value){return null;}});
  }
  static <T> void names(ListView<T> list,Function<T,String> name) {list.setCellFactory(v->new ListCell<>(){protected void updateItem(T item,boolean empty){super.updateItem(item,empty);setText(empty||item==null?null:name.apply(item));}});}
  static boolean confirm(String heading,String message) {Alert a=new Alert(Alert.AlertType.CONFIRMATION,message,ButtonType.CANCEL,ButtonType.OK);a.setTitle("HotelUQ · confirmar");a.setHeaderText(heading);return a.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK;}
  static String quote(Quote q) {
    return "Habitación "+q.roomNumber()+" · "+q.typeName()+" · "+q.guests()+" huésped(es)\n"+q.checkIn()+" → "+q.checkOut()+"\nSubtotal: "+UiText.cop(q.subtotal())+"\nDescuento: "+UiText.cop(q.discount())+"\nTOTAL: "+UiText.cop(q.total())+"\nServicios: "+q.services().stream().map(s->s.name()).toList()+
        (q.promotionDefinition()==null?"":"\nPromoción v"+q.promotionDefinition().version()+": "+q.promotionName()+"\nCondiciones: "+q.promotionDefinition().settings().conditions());
  }
}
