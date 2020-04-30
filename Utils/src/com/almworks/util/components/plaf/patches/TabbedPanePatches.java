package com.almworks.util.components.plaf.patches;

import com.almworks.util.Env;

import javax.swing.*;
import java.awt.*;

public class TabbedPanePatches extends WindowsLAFPatch {
  private static final String TAB_INSETS_KEY = "TabbedPane.tabInsets";
  private static final String CONTENT_BORDER_INSETS_KEY = "TabbedPane.contentBorderInsets";
  private static final String TAB_AREA_INSETS_KEY = "TabbedPane.tabAreaInsets";

  public void install(LookAndFeel laf) {
    patchTabInsets();
    if (Env.isWindowsVistaOrLater() && isStyleXP(laf)) {
      patchContentBorderInsets();
      patchTabAreaInsets();
    }
  }

  private void patchTabInsets() {
    Insets insets = defaults().getInsets(TAB_INSETS_KEY);
    if (insets != null) {
      save(TAB_INSETS_KEY);
      defaults().put(TAB_INSETS_KEY, new Insets(insets.top + 3, insets.left, insets.bottom, insets.right));
    }
  }

  private void patchContentBorderInsets() {
    Insets borderInsets = defaults().getInsets(CONTENT_BORDER_INSETS_KEY);
    if (borderInsets != null) {
      save(CONTENT_BORDER_INSETS_KEY);
      defaults().put(CONTENT_BORDER_INSETS_KEY, new Insets(borderInsets.top, 1, borderInsets.bottom - 1, 3));
    }
  }

  private void patchTabAreaInsets() {
    Insets tabAreaInsets = defaults().getInsets(TAB_AREA_INSETS_KEY);
    if (tabAreaInsets != null) {
      save(TAB_AREA_INSETS_KEY);
      defaults().put(TAB_AREA_INSETS_KEY, new Insets(tabAreaInsets.top - 1, tabAreaInsets.left, tabAreaInsets.bottom, tabAreaInsets.right));
    }
  }

  public void uninstall(LookAndFeel laf) {
    restore(TAB_INSETS_KEY);
    restore(CONTENT_BORDER_INSETS_KEY);
    restore(TAB_AREA_INSETS_KEY);
  }
}
