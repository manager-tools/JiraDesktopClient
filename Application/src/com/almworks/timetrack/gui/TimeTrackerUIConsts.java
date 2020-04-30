package com.almworks.timetrack.gui;

import com.almworks.util.images.IconHandle;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class TimeTrackerUIConsts {
  public static final String AFFORDANCE_RIGHT;
  public static final Icon AFFORDANCE_RIGHT_ICON;

  static {
    final URL url = IconHandle.class.getClassLoader().getResource("com/almworks/rc/affr.png");
    if(url != null) {
      AFFORDANCE_RIGHT = " <img src=\"" + url + "\">";
      AFFORDANCE_RIGHT_ICON = new ImageIcon(url);
    } else {
      AFFORDANCE_RIGHT = "";
      AFFORDANCE_RIGHT_ICON = null;
    }
  }

  public static void setAffordanceIcon(Component c) {
    if(AFFORDANCE_RIGHT_ICON == null) {
      return;
    }

    if(c instanceof AbstractButton) {
      final AbstractButton b = (AbstractButton)c;
      b.setIcon(AFFORDANCE_RIGHT_ICON);
      b.setHorizontalTextPosition(SwingConstants.LEFT);
      b.setVerticalTextPosition(SwingConstants.CENTER);
    }
  }

  public static void setNullIcon(Component c) {
    if(c instanceof AbstractButton) {
      final AbstractButton b = (AbstractButton)c;
      b.setIcon(null);
    }
  }
}
