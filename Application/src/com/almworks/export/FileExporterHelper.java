package com.almworks.export;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.Engine;
import com.almworks.api.gui.DialogManager;
import com.almworks.export.csv.CSVParams;
import com.almworks.util.Terms;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.Local;
import com.almworks.util.io.IOUtils;
import com.almworks.util.progress.Progress;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.xml.JDOMUtils;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import util.concurrent.SynchronizedBoolean;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public abstract class FileExporterHelper implements Exporter {
  protected final SynchronizedBoolean myCancelled = new SynchronizedBoolean(false);
  protected final ExporterDescriptor myDescriptor;
  protected final DialogManager myDialogManager;
  protected final FileExporterUIHelper myForm;
  protected final Progress myProgress = new Progress();

  protected FileExporterHelper(ExporterDescriptor descriptor, FileExporterUIHelper form, DialogManager dialogManager) {
    myDescriptor = descriptor;
    myForm = form;
    myDialogManager = dialogManager;
  }

  public void export(@NotNull final ExportedData data, @NotNull final ExportParameters parameters, boolean silent) {
    ExportInProgressForm progressForm = new ExportInProgressForm(myDialogManager, silent);
    progressForm.setTargetFile(parameters.get(CSVParams.TARGET_FILE));
    progressForm.setProgress(myProgress);
    progressForm.setCancelAction(new CancelExportAction(myProgress, myCancelled));
    progressForm.setDoneActions(ExportUtils.createDoneActions(parameters));
    progressForm.show();
    ThreadGate.LONG(this).execute(new Runnable() {
      public void run() {
        doExport(data, parameters);
      }
    });
  }

  @NotNull
  public ExporterDescriptor getDescriptor() {
    return myDescriptor;
  }

  @NotNull
  public ExporterUI getUI() {
    return myForm;
  }

  public void confirmExport() throws CantPerformException {
    // todo join Exporter and exporter UI?
    myForm.confirmExport();
  }

  protected abstract void writeData(OutputStream out, ExportedData data, ExportParameters parameters) throws IOException;

  protected final void doExport(ExportedData data, ExportParameters parameters) {
    myProgress.setProgress(0.05);
    File file = parameters.get(CSVParams.TARGET_FILE);
    FileOutputStream fos = null;
    try {
      mkdirs(file);
      fos = new FileOutputStream(file);
      writeData(fos, data, parameters);
    } catch (IOException e) {
      myProgress.addError(e.getMessage());
      Log.debug("export error", e);
    } finally {
      IOUtils.closeStreamIgnoreExceptions(fos);
      finished();
    }
  }

  private void mkdirs(File file) throws IOException {
    final File dir = file.getParentFile();
    if(!dir.exists()) {
      if(!dir.mkdirs()) {
        throw new IOException("Cannot create directory " + dir.getCanonicalPath());
      }
    }
  }

  private void finished() {
    try {
      myProgress.setProgress(1F, "");
    } catch (Exception e) {
      assert false : e;
      Log.debug("ignoring ", e);
    }
  }

  protected String getTitle(ExportedData data) {
    return Util.NN(data.getCollectionName(), Local.parse(Terms.ref_Deskzilla + " Query"));
  }

  protected String getDate(Date dateCollected, Locale locale) {
    DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale);
    return format.format(dateCollected);
  }

  protected String getPath(GenericNode node) {
    StringBuffer result = new StringBuffer();
    while (node != null) {
      String name = node.getName();
      if (name == null || name.trim().length() == 0) {
        name = ".";
      }
      if (result.length() > 0)
        result.insert(0, " - ");
      result.insert(0, name);
      node = node.getParent();
    }
    return result.length() == 0 ? null : result.toString();
  }

  protected String getConnectionName(GenericNode node) {
    Connection connection = node.getConnection();
    if (connection == null)
      return null;
    Engine engine = connection.getConnectionContainer().getActor(Engine.ROLE);
    if (engine == null)
      return null;
    String name = engine.getConnectionManager().getConnectionName(connection.getConnectionID());
    return name;
  }

  protected String escape(String s) {
    return JDOMUtils.escapeXmlEntities(s);
  }
}