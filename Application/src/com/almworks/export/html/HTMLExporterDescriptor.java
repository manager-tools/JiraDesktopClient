package com.almworks.export.html;

import com.almworks.api.gui.DialogManager;
import com.almworks.api.misc.WorkArea;
import com.almworks.export.*;
import com.almworks.util.config.Configuration;
import org.jetbrains.annotations.NotNull;

public class HTMLExporterDescriptor implements ExporterDescriptor {
  private final DialogManager myDialogManager;
  private final WorkArea myWorkArea;

  public HTMLExporterDescriptor(DialogManager dialogManager, WorkArea workArea) {
    myDialogManager = dialogManager;
    myWorkArea = workArea;
  }

  @NotNull
  public String getKey() {
    return "com.almworks.export.html";
  }

  @NotNull
  public String getDisplayableName() {
    return "HTML file";
  }

  @NotNull
  public Exporter createExporter(Configuration configuration, ExportParameters target, ExportedData data)
    throws ExporterNotApplicableException
  {
    return new HTMLExporter(this, myDialogManager, configuration, myWorkArea);
  }
}
