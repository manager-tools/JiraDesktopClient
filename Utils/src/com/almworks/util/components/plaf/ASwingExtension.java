package com.almworks.util.components.plaf;

import com.almworks.util.ui.ColorUtil;
import org.almworks.util.Log;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;

/**
 * :todoc:
 *
 * @author sereda
 */
public class ASwingExtension extends LAFExtension {
  public void install(LookAndFeel laf) {
    initUI();
    initToolbarButton();
    initLink();
  }

  private void initLink() {
    defaults().put("Link.font", defaults().getFont("Label.font"));
    defaults().put("Link.background", defaults().getColor("Button.background"));
    defaults().put("Link.foreground", new ColorUIResource(0, 0, 255));
    defaults().put("Link.hover", new ColorUIResource(0, 0, 255));
    defaults().put("Link.pressed", new ColorUIResource(90, 90, 255));
    defaults().put("Link.underlined", true);
  }

  private void initToolbarButton() {
    Color color = UIManager.getColor("InternalFrame.activeTitleBackground");
    if (color == null)
      color = Color.BLACK;
    float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
    if (hsb[1] < 0.05 || hsb[2] < 0.05) {
      // adjust bad color :(
      // very low saturation - shade of gray. hue would be irrelevant, so set it to nice blue.
      // very low brightness - shade of black.
      Color oldColor = color;
      color = new Color(10, 36, 106);
      Log.debug("adjusted toolbarbutton.highlight from " + ColorUtil.formatColor(oldColor) + " to " + ColorUtil.formatColor(color));
    }
    defaults().put("AToolbarButton.highlight", color);
  }

  private void initUI() {
    defaults().put("LinkUI", LinkUI.class.getName());
    defaults().put("AToolbarButtonUI", AToolbarButtonUI.class.getName());
  //  defaults().put("TextFieldUI", null)
  }
}
