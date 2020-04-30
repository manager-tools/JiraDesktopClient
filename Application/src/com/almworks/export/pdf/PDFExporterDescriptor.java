package com.almworks.export.pdf;

import com.almworks.api.gui.DialogManager;
import com.almworks.api.misc.WorkArea;
import com.almworks.export.*;
import com.almworks.util.config.Configuration;
import org.jetbrains.annotations.NotNull;

public class PDFExporterDescriptor implements ExporterDescriptor {
  private final DialogManager myDialogManager;
  private final WorkArea myWorkArea;

  public PDFExporterDescriptor(DialogManager dialogManager, WorkArea wa) {
    myDialogManager = dialogManager;
    myWorkArea = wa;
  }

  @NotNull
  public String getKey() {
    return "com.almworks.export.pdf";
  }

  @NotNull
  public String getDisplayableName() {
    return "PDF file";
  }

  @NotNull
  public Exporter createExporter(Configuration configuration, ExportParameters target, ExportedData data)
    throws ExporterNotApplicableException
  {
    return new PDFExporter(this, configuration, myDialogManager, myWorkArea);
  }
}
