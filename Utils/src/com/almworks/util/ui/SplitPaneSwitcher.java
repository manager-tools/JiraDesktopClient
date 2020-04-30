package com.almworks.util.ui;

import javax.swing.*;
import java.awt.*;

/**
 * :todoc:
 *
 * @author sereda
 */
public class SplitPaneSwitcher {
  private final JSplitPane mySplitPane;
  private final Resizable myWindow;

  private Component mySavedRightComponent = null;
  private int mySavedDividerSize = -1;
  private int mySavedSizeIncrement = -1;

  public SplitPaneSwitcher(JSplitPane splitPane, Resizable window) {
    mySplitPane = splitPane;
    myWindow = window;
  }

  public int getSizeIncrement() {
    return mySavedSizeIncrement;
  }

  public boolean isRightShown() {
    return mySplitPane.getRightComponent() != null;
  }

  public void setRightShown(boolean show, boolean resize) {
    // todo fight with flickering
    if (show) {
      if (!checkForShowing())
        return;
      if (resize && mySavedSizeIncrement >= 0)
        resize(mySavedSizeIncrement);
      if (mySavedDividerSize > 0)
        mySplitPane.setDividerSize(mySavedDividerSize);
      mySplitPane.setRightComponent(mySavedRightComponent);
      if (resize) {
        final int divider = getMaxDividerLocation();
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            Container parent = mySplitPane.getParent();
            if (parent != null)
              parent.validate();
            if (divider >= 0)
              mySplitPane.setDividerLocation(divider);
          }
        });
      }
    } else {
      if (!checkForHiding())
        return;
      mySavedRightComponent = mySplitPane.getRightComponent();
      mySavedDividerSize = mySplitPane.getDividerSize();
      mySplitPane.setRightComponent(null);
      mySplitPane.setDividerSize(0);
      if (resize) {
        int w = getMaxDividerLocation();
        final int divider = Math.min(mySplitPane.getDividerLocation(), w);
        final int increment = w - divider;
        mySavedSizeIncrement = increment;
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            resize(-increment);
            Container parent = mySplitPane.getParent();
            if (parent != null)
              parent.validate();
          }
        });
      }
    }
  }

  public void setSizeIncrement(int sizeIncrement) {
    mySavedSizeIncrement = sizeIncrement;
  }

  private boolean checkForHiding() {
    assert mySplitPane.getRightComponent() != null;
    return mySplitPane.getRightComponent() != null;
  }

  private boolean checkForShowing() {
    assert mySavedRightComponent != null;
    assert mySavedDividerSize > 0;
    if (mySavedRightComponent == null || mySavedDividerSize <= 0)
      return false;
    return true;
  }

  private int getMaxDividerLocation() {
    return mySplitPane.getWidth() - mySplitPane.getInsets().right;
  }

  private void resize(int increment) {
    if (mySplitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT)
      myWindow.addWidth(increment);
    else
      myWindow.addHeight(increment);
  }
}
