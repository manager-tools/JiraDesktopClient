package com.almworks.util.components.plaf;

import com.almworks.util.ui.ColorUtil;

import javax.swing.*;
import java.awt.*;

public class AdditionalColorExtension extends LAFExtension {
  public void install(LookAndFeel laf) {
    Color background = defaults().getColor("EditorPane.background");
    Color foreground = defaults().getColor("EditorPane.foreground");

    Color hiBackground = ColorUtil.between(background, foreground, 0.5F);
    Color hiForeground = background;
    Color hiLinkForeground = defaults().getColor("Panel.background");
    Color hiLinkPressed = ColorUtil.between(hiLinkForeground, hiForeground, 0.5F);

    defaults().put("HalfInverse.background", hiBackground);
    defaults().put("HalfInverse.foreground", hiForeground);
    defaults().put("HalfInverse.Link.foreground", hiLinkForeground);
    defaults().put("HalfInverse.Link.pressed", hiLinkPressed);
  }

  public void uninstall(LookAndFeel laf) {
    defaults().remove("HalfInverse.background");
    defaults().remove("HalfInverse.foreground");
  }
}
