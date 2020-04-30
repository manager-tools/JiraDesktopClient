package com.almworks.api.gui;

import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionListener;

public class StatusBarMessage implements StatusBarComponent {
  protected final ActionListener myListener;
  private final Lifecycle myLife = new Lifecycle();
  protected final StatusBar myStatusBar;
  protected final StatusBarLink myLink = new StatusBarLink();
  private Detach myBarDetach = Detach.NOTHING;
  private boolean myVisible = false;

  public StatusBarMessage(StatusBar statusBar, ActionListener listener, String text, Icon icon, String tooltip) {
    myStatusBar = statusBar;
    myListener = listener;
    myLink.setBorder(new EmptyBorder(0, 4, 0, 4));
    setIcon(icon);
    setText(text);
    setTooltip(tooltip);
  }

  public StatusBarMessage(StatusBar statusBar, ActionListener listener) {
    this(statusBar, listener, null, null, null);
  }

  public void setTooltip(String tooltip) {
    myLink.setToolTipText(tooltip);
  }

  public void setText(final String text) {
    myLink.setText(text);
  }

  public void setIcon(Icon icon) {
    myLink.setIcon(icon);
  }

  public int getReservedWidth() {
    return 0;
  }

  public void attach() {
    if (myListener == null) {
      myLink.setEnabled(false);
    } else {
      myLink.addActionListener(myListener);
      myLife.lifespan().add(new Detach() {
        protected void doDetach() {
          myLink.removeActionListener(myListener);
        }
      });
    }
  }

  public JComponent getComponent() {
    return myLink;
  }

  public void dispose() {
    myLife.cycle();
  }

  private void detachBar() {
    myBarDetach.detach();
    myBarDetach = Detach.NOTHING;
  }

  protected void validate() {
    detachBar();
    if (isVisible()) {
      myBarDetach = myStatusBar.addComponent(StatusBar.Section.MESSAGES, this);
    }
  }

  public boolean isVisible() {
    return myVisible;
  }

  public void setVisible(boolean visible) {
    if (myVisible != visible) {
      myVisible = visible;
      validate();
    }
  }
}
