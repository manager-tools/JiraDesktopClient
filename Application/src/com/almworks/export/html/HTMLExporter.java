package com.almworks.export.html;

import com.almworks.api.application.ExportValueType;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.util.ExportContext;
import com.almworks.api.application.util.ItemExport;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.misc.WorkArea;
import com.almworks.export.*;
import com.almworks.util.Pair;
import com.almworks.util.config.Configuration;
import org.almworks.util.Log;
import org.almworks.util.Util;

import java.io.File;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class HTMLExporter extends WriterExporterHelper {
  public static final Pattern CSS_CLASS_PATTERN = Pattern.compile("^\\w[\\w\\d_]+$");
  public static final int MAX_BUGS_PER_PAGE = Integer.MAX_VALUE / 2 - 1;

  public HTMLExporter(ExporterDescriptor descriptor, DialogManager dialogManager, Configuration configuration, WorkArea workArea) {
    super(descriptor, new HTMLParametersForm(configuration, workArea), dialogManager);
  }

  protected void writeData(PrintWriter out, ExportedData data, ExportParameters parameters) {
    writeHtmlHeader(out, data, parameters);
    writeDataTables(out, data, parameters);
    writeHtmlFooter(out, data, parameters);
  }

  private void writeDataTables(PrintWriter out, ExportedData data, ExportParameters parameters) {
    Set<ItemExport> keys = parameters.getKeys();
    List<ExportedData.ArtifactRecord> records = data.getRecords();
    DateFormat dateFormat = parameters.getDateFormat();
    NumberFormat numberFormat = parameters.getNumberFormat();
    Integer p = parameters.get(HTMLParams.BUGS_PER_TABLE);
    int bugsPerTable = p == null ? MAX_BUGS_PER_PAGE : p;
    int size = records.size();
    double step = size == 0 ? 0F : 1F / size;
    int count = 0;
    int totalPages = (size + bugsPerTable - 1) / bugsPerTable;
    ExportContext context = new ExportContext(numberFormat, dateFormat, true);
    for (ExportedData.ArtifactRecord record : records) {
      int page = (count + bugsPerTable) / bugsPerTable;
      if (count % bugsPerTable == 0) {
        writeTableHeader(out, keys, page, totalPages, data, parameters);
      }
      if (myCancelled.get()) {
        myProgress.addError("Export cancelled");
        break;
      }
      String rowClasses = getRowClasses(record, keys, context);
      out.println("<tr>");
      for (ItemExport key : keys) {
        Pair<String, ExportValueType> pair = key.formatForExport(record.getValues(), context);
        ExportValueType valType = pair == null ? null : pair.getSecond();
        String colClasses = getColClasses(key, valType);
        String value = pair == null ? "" : Util.NN(pair.getFirst());
        out.print("   <td class=\"");
        out.print(colClasses);
        if (rowClasses.length() > 0 && colClasses.length() > 0)
          out.print(' ');
        out.print(rowClasses);
        out.print("\">");
        if (value.length() == 0) {
          out.print("&nbsp;");
        } else {
          if (valType != ExportValueType.STRING_HTML) {
            value = escape(value);
          }
          if (valType == ExportValueType.LARGE_STRING) {
            value = value.replaceAll("(\n\r)|(\r\n)|\n", "<br>");
          }
          out.print(value);
        }
        out.println("</td>");
      }
      out.println("</tr>");
      count++;
      if (count % bugsPerTable == 0) {
        writeTableFooter(out);
      }
      myProgress.setProgress((float) step * count);
    }
    if (count % bugsPerTable != 0) {
      writeTableFooter(out);
    }
  }

  private void writeTableFooter(PrintWriter out) {
    out.println("</table>");
    out.println("</div>");
  }

  private void writeTableHeader(PrintWriter out, Set<ItemExport> keys, int page, int totalPages, ExportedData data,
    ExportParameters parameters)
  {
    String firstLast = (page == 1 ? " first" : " notfirst") + (page == totalPages ? " last" : " notlast");
    out.println("<div class=\"page" + firstLast + "\">");
    out.println("<div class=\"header\">");
    out.println("  <h1>" + escape(getTitle(data)) + "</h1>");
    out.println("  <table class=\"headerInfo infoTable\" border=\"0\" cellpadding=\"0\" cellspacing=\"1\">");
    GenericNode node = data.getNode();
    if (node != null) {
      writeInfoRow(out, "Connection", getConnectionName(node), "infoConnection");
      writeInfoRow(out, "Path", getPath(node), "infoPath");
    }
    writeInfoRow(out, "Report Created", getDate(data.getDateCollected(), parameters.getLocale()), "infoDate");
    writeInfoRow(out, "Total", ExportUtils.getTotals(data.getRecords()), "infoTotal");
    writeInfoRow(out, "Page", page + " of " + totalPages, "page");
    out.println("  </table>");
    out.println("</div>");
    String classes = "bugs" + firstLast;
    out.println("<table class=\"" + classes + "\">");
    out.print("<tr>");
    for (ItemExport key : keys) {
      out.print("<th>" + escape(key.getDisplayName()) + "</th>");
    }
    out.println("</tr>");
  }

  private void writeHtmlHeader(PrintWriter out, ExportedData data, ExportParameters parameters) {
    out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
    out.println("<head>");
    out.println("  <title>" + escape(getTitle(data)) + "</title>");
    out.println("  <meta http-equiv=\"Content-type\" content=\"text/html; charset=UTF-8\"/>");
    File cssFile = parameters.get(HTMLParams.CSS_FILE);
    if (cssFile != null && cssFile.getName().length() > 0) {
      try {
        URL url = cssFile.toURI().toURL();
        out.println("  <link rel=\"stylesheet\" type=\"text/css\" href=\"" + url + "\"/>");
      } catch (MalformedURLException e) {
        Log.warn("cannot construct url from " + cssFile);
      }
    }
    out.println("</head>");
    out.println("<body>");
  }

  private void writeInfoRow(PrintWriter out, String name, String value, String css) {
    if (value != null) {
      out.println("  <tr><td class=\"infoLabel\">" + name + ":</td><td class=\"infoData " + css + "\">" +
        escape(value) + "</td></tr>");
    }
  }

  private void writeHtmlFooter(PrintWriter out, ExportedData data, ExportParameters parameters) {
    out.println("<div class=\"footer\">");
    out.println("  <table class=\"footerInfo infoTable\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">");
    writeInfoRow(out, "Total", ExportUtils.getTotals(data.getRecords()), "infoTotal");
    out.println("  </table>");
    out.println("</div>");
    out.println("</body>");
    out.println("</html>");
  }

  private String getColClasses(ItemExport key, ExportValueType type) {
    StringBuffer result = new StringBuffer();
    String name = Util.lower(key.getDisplayName());
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if (Character.isLetter(c) || c == '_')
        result.append(c);
    }
    if (type != null) {
      if (result.length() > 0)
        result.append(' ');
      result.append(Util.lower(type.name()));
    }
    return result.toString();
  }

  private String getRowClasses(ExportedData.ArtifactRecord record, Set<ItemExport> keys, ExportContext context) {
    StringBuffer result = new StringBuffer();
    for (ItemExport key : keys) {
      Pair<String, ExportValueType> pair = key.formatForExport(record.getValues(), context);
      if (pair == null)
        continue;
      if (pair.getSecond() != ExportValueType.STRING  && pair.getSecond() != ExportValueType.LARGE_STRING)
        continue;
      String value = pair.getFirst();
      if (CSS_CLASS_PATTERN.matcher(value).matches()) {
        if (result.length() > 0)
          result.append(' ');
        result.append(Util.lower(value));
      }
    }
    return result.toString();
  }
}
