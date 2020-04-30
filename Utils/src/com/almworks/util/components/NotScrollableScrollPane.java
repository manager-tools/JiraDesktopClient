package com.almworks.util.components;

import org.almworks.util.Collections15;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelListener;
import java.util.List;

/**
 * @author dyoma
 */
public class NotScrollableScrollPane extends JScrollPane {
  private boolean myScrollable;
  private List<MouseWheelListener> mySavedListeners;

  public NotScrollableScrollPane() {
    super();
    setScrollableMode(true);
  }

  public NotScrollableScrollPane(Component view) {
    super(view);
    setScrollableMode(true);
  }

  public synchronized void setScrollableMode(boolean scrollable) {
    if (myScrollable == scrollable)
      return;
    myScrollable = scrollable;
    if (scrollable)
      if (mySavedListeners != null) {
        for (MouseWheelListener listener : mySavedListeners)
          super.addMouseWheelListener(listener);
        mySavedListeners = null;
        return;
      }
    assert mySavedListeners == null;
    MouseWheelListener[] listeners = getListeners(MouseWheelListener.class);
    mySavedListeners = Collections15.arrayList(listeners);
    for (MouseWheelListener listener : mySavedListeners)
      super.removeMouseWheelListener(listener);
  }

  public synchronized void addMouseWheelListener(MouseWheelListener l) {
    if (myScrollable) {
      assert mySavedListeners == null;
      super.addMouseWheelListener(l);
      return;
    }
    if (mySavedListeners == null)
      mySavedListeners = Collections15.arrayList(4);
    mySavedListeners.add(l);
  }

  public synchronized void removeMouseWheelListener(MouseWheelListener l) {
    if (myScrollable) {
      assert mySavedListeners == null;
      super.removeMouseWheelListener(l);
      return;
    }
    if (mySavedListeners != null)
      mySavedListeners.remove(l);
  }
}
