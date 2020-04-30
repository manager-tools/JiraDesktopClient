package com.almworks.util.components.plaf.patches;

import com.almworks.util.components.plaf.LAFExtension;

import javax.swing.*;
import java.awt.*;

public class RowHeightPatch extends LAFExtension {
  public void install(LookAndFeel laf) {
    patch("Table");
    patch("Tree");
  }

  private void patch(String uiclass) {
    Font font = defaults().getFont(uiclass + ".font");
    if (font != null) {
      JLabel label = new JLabel("aAmMgj");
      label.setFont(font);
      Dimension size = label.getPreferredSize();
      if (size != null) {
        // 2 pixels for border
        int h = size.height + 2;
        String key = uiclass + ".rowHeight";
        save(key);
        defaults().put(key, Integer.valueOf(h));
      }
    }
  }

  public void uninstall(LookAndFeel laf) {
    restore("Tree.rowHeight");
    restore("Table.rowHeight");
  }
}
