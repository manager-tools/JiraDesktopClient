package com.almworks.util.components.plaf.patches;

import com.almworks.util.Env;
import com.almworks.util.ui.AdjustedSplitPane;
import com.almworks.util.ui.BrokenLineBorder;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Extensions to make the application look good with Windows Aero theme
 */
public class Aero extends WindowsLAFPatch {
  public static final String SCROLL_PANE_BORDER_COLOR = "__almworks.patch.AeroScrollPaneBorderColor";
  public static final String LIGHT_BORDER_COLOR = "__almworks.patch.AeroTextFieldBorderColor";

  private static final boolean IS_WINDOWS_OK = Env.isWindows();

  @Override
  public boolean isExtendingLookAndFeel(LookAndFeel laf) {
    return super.isExtendingLookAndFeel(laf) && IS_WINDOWS_OK && isStyleXP(laf);
  }

  @Override
  public void install(LookAndFeel laf) {
    fetchBorderColor();
    makeLighterColor();
  }

  private void fetchBorderColor() {
    final Border border = UIManager.getBorder("ScrollPane.border");
    if(border instanceof LineBorder) {
      final Color borderColor = ((LineBorder) border).getLineColor();
      UIManager.put(SCROLL_PANE_BORDER_COLOR, borderColor);
    }
  }

  private void makeLighterColor() {
    final Color border = UIManager.getColor(SCROLL_PANE_BORDER_COLOR);
    final Color panel = UIManager.getColor("Panel.background");
    if(border != null && panel != null) {
      UIManager.put(LIGHT_BORDER_COLOR, ColorUtil.between(border, panel, 0.5f));
    }
  }

  @Override
  public void uninstall(LookAndFeel laf) {
    UIManager.put(SCROLL_PANE_BORDER_COLOR, null);
    UIManager.put(LIGHT_BORDER_COLOR, null);
  }

  public static boolean isAero() {
    return IS_WINDOWS_OK && isWindowsModernStyle();     
  }

  public static Border getAeroBoxBorder() {
    return getBorder(SCROLL_PANE_BORDER_COLOR, BrokenLineBorder.EAST | BrokenLineBorder.SOUTH | BrokenLineBorder.WEST | BrokenLineBorder.NORTH);
  }

  public static Border getAeroBorderSouth() {
    return getBorder(SCROLL_PANE_BORDER_COLOR, BrokenLineBorder.SOUTH);
  }

  public static Border getAeroBorderNorth() {
    return getBorder(SCROLL_PANE_BORDER_COLOR, BrokenLineBorder.NORTH);
  }

  public static Border getAeroBorderNorthSouth() {
    return getBorder(SCROLL_PANE_BORDER_COLOR, BrokenLineBorder.NORTH | BrokenLineBorder.SOUTH);
  }

  public static Border getAeroBorderWestEast() {
    return getBorder(SCROLL_PANE_BORDER_COLOR, BrokenLineBorder.WEST | BrokenLineBorder.EAST);
  }

  public static Border getLightBorderSouth() {
    return getBorder(LIGHT_BORDER_COLOR, BrokenLineBorder.SOUTH);
  }

  public static Border getLightBorderNorth() {
    return getBorder(LIGHT_BORDER_COLOR, BrokenLineBorder.NORTH);
  }

  private static Border getBorder(String colorName, int sides) {
    final Color color = UIManager.getColor(colorName);
    if(color == null) {
      return null;
    }
    return new BrokenLineBorder(color, 1, sides);
  }

  public static void makeBorderlessTabbedPane(JTabbedPane tp) {
    if(isAero()) {
      tp.setBorder(new EmptyBorder(0, 0, 0, 0));
    }
  }

  public static void cleanScrollPaneBorder(JComponent scrollPane) {
    if(isAero() && scrollPane instanceof JScrollPane) {
      scrollPane.setBorder(null);
    }
  }

  public static void addSouthBorder(JComponent c) {
    if(isAero()) {
      UIUtil.addOuterBorder(c, getAeroBorderSouth());
    }
  }

  public static void addNorthBorder(JComponent c) {
    if(isAero()) {
      UIUtil.addOuterBorder(c, getAeroBorderNorth());
    }
  }

  public static void addLightSouthBorder(JComponent c) {
    if(isAero()) {
      UIUtil.addOuterBorder(c, getLightBorderSouth());
    }
  }

  public static void makeBorderedDividerSplitPane(JComponent sp) {
    if(isAero() && sp instanceof AdjustedSplitPane) {
      sp.putClientProperty(AdjustedSplitPane.AERO_BORDERED_DIVIDER, true);
    }
  }
}
