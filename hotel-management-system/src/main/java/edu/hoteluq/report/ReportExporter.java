package edu.hoteluq.report;

/** Format adapter contract: exporters consume the same calculated report, never recompute business totals. */
public interface ReportExporter {
  String extension();
  byte[] render(ReportResult report);
}
