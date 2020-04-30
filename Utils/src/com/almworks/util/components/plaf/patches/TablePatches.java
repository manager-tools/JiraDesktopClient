package com.almworks.util.components.plaf.patches;

import javax.swing.*;
import javax.swing.border.Border;

public class TablePatches extends WindowsLAFPatch {
  private final static String SCROLLPANE_BORDER = "Table.scrollPaneBorder";

  public void install(LookAndFeel laf) {
    Border border = defaults().getBorder(SCROLLPANE_BORDER);
    if (border != null) {
      save(SCROLLPANE_BORDER);
      defaults().put(SCROLLPANE_BORDER, null);
    }
  }

  public void uninstall(LookAndFeel laf) {
    restore(SCROLLPANE_BORDER);
  }
}
