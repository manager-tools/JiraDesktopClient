package com.almworks.util.components.plaf;

import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.ui.BrokenLineBorder;
import com.almworks.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * This extension installs UI style elements for status bar rendering
 */
public class StatusBarExtensions extends LAFExtension {
  public static final String STATUSBAR_ELEMENT_BORDER = "Ext.StatusBar.sectionBorder";

  public void install(LookAndFeel laf) {
    Border border = getStatusBarElementBorder(laf);
    defaults().put(STATUSBAR_ELEMENT_BORDER, border);
  }

  public void uninstall(LookAndFeel laf) {
    defaults().put(STATUSBAR_ELEMENT_BORDER, null);
  }

  private Border getStatusBarElementBorder(LookAndFeel laf) {
    Border border;
    Color bg = defaults().getColor("Panel.background");
    if(isStyleXP(laf)) {
      border = new CompoundBorder(
        new EmptyBorder(1, 2, 2, 2),
        new CompoundBorder(
          new CompoundBorder(
            new BrokenLineBorder(bg.darker(), 1, BrokenLineBorder.WEST),
            new BrokenLineBorder(bg.brighter(), 1, BrokenLineBorder.WEST)),
          new EmptyBorder(0, 2, 0, 0)));
    } else if(Aqua.isAqua()) {
      border = UIUtil.createWestBevel(UIManager.getColor("Panel.background"));
    } else {
      border = new CompoundBorder(
        new EmptyBorder(0, 2, 0, 0),
        new CompoundBorder(
          new BrokenLineBorder(bg.brighter(), 1, BrokenLineBorder.SOUTH | BrokenLineBorder.EAST),
          new BrokenLineBorder(bg.darker(), 1, BrokenLineBorder.NORTH | BrokenLineBorder.WEST)));
    }
    return border;
  }
}
