package com.almworks.util.ui;

import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import javax.swing.FocusManager;
import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * @author : Dyoma
 */
public class DialogsUtil {
  public static final int DEFAULT_OPTION = JOptionPane.DEFAULT_OPTION;
  public static final int YES_NO_OPTION = JOptionPane.YES_NO_OPTION;
  public static final int YES_NO_CANCEL_OPTION = JOptionPane.YES_NO_CANCEL_OPTION;
  public static final int OK_CANCEL_OPTION = JOptionPane.OK_CANCEL_OPTION;

  public static final int YES_OPTION = JOptionPane.YES_OPTION;
  public static final int NO_OPTION = JOptionPane.NO_OPTION;
  public static final int CANCEL_OPTION = JOptionPane.CANCEL_OPTION;
  public static final int OK_OPTION = JOptionPane.OK_OPTION;
  public static final int CLOSED_OPTION = JOptionPane.CLOSED_OPTION;

  public static final int ERROR_MESSAGE = JOptionPane.ERROR_MESSAGE;
  public static final int INFORMATION_MESSAGE = JOptionPane.INFORMATION_MESSAGE;
  public static final int WARNING_MESSAGE = JOptionPane.WARNING_MESSAGE;
  public static final int QUESTION_MESSAGE = JOptionPane.QUESTION_MESSAGE;
  public static final int PLAIN_MESSAGE = JOptionPane.PLAIN_MESSAGE;

  public static FileDialog createFileDialog(String title, int mode) {
    Window activeWindow = FocusManager.getCurrentManager().getActiveWindow();
    if (activeWindow instanceof Frame)
      return new FileDialog((Frame) activeWindow, title, mode);
    else
      return new FileDialog(JOptionPane.getRootFrame(), title, mode);
  }

  public static File openFileDialog(String title, int mode) {
    FileDialog dialog = createFileDialog(title, mode);
    dialog.setModal(true);
    dialog.setVisible(true);
    String filename = dialog.getFile();
    return filename != null ? new File(dialog.getDirectory() + filename).getAbsoluteFile() : null;
  }

  public static void showException(String actionPresentableName, String message, Exception exception) {
    showErrorMessage(null, message + ": \n" + exception.getLocalizedMessage(), actionPresentableName);
  }

  public static void showPlainMessage(Component parentComponent, Object message, String title) {
    showMessage(parentComponent, message, title, PLAIN_MESSAGE);
  }

  public static void showInformationMessage(Component parentComponent, Object message, String title) {
    showMessage(parentComponent, message, title, INFORMATION_MESSAGE);
  }

  public static void showWarningMessage(Component parentComponent, Object message, String title) {
    showMessage(parentComponent, message, title, WARNING_MESSAGE);
  }

  public static void showErrorMessage(Component parentComponent, Object message, String title) {
    showMessage(parentComponent, message, title, ERROR_MESSAGE);
  }

  public static void showMessage(Component parentComponent, Object message, String title, int messageType) {
    final JOptionPane pane = new JOptionPane(message, messageType, DEFAULT_OPTION);
    final JDialog dialog = pane.createDialog(parentComponent, title);
    dialog.setLocation(getCenteredLocation(dialog));
    dialog.show();
  }

  public static int askUser(Component parentComponent, Object message, String title, int optionType) {
    return askUser(parentComponent, message, title, optionType, QUESTION_MESSAGE);
  }

  public static int askUser(Component parentComponent, Object message, String title, int optionType, int messageType) {
    final JOptionPane pane = new JOptionPane(message, messageType, optionType);
    final JDialog dialog = pane.createDialog(parentComponent, title);
    dialog.setLocation(getCenteredLocation(dialog));
    try {
      dialog.show();
    } catch (ArrayIndexOutOfBoundsException e) {
      Log.error("caught AIOOBE: " + message + " " + title, e);
    }
    dialog.dispose();

    Object selectedValue = pane.getValue();

    if (selectedValue instanceof Integer)
      return ((Integer) selectedValue).intValue();
    return CLOSED_OPTION;
  }

  public static boolean askConfirmation(Component parent, Object message, String title) {
    int reply = askUser(parent, message, title, OK_CANCEL_OPTION, QUESTION_MESSAGE);
    return reply == OK_OPTION;
  }

  public static Point getCenteredLocation(Window w) {
    GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
    if (genv.isHeadlessInstance())
      return new Point(0, 0);
    GraphicsConfiguration gc = w.getGraphicsConfiguration();
    Rectangle size = UIUtil.getScreenUserSize(gc);
    int x = size.x + Math.max(0, (size.width - w.getWidth()) / 2);
    int y = size.y + Math.max(0, (size.height - w.getHeight()) / 2);
    return new Point(x, y);
  }

  /**
   * JOptionPane fails to render HTML in message with line separators. This method replaces line separators with <br>.
   */
  @Nullable
  public static Object adjustJOptionPaneMessage(@Nullable Object message) {
    if(message instanceof String) {
      String s = (String)message;
      return s.startsWith("<html>") ? s.replaceAll("\r?\n", "<br>") : s;
    }
    return message;
  }
}
