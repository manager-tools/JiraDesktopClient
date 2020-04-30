package com.almworks.export.pdf.itext;

import com.almworks.api.application.ExportValueType;
import com.almworks.api.application.util.ExportContext;
import com.almworks.api.application.util.ItemExport;
import com.almworks.api.engine.Connection;
import com.almworks.util.Pair;
import com.almworks.util.properties.PropertyMap;

import java.util.Collections;

/**
 * @author Alex
 */

public abstract class ExportedPrintElement<D> extends PrintKeyElement<D>{
  private final ItemExport myExport;
  private String myExportText;

  public ExportedPrintElement(ItemExport export) {
    myExport = export;
  }

  public String getExportText() {
    return myExportText;
  }

  public ItemExport getExport() {
    return myExport;
  }

  public void setContext(Connection connection, PropertyMap propertyMap, ExportContext context) {
    if (myExport.isExportable(Collections.singletonList(connection))) {
      final Pair<String,ExportValueType> stringExportValueTypePair =
        myExport.formatForExport(propertyMap, context);
      if (stringExportValueTypePair != null) {
        myExportText = stringExportValueTypePair.getFirst();
      }
    } else {
      myExportText = null;
    }
  }

}
