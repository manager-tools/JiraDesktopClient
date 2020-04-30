package com.almworks.export.xml;

import com.almworks.api.gui.DialogManager;
import com.almworks.api.platform.ProductInformation;
import com.almworks.export.*;
import com.almworks.util.config.Configuration;
import org.jetbrains.annotations.NotNull;

public class XMLExporterDescriptor implements ExporterDescriptor {
  private final DialogManager myDialogManager;
  private final ProductInformation myProductInfo;

  public XMLExporterDescriptor(DialogManager dialogManager, ProductInformation productInfo) {
    myDialogManager = dialogManager;
    myProductInfo = productInfo;
  }

  @NotNull
  public String getKey() {
    return "com.almworks.export.xml";
  }

  @NotNull
  public String getDisplayableName() {
    return "XML file";
  }

  @NotNull
  public Exporter createExporter(Configuration configuration, ExportParameters target, ExportedData data)
    throws ExporterNotApplicableException
  {
    return new XMLExporter(this, myDialogManager, configuration, myProductInfo);
  }
}
