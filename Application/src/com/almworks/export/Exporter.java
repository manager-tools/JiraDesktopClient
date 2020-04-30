package com.almworks.export;

import com.almworks.util.ui.actions.CantPerformException;
import org.jetbrains.annotations.NotNull;

public interface Exporter {
  void export(@NotNull ExportedData data, @NotNull ExportParameters parameters, boolean silent);

  @NotNull ExporterDescriptor getDescriptor();

  @NotNull ExporterUI getUI();

  /**
   * This method is called when user presses "Export" button. Exporter has a last chance to validate
   * parameters and ask user questions.
   */
  void confirmExport() throws CantPerformException;
}
