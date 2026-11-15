package edu.hoteluq.ui;

import javafx.scene.Scene;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Diagnostic scene-only capture; never captures other applications or seeds data. */
final class FxCapture {
  private FxCapture() {}
  static void save(Scene scene,Path file) throws java.io.IOException {
    var image=scene.snapshot(null);var pixels=image.getPixelReader();
    var out=new BufferedImage((int)image.getWidth(),(int)image.getHeight(),BufferedImage.TYPE_INT_ARGB);
    for(int y=0;y<out.getHeight();y++)for(int x=0;x<out.getWidth();x++)out.setRGB(x,y,pixels.getArgb(x,y));
    Files.createDirectories(file.getParent());ImageIO.write(out,"png",file.toFile());
  }
}
