package edu.hoteluq.ui;

import edu.hoteluq.report.ReportResult.ChartSpec;
import javafx.scene.Node;

/** Target presentation port: controllers do not depend on XYChart/PieChart data contracts. */
public interface ReportChartView {
  enum Type { BARS("Barras"),PIE("Pastel"),LINE("Líneas");private final String title;Type(String title){this.title=title;}public String toString(){return title;}}
  Node render(ChartSpec spec,Type type);
}
