package com.almworks.util.components;

import javax.swing.*;
import java.awt.*;

public class RenderingLabel extends JLabel implements RenderingComponent {
  public RenderingLabel() {
  }

  public RenderingLabel(Icon image) {
    super(image);
  }

  public RenderingLabel(Icon image, int horizontalAlignment) {
    super(image, horizontalAlignment);
  }

  public RenderingLabel(String text) {
    super(text);
  }

  public RenderingLabel(String text, int horizontalAlignment) {
    super(text, horizontalAlignment);
  }

  public RenderingLabel(String text, Icon icon, int horizontalAlignment) {
    super(text, icon, horizontalAlignment);
  }

  public void validate() {}

  public void invalidate() {}

  public void revalidate() {}

  public void repaint(Rectangle r) {}

  public void repaint(long tm, int x, int y, int width, int height) {}

  public void repaint() {}

  public void repaint(long tm) {}

  public void repaint(int x, int y, int width, int height) {}

  public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}

  public void firePropertyChange(String propertyName, char oldValue, char newValue) {}

  public void firePropertyChange(String propertyName, int oldValue, int newValue) {}

  public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {}

  public void firePropertyChange(String propertyName, double oldValue, double newValue) {}

  public void firePropertyChange(String propertyName, float oldValue, float newValue) {}

  public void firePropertyChange(String propertyName, long oldValue, long newValue) {}

  protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}

  public void firePropertyChange(String propertyName, short oldValue, short newValue) {}

  public void reallyInvalidate() {
    super.invalidate();
  }
}
