package com.almworks.export;

import com.almworks.util.config.Configuration;
import org.jetbrains.annotations.NotNull;

public interface ExporterDescriptor {
  @NotNull
  String getKey();

  /**
   * @return answer to question "export to what?"
   */
  @NotNull
  String getDisplayableName();

  @NotNull
  Exporter createExporter(Configuration configuration, ExportParameters target, ExportedData data)
    throws ExporterNotApplicableException;
}
