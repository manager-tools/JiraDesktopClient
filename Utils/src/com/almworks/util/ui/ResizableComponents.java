package com.almworks.util.ui;

import com.almworks.util.Getter;

import java.awt.*;

/**
 * :todoc:
 *
 * @author sereda
 */
public class ResizableComponents {
  public static Resizable getResizable(final Component component) {
    assert component != null;
    return new AbstractResizable() {
      public int getHeight() {
        return component.getHeight();
      }

      public int getWidth() {
        return component.getWidth();
      }

      public void setSize(int width, int height) {
        component.setSize(width, height);
      }
    };
  }

  public static Resizable getResizable(final Getter<? extends Component> getter) {
    assert getter != null;
    return new AbstractResizable() {
      public int getHeight() {
        return getter.get().getHeight();
      }

      public int getWidth() {
        return getter.get().getWidth();
      }

      public void setSize(int width, int height) {
        getter.get().setSize(width, height);
      }
    };
  }


  public static abstract class AbstractResizable implements Resizable {
    public int addHeight(int plusHeight) {
      setSize(getWidth(), Math.max(getHeight() + plusHeight, 0));
      return getHeight();
    }

    public int addWidth(int plusWidth) {
      setSize(Math.max(getWidth() + plusWidth, 0), getHeight());
      return getWidth();
    }
  }
}
