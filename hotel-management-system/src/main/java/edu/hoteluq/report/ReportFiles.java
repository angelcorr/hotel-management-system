package edu.hoteluq.report;

import java.nio.file.*;
import java.io.IOException;

/** Export to a completed temporary sibling first. Default never replaces an existing file. */
public final class ReportFiles {
  private ReportFiles(){}
  public static Path write(Path target,byte[] content,boolean replace) throws IOException {
    Path absolute=target.toAbsolutePath();Path directory=absolute.getParent();
    Path temp=Files.createTempFile(directory,".hoteluq-report-",".tmp");
    try{Files.write(temp,content,StandardOpenOption.TRUNCATE_EXISTING);
      if(replace)Files.move(temp,absolute,StandardCopyOption.REPLACE_EXISTING);else Files.move(temp,absolute);
      return absolute;
    }finally{Files.deleteIfExists(temp);}
  }
}
