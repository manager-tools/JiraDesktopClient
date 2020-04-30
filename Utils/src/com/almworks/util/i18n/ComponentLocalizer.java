package com.almworks.util.i18n;

import javax.swing.*;

public interface ComponentLocalizer {
  String forJLabel(JLabel c, Kind kind, String value);

  String forJComponent(JComponent c, Kind kind, String value);

  enum Kind {
    TEXT,
    TOOLTIP,
  }
}
