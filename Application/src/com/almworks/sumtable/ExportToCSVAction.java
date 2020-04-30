package com.almworks.sumtable;

import com.almworks.api.application.ExportValueType;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.DialogResult;
import com.almworks.export.CancelExportAction;
import com.almworks.export.ExportInProgressForm;
import com.almworks.export.ExportUtils;
import com.almworks.export.csv.CSVParametersForm;
import com.almworks.export.csv.CSVParams;
import com.almworks.export.csv.CSVUtil;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.images.Icons;
import com.almworks.util.io.IOUtils;
import com.almworks.util.progress.Progress;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Log;
import util.concurrent.SynchronizedBoolean;

import java.io.*;
import java.util.List;

public class ExportToCSVAction extends SimpleAction {
  public ExportToCSVAction() {
    super("", Icons.ACTION_EXPORT);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Export to CSV");
    watchRole(SummaryTableData.DATA);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.setEnabled(EnableState.INVISIBLE);
    SummaryTableData data = context.getSourceObject(SummaryTableData.DATA);
    if (data != null) {
      context.setEnabled(data.isDataAvailable() && data.getCounters().size() > 0);
    }
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    try {
      final SummaryTableData data = context.getSourceObject(SummaryTableData.DATA);
      if (data == null || !data.isDataAvailable())
        return;
      DialogManager dialogManager = context.getSourceObject(DialogManager.ROLE);
      final Params params = askParams(dialogManager);
      if (params != null) {
        final Progress progress = new Progress();
        final SynchronizedBoolean cancelled = new SynchronizedBoolean(false);

        setupProgressForm(dialogManager, params, progress, cancelled);

        ThreadGate.LONG(this).execute(new Runnable() {
          public void run() {
            export(params, data, progress, cancelled);
          }
        });
      }
    } catch (CantPerformException e) {
      // ignore
    }
  }

  private static void setupProgressForm(DialogManager dialogManager, Params params, Progress progress, SynchronizedBoolean cancelled) {
    ExportInProgressForm progressForm = new ExportInProgressForm(dialogManager, false);
    progressForm.setTargetFile(params.getTargetFile());
    progressForm.setProgress(progress);
    progressForm.setCancelAction(new CancelExportAction(progress, cancelled));
    progressForm.setDoneActions(ExportUtils.createDoneActions(params));
    progressForm.show();
  }

  private Params askParams(DialogManager dialogManager) throws CantPerformException {
    DialogBuilder builder = dialogManager.createBuilder("sumtable.export.csv");
    CSVParametersForm form = new CSVParametersForm(builder.getConfiguration());
    form.setExtraOptionsVisible(false);
    builder.setContent(form);
    builder.setTitle("Export to CSV");
    DialogResult dr = new DialogResult(builder);
    dr.setOkResult("ok").setCancelResult("cancel");
    dr.pack();
    Object r = dr.showModal();
    if (!"ok".equals(r))
      return null;
    Params result = new Params();
    form.addParametersTo(result);
    form.confirmExport();
    return result;
  }

  private boolean export(Params params, SummaryTableData data, Progress progress, SynchronizedBoolean cancelled) {
    File file = params.getTargetFile();
    if (file == null)
      return false;
    FileOutputStream fos = null;
    PrintWriter out = null;
    try {
      if (cancelled.get())
        return false;
      fos = new FileOutputStream(file);
      if (cancelled.get())
        return false;
      OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
      out = new PrintWriter(new BufferedWriter(writer), false);
      writeData(out, params, data, progress, cancelled);
    } catch (IOException e) {
      Log.debug("export error", e);
      progress.addError(e.getMessage());
      return false;
    } finally {
      IOUtils.closeWriterIgnoreExceptions(out);
      IOUtils.closeStreamIgnoreExceptions(fos);
      progress.setDone();
    }
    return true;
  }

  private void writeData(PrintWriter out, Params params, SummaryTableData data, Progress progress,
    SynchronizedBoolean cancelled) throws IOException
  {
    char delimiter = params.getDelimiter();

    List<STFilter> columns = data.getColumns();
    List<STFilter> rows = data.getRows();
    List<STFilter> counters = data.getCounters();

    writeColumnsHeader(out, columns, delimiter);
    out.println();
    int rowCount = rows.size();
    double step = rowCount == 0 ? 0.0 : 1.0 / rowCount;
    double p = 0.0;
    for (int ri = 0; ri < rowCount; ri++) {
      for (int k = 0; k < counters.size(); k++) {
        if (k == 0) {
          writeFilter(out, rows.get(ri), delimiter);
        }
        for (int ci = 0; ci < columns.size(); ci++) {
          Integer value = data.getCellCount(k, ri, ci);
          writeValue(out, value, delimiter);
        }
        out.println();
      }
      progress.setProgress(p += step);
    }
  }

  private void writeColumnsHeader(PrintWriter out, List<STFilter> columns, char delimiter) {
    for (STFilter column : columns) {
      out.print(delimiter);
      writeFilter(out, column, delimiter);
    }
  }

  private void writeFilter(PrintWriter out, STFilter filter, char delimiter) {
    printEscaped(out, filter.getName(), delimiter);
  }

  private void printEscaped(PrintWriter out, String string, char delimiter) {
    String escaped = CSVUtil.escapeString(string, ExportValueType.STRING, false, delimiter, true);
    out.print(escaped);
  }

  private void writeValue(PrintWriter out, Integer value, char delimiter) {
    out.print(delimiter);
    if (value != null) {
      out.print(value);
    }
  }


  public static class Params extends PropertyMap {
    public File getTargetFile() {
      return get(CSVParams.TARGET_FILE);
    }

    public char getDelimiter() {
      Character c = get(CSVParams.DELIMITER_CHAR);
      return (c == null || c == 0) ? CSVUtil.guessDefaultListSeparator() : c;
    }
  }
}
