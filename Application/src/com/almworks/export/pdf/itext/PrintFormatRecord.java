package com.almworks.export.pdf.itext;

import com.almworks.api.application.ExportValueType;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.util.ExportContext;
import com.almworks.api.application.util.ItemExport;
import com.almworks.api.application.viewer.Comment;
import com.almworks.api.engine.Connection;
import com.almworks.api.misc.WorkArea;
import com.almworks.export.ExportParameters;
import com.almworks.export.ExportedData;
import com.almworks.export.pdf.PDFParams;
import com.almworks.util.Pair;
import com.almworks.util.progress.Progress;
import com.almworks.util.properties.PropertyMap;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;
import org.almworks.util.Log;
import util.concurrent.SynchronizedBoolean;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * This file is main collector interface. It contains some hacks to
 * prevent dependency of tracker type.
 *
 * @author Alex
 */
public class PrintFormatRecord {
  private final List<PrintElement<Document>> myPrintList;
  private final ExportedData myData;
  private final ExportParameters myParameters;
  private final WorkArea myWorkArea;

  public PrintFormatRecord(ExportedData data, ExportParameters parameters, WorkArea workArea) {
    myData = data;
    myParameters = parameters;
    myWorkArea = workArea;
    myPrintList = buildList();
  }

  public List<PrintElement<Document>> buildList() {
    final PrintBuilder builder = new PrintBuilder();

    final Set<ItemExport> exportkeySet = new LinkedHashSet<ItemExport>(myParameters.getKeys());
    exportkeySet.remove(myData.getKeyExport());
    exportkeySet.remove(myData.getSummaryExport());

    final Collection<Connection> conns = myData.getConnections();
    ExportContext context = new ExportContext(myParameters.getNumberFormat(), myParameters.getDateFormat(), false);
    for (ItemExport modelKey : exportkeySet) {
      if (modelKey.isExportable(conns)) {
        ExportValueType type = getExportValueType(modelKey, context);
        assert isValueTypeForAllRecords(modelKey, type, context) : type + " " + modelKey;
        if (type == ExportValueType.LARGE_STRING) {
          builder.addLargeField(modelKey);
        } else {
          builder.addAttribute(modelKey);
        }
      }
    }

    if (myParameters.getBoolean(PDFParams.COMMENTS)) {
      final ModelKey<List<Comment>> modelKey = myData.getComments();
      if (modelKey != null) builder.addComments(modelKey);
    }

    builder.setHeader(myData.getKeyExport(), myData.getSummaryExport());
    builder.setTableCompact(myParameters.getBoolean(PDFParams.COMPACT_TABLE));
    builder.setNewFromBlank(myParameters.getBoolean(PDFParams.ON_NEW_PAGE));
    builder.setAttaches(myData.getAttachments(),
      myParameters.getBoolean(PDFParams.ATTACH_GRAPH), myParameters.getBoolean(PDFParams.ATTACH_TEXT));
    return builder.createList();
  }

  private boolean isValueTypeForAllRecords(ItemExport modelKey, ExportValueType type, ExportContext context) {
    for (ExportedData.ArtifactRecord artifactRecord : myData.getRecords()) {
      Pair<String, ExportValueType> pair = modelKey.formatForExport(artifactRecord.getValues(), context);
      if (pair == null)
        continue;
      ExportValueType t = pair.getSecond();
      if (type != null && t != type) {
        Log.warn("export format: " + modelKey.getDisplayName() + " " + type + " " + t);
      }
    }
    return true;
  }

  private ExportValueType getExportValueType(ItemExport modelKey, ExportContext context) {
    List<ExportedData.ArtifactRecord> records = myData.getRecords();
    if (records.isEmpty())
      return ExportValueType.STRING;
    ExportedData.ArtifactRecord first = records.get(0);
    Pair<String,ExportValueType> pair =
      modelKey.formatForExport(first.getValues(), context);
    if (pair == null)
      return ExportValueType.STRING;
    return pair.getSecond();
  }

  public void writePdf(OutputStream outStream, Progress progress, SynchronizedBoolean cancelled) {
    Document document;
    try {
      document = new Document();
      PdfWriter writer = PdfWriter.getInstance(document, outStream);
      ReportMetrics metrics = new ReportMetrics(myWorkArea.getEtcFile(WorkArea.ETC_EXPORT_PROPERTIES));
      writer.setPageEvent(new ReportPageEvent(metrics, myData));
      try {
        document.open();
      } catch (Exception e) {
        throw new IOException(e.getMessage());
      }

      double p = 0.1;
      double step = 0.9 / myData.getRecords().size();

      NumberFormat numberFormat = myParameters.getNumberFormat();
      DateFormat dateFormat = myParameters.getDateFormat();
      ExportContext context = new ExportContext(numberFormat, dateFormat, false);
      for (ExportedData.ArtifactRecord record : myData.getRecords()) {
        PropertyMap values = record.getValues();
        for (PrintElement<Document> printElement : myPrintList) {
          if (cancelled.get()) {
            throw new IOException("Export cancelled");
          }
          printElement.setContext(record.getConnection(), values, context);
          printElement.appendPrintElement(document, metrics, writer);
        }
        p += step;
        progress.setProgress(p);
      }

      try {
        document.close();
      } catch (Exception e) {
        throw new IOException(e.getMessage());
      }
    } catch (DocumentException e) {
      progress.addError(e.getMessage());
    } catch (IOException e) {
      progress.addError(e.getMessage());
    }

    progress.setProgress(1F);
  }
}
