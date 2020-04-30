package com.almworks.api.gui;

import com.almworks.util.images.IconHandle;
import com.almworks.util.threads.Threads;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StatusBarExpiringMessage extends StatusBarMessage {
  private final long myDuration;
  private Timer myTimer;

  public StatusBarExpiringMessage(StatusBar statusBar, long
    duration, String text, IconHandle icon, String tooltip, ActionListener listener) {
    super(statusBar, listener, text, icon, tooltip);
    myDuration = duration;
  }

  public StatusBarExpiringMessage(StatusBar statusBar, long duration, ActionListener listener) {
    super(statusBar, listener);
    myDuration = duration;
  }

  public void setVisible(boolean visible) {
    Threads.assertAWTThread();
    if (myTimer != null) {
      myTimer.stop();
      myTimer = null;
    }
    super.setVisible(visible);
    if (visible && myDuration > 0) {
      myTimer = new Timer((int)myDuration, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          setVisible(false);
        }
      });
      myTimer.start();
    }
  }
}
