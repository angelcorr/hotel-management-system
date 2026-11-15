package edu.hoteluq;

/** Separate launcher avoids the JDK special launcher path when executing a packaged JavaFX app. */
public final class Main {
  private Main() {}
  public static void main(String[] args) {javafx.application.Application.launch(edu.hoteluq.ui.HotelApplication.class,args);}
}
