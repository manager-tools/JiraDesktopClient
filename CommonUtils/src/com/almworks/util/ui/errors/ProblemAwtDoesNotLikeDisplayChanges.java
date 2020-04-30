package com.almworks.util.ui.errors;

import com.almworks.util.Env;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Util;

import javax.swing.*;

public class ProblemAwtDoesNotLikeDisplayChanges extends KnownProblem {
  private boolean myNotificationShown;

  public ProblemAwtDoesNotLikeDisplayChanges() {
    super("http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4417798");
  }

  protected boolean isProblem(Throwable throwable, StackTraceElement[] trace) {
    if (trace.length < 5)
      return false;
    String clazz = trace[0].getClassName();
    String method = trace[0].getMethodName();
    if (throwable instanceof ArrayIndexOutOfBoundsException) {
      return
        ("sun.awt.windows.WWindowPeer".equals(clazz) && "updateGC".equals(method))
        ||
        ("apple.awt.CWindow".equals(clazz) && "displayChanged".equals(method))
        ||
        ("sun.awt.Win32GraphicsEnvironment".equals(clazz) && "getDefaultScreenDevice".equals(method))
      ;
    }
    if (throwable instanceof RuntimeException && Env.isMac()) {
      return "apple.awt.CGraphicsConfig".equals(clazz) && "getBoundsForDisplay".equals(method);
    }
    if (throwable instanceof IllegalArgumentException) {
      String message = throwable.getMessage();
      if (message != null && Util.lower(message).indexOf("noncontiguous") >= 0) {
        return "createBitsArray".equals(method);
      }
    }
    return false;
  }


  protected void handleProblem(Throwable throwable, StackTraceElement[] trace, boolean reported) {
    if (setNotificationShown()) {
      ThreadGate.AWT.execute(new Runnable() {
        public void run() {
          showMessage();
        }
      });
    }
  }

  private synchronized boolean setNotificationShown() {
    boolean result = !myNotificationShown;
    myNotificationShown = true;
    return result;
  }

  private void showMessage() {
    String message = "<html><body>Error: run-time display configuration change is not supported.<br><br>" +
      "It seems that a secondary display has been added to or removed from the system.<br>" +
      "The application does not support display configuration changes and should be restarted.<br>" +
      "Please restart the application as soon as possible to avoid further errors.<br>" +
      "We apologize for the inconvenience.";
    JOptionPane.showMessageDialog(null, message, "Please Restart Application", JOptionPane.ERROR_MESSAGE);
  }
}
