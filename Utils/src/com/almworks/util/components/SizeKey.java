package com.almworks.util.components;

import com.almworks.util.Enumerable;

import javax.swing.*;
import java.awt.*;

public abstract class SizeKey extends Enumerable<SizeKey> {
  public static final SizeKey PREFERRED = new SizeKey("PREFERRED") {
    public Dimension getFrom(JComponent component) {
      return component.getPreferredSize();
    }
  };
  public static final SizeKey MINIMUM = new SizeKey("MINIMUM") {
    public Dimension getFrom(JComponent component) {
      return component.getMinimumSize();
    }
  };
  public static final SizeKey MAXIMUM = new SizeKey("MAXIMUM") {
    public Dimension getFrom(JComponent component) {
      return component.getMaximumSize();
    }
  };

  private SizeKey(String name) {
    super(name);
  }

  public abstract Dimension getFrom(JComponent component);
}
