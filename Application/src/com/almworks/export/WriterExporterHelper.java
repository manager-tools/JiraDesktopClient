package com.almworks.export;

import com.almworks.api.gui.DialogManager;
import com.almworks.util.io.IOUtils;

import java.io.*;

public abstract class WriterExporterHelper extends FileExporterHelper {

  public static final String DEFAULT_ENCODING = "UTF-8";

  protected WriterExporterHelper(ExporterDescriptor descriptor, FileExporterUIHelper form,
    DialogManager dialogManager)
  {
    super(descriptor, form, dialogManager);
  }

  protected abstract void writeData(PrintWriter out, ExportedData data, ExportParameters parameters);

  protected final void writeData(OutputStream stream, ExportedData data, ExportParameters parameters)
    throws IOException
  {
    PrintWriter out = null;
    try {
      OutputStreamWriter writer = new OutputStreamWriter(stream, getExportCharset(parameters));
      out = new PrintWriter(new BufferedWriter(writer), false);
      writeData(out, data, parameters);
    } finally {
      IOUtils.closeWriterIgnoreExceptions(out);
    }
  }

  protected String getExportCharset(ExportParameters parameters) {
    return DEFAULT_ENCODING;
  }
}