package com.almworks.util.ui.widgets.genutil;

import org.almworks.util.StringUtil;

import javax.swing.*;
import java.awt.*;

public class StubComponent extends JComponent {
  public StubComponent(String name) {
    setName(name);
  }

  public void validate() {
  }

  public void revalidate() {
  }

  public void repaint(long tm, int x, int y, int width, int height) {
  }

  public void repaint(Rectangle r) {
  }

  public void invalidate() {
  }

  public void layout() {
  }

  public String toString() {
    return StringUtil.substringAfterLast(getClass().getName(), ".") + "[" + getName() + "]";
  }
}
