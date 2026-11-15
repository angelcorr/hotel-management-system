package edu.hoteluq.ui;

import javafx.application.Platform;
import javafx.beans.property.*;
import java.util.concurrent.*;
import java.util.function.*;

/** One background command at a time; all JavaFX mutations are delivered on its application thread. */
public final class FxActions implements AutoCloseable {
  private final ExecutorService executor=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"hoteluq-desktop");t.setDaemon(true);return t;});
  private final BooleanProperty busy=new SimpleBooleanProperty();
  private final DesktopController controller;
  private final Consumer<Throwable> errors;
  public FxActions(DesktopController controller,Consumer<Throwable> errors) {this.controller=controller;this.errors=errors;}
  public ReadOnlyBooleanProperty busyProperty() {return busy;}
  public <T> void run(Supplier<T> operation,Consumer<T> success) {
    if(busy.get())return;
    busy.set(true);
    executor.submit(()->{
      T value=null;Throwable failure=null;
      try{value=controller.execute(operation);}catch(Throwable ex){failure=ex;}
      T result=value;Throwable error=failure;
      Platform.runLater(()->{busy.set(false);if(error!=null)errors.accept(error);else try{success.accept(result);}catch(RuntimeException ex){errors.accept(ex);}});
    });
  }
  public void close() {executor.shutdownNow();}
}
