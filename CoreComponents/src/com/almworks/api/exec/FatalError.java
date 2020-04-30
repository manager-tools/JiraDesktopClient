package com.almworks.api.exec;

import com.almworks.util.exec.Context;
import com.almworks.util.exec.Gateable;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.Threads;
import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class FatalError {
  public static void showDialogAndTerminate(Gateable dialog) {
    ThreadGate.AWT_IMMEDIATE.execute(false, dialog);
    if (!Context.isAWT()) {
      endlessCycle();
    }
  }

  private static synchronized void endlessCycle() {
    try {
      Threads.assertIndefiniteWaitAllowed();
      while (true) {
        FatalError.class.wait(300);
      }
    } catch (InterruptedException e) {
      throw new RuntimeInterruptedException(e);
    }
  }

  private static void terminate() {
    System.exit(3);
  }

  public static Gateable prepareErrorDialog(String title, String errorMessage, boolean modal) {
    JOptionPane pane = new JOptionPane(errorMessage, JOptionPane.ERROR_MESSAGE, JOptionPane.DEFAULT_OPTION);
    final JDialog dialog = pane.createDialog(JOptionPane.getRootFrame(), title);
    pane.selectInitialValue();
    dialog.setModal(modal);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    return new Gateable() {
      public void runGated() {
        dialog.show();
        dialog.addWindowListener(new WindowAdapter() {
          public void windowClosing(WindowEvent we) {
            terminate();
          }
        });
        dialog.addComponentListener(new ComponentAdapter() {
          public void componentHidden(ComponentEvent e) {
            terminate();
          }
        });
      }
    };
  }

  public static void terminate(String title, String errorMessage, Throwable exception) {
    Log.warn("FATAL PROBLEM", exception);
    // if the problem is in awt thread, make dialog modal to avoid execution after problem point
    boolean modal = Context.isAWT();
    Gateable dialog = prepareErrorDialog(title, errorMessage, modal);
    showDialogAndTerminate(dialog);
  }
}
