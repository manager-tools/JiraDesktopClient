package com.almworks.export.csv;

import com.almworks.api.application.ExportValueType;
import com.almworks.api.application.util.ExportContext;
import com.almworks.api.application.util.ItemExport;
import com.almworks.api.gui.DialogManager;
import com.almworks.export.ExportParameters;
import com.almworks.export.ExportedData;
import com.almworks.export.ExporterDescriptor;
import com.almworks.export.WriterExporterHelper;
import com.almworks.util.Pair;
import com.almworks.util.config.Configuration;
import com.almworks.util.properties.PropertyMap;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Set;

public class CSVExporter extends WriterExporterHelper {

  public CSVExporter(ExporterDescriptor descriptor, Configuration config, DialogManager dialogManager) {
    super(descriptor, new CSVParametersForm(config), dialogManager);
  }

  protected String getExportCharset(ExportParameters parameters) {
    Charset encoding = parameters.get(CSVParams.CHARSET);
    if (encoding == null) encoding = getDefaultCharset();
    return encoding.name();
  }

  @NotNull
  public static Charset getDefaultCharset() {
    String encoding = System.getProperty("file.encoding");
    if (encoding != null) {
      try {
        Charset charset = Charset.forName(encoding);
        if (charset != null) return charset;
        charset = Charset.forName(DEFAULT_ENCODING);
        if (charset != null) return charset;
      } catch (Exception e) {
        Log.debug("not using " + encoding + " for export", e);
      }
    }
    return Charset.defaultCharset();
  }

  protected void writeData(PrintWriter out, ExportedData data, ExportParameters parameters) {
    boolean quotesAlways = Util.NN(parameters.get(CSVParams.QUOTES_ALWAYS), false);
    char delimiter = getDelimiter(parameters);
    boolean protectFormula = Util.NN(parameters.get(CSVParams.PROTECT_FORMULA), false);
    DateFormat dateFormat = parameters.getDateFormat();
    NumberFormat numberFormat = parameters.getNumberFormat();
    Set<ItemExport> keys = parameters.getKeys();
    if (keys.size() == 0) {
      Log.warn("no keys for export");
      return;
    }

    if (Util.NN(parameters.get(CSVParams.OUTPUT_HEADER), false))
      writeHeader(out, keys, delimiter);
    List<ExportedData.ArtifactRecord> records = data.getRecords();
    int size = records.size();
    double step = size == 0 ? 0F : 1F / size;
    int count = 0;
    ExportContext context = new ExportContext(numberFormat, dateFormat, false);
    for (ExportedData.ArtifactRecord record : records) {
      if (myCancelled.get()) {
        myProgress.addError("Export cancelled");
        break;
      }
      PropertyMap values = record.getValues();
      StringBuffer line = new StringBuffer();
      boolean first = true;
      for (ItemExport key : keys) {
        if (first)
          first = false;
        else
          line.append(delimiter);
        Pair<String, ExportValueType> p = key.formatForExport(values, context);
        String formatted = p == null ? "" : p.getFirst();
        ExportValueType type = p == null ? ExportValueType.STRING : p.getSecond();
        String noLineSeps = formatted.replaceAll("(\n\r)|(\r\n)|\n", " ");
        String escaped = CSVUtil.escapeString(noLineSeps, type, quotesAlways, delimiter, protectFormula);
        line.append(escaped);
      }
      out.println(line);
      count++;
      myProgress.setProgress((float) step * count);
    }
  }

  private char getDelimiter(ExportParameters parameters) {
    Character c = parameters.get(CSVParams.DELIMITER_CHAR);
    char delimiter = (c == null || c == 0) ? CSVUtil.guessDefaultListSeparator() : c;
    return delimiter;
  }

  private void writeHeader(PrintWriter out, Set<ItemExport> keys, char delimiter) {
    StringBuffer buf = new StringBuffer();
    for (ItemExport key : keys) {
      String name = key.getDisplayName();
      if (buf.length() > 0)
        buf.append(delimiter);
      buf.append('"').append(name).append('"');
    }
    out.println(buf);
  }
}