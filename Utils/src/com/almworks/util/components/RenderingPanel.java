package com.almworks.util.components;

import javax.swing.*;
import java.awt.*;

public class RenderingPanel extends JPanel implements RenderingComponent {
  public RenderingPanel() {
  }

  public RenderingPanel(boolean isDoubleBuffered) {
    super(isDoubleBuffered);
  }

  public RenderingPanel(LayoutManager layout) {
    super(layout);
  }

  public RenderingPanel(LayoutManager layout, boolean isDoubleBuffered) {
    super(layout, isDoubleBuffered);
  }

  /**
   * Always all invalidate children
   */
  public void validate() {
    reallyInvalidate();
    synchronized (getTreeLock()) {
      for (int i = 0; i < getComponentCount(); i++) {
        Component component = getComponent(i);
        if (component instanceof RenderingComponent)
          ((RenderingComponent) component).reallyInvalidate();
        else
          component.invalidate();
      }
    }
    super.validate();
  }

  public void invalidate() {
  }

  public void revalidate() {
  }

  public void repaint(Rectangle r) {
  }

  public void repaint(long tm, int x, int y, int width, int height) {
  }

  public void repaint() {
  }

  public void repaint(long tm) {
  }

  public void repaint(int x, int y, int width, int height) {
  }

  public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
  }

  public void firePropertyChange(String propertyName, char oldValue, char newValue) {
  }

  public void firePropertyChange(String propertyName, int oldValue, int newValue) {
  }

  public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {
  }

  public void firePropertyChange(String propertyName, double oldValue, double newValue) {
  }

  public void firePropertyChange(String propertyName, float oldValue, float newValue) {
  }

  public void firePropertyChange(String propertyName, long oldValue, long newValue) {
  }

  protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
  }

  public void firePropertyChange(String propertyName, short oldValue, short newValue) {
  }

  public void reallyInvalidate() {
    super.invalidate();
  }
}
