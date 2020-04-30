package com.almworks.export.csv;

import com.almworks.api.gui.DialogManager;
import com.almworks.export.*;
import com.almworks.util.config.Configuration;
import org.jetbrains.annotations.NotNull;

public class CSVExporterDescriptor implements ExporterDescriptor {
  private final DialogManager myDialogManager;

  public CSVExporterDescriptor(DialogManager dialogManager) {
    myDialogManager = dialogManager;
  }

  @NotNull
  public String getKey() {
    return "com.almworks.export.csv";
  }

  @NotNull
  public String getDisplayableName() {
    return "Comma-separated file (CSV)";
  }

  @NotNull
  public Exporter createExporter(Configuration configuration, ExportParameters target, ExportedData data)
    throws ExporterNotApplicableException
  {
    return new CSVExporter(this, configuration, myDialogManager);
  }
}
