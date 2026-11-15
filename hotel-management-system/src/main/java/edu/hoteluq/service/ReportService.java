package edu.hoteluq.service;

import edu.hoteluq.singleton.Hotel;
import edu.hoteluq.report.*;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Objects;

/** Admin-only reporting facade. Calculations use a single consistent cut, exports the displayed cut. */
public final class ReportService {
  private final Hotel hotel;
  public ReportService(){this(Hotel.getInstance());}
  public ReportService(Hotel hotel){this.hotel=Objects.requireNonNull(hotel);}
  public ReportResult generate(String token,ReportQuery query){return hotel.withSession(token,()->new ReportEngine().generate(hotel.reportSnapshot(),Objects.requireNonNull(query)));}
  public byte[] render(String token,ReportResult report,ReportExporter exporter){return hotel.withSession(token,()->{hotel.authorizeReports();return exporter.render(report);});}
  public Path export(String token,ReportResult report,ReportExporter exporter,Path path,boolean replace) throws IOException {
    // Authorization and writing share the session scope/monitor; logout cannot race an unchecked write.
    try{return hotel.withSession(token,()->{hotel.authorizeReports();try{return ReportFiles.write(path,exporter.render(report),replace);}catch(IOException ex){throw new java.io.UncheckedIOException(ex);}});}
    catch(java.io.UncheckedIOException ex){throw ex.getCause();}
  }
}
