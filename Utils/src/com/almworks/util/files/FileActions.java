package com.almworks.util.files;

import com.almworks.util.AppBook;
import com.almworks.util.Env;
import com.almworks.util.ExceptionHandler;
import com.almworks.util.LogHelper;
import com.almworks.util.i18n.LText;
import com.almworks.util.i18n.LText2;
import com.almworks.util.ui.MacIntegration;
import com.almworks.util.ui.swing.SwingTreeUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static com.almworks.util.ui.DialogsUtil.adjustJOptionPaneMessage;

public class FileActions {
  public static final String OPEN_TITLE = "&Open with Default Program";
  public static final String OPEN_WITH_TITLE = "Open &With\u2026";
  public static final String OPEN_FOLDER_TITLE = getOpenFolderTitle();

  private static String getOpenFolderTitle() {
    if(MacIntegration.isRevealInFinderSupported() || Env.isMacSnowLeopardOrNewer()) {
      return "Show in Finder";
    }
    return "Ope&n Containing Folder";
  }

  private static final String X = "Utils.FileActions.";

  private static final LText CANNOT_OPEN_AS_TITLE = AppBook.text(X + "CANNOT_OPEN_AS_TITLE", "Cannot Open File");
  private static final LText2<String, String> CANNOT_OPEN_AS =
    AppBook.text(X + "CANNOT_OPEN_AS", "<html><body>Cannot open file {0}<br>Error: {1}</body></html>", "", "");

  private static final LText CANNOT_OPEN_TITLE = AppBook.text(X + "CANNOT_OPEN_TITLE", "Cannot Open File");
  private static final LText2<String, String> CANNOT_OPEN =
    AppBook.text(X + "CANNOT_OPEN", "<html><body>Cannot open file {0}<br>Error: {1}</body></html>", "", "");

  private static final LText CANNOT_OPEN_FOLDER_TITLE = AppBook.text(X + "CANNOT_OPEN_AS_TITLE", "Cannot Open Folder");
  private static final LText2<String, String> CANNOT_OPEN_FOLDER =
    AppBook.text(X + "CANNOT_OPEN_FOLDER", "<html><body>Cannot open folder {0}<br>Error: {1}</body></html>", "", "");

  private FileActions() {}

  public static boolean isSupported(Action action) {
    return action.isSupported();
  }

  public static void performAction(Action action, File file, Component owner) {
    action.perform(file, owner);
  }

  public static void openFile(File file, Component owner) {
    if (Env.isWindows()) {
      winOpenFile(file, owner);
    } else if (isDesktopOpenSupported()) {
      desktopOpenFile(file, owner);
    }
  }

  /**
   * We need to use the old way on Windows due to JVM bug: http://bugs.sun.com/view_bug.do?bug_id=6631015
   * http://jira.almworks.com/browse/JCO-873
   */
  public static void winOpenFile(File file, Component owner) {
    if (!Env.isWindows())
      return;
    String path = file.getAbsolutePath();
    String[] cmd = new String[]{
      "cmd.exe",
      "/c",
       path
    };
    try {
      Runtime.getRuntime().exec(cmd);
    } catch (Exception e) {
      JOptionPane.showMessageDialog(owner, adjustJOptionPaneMessage(CANNOT_OPEN.format(path, e.getMessage())),
        CANNOT_OPEN_TITLE.format(), JOptionPane.ERROR_MESSAGE);
    }
  }

  private static void desktopOpenFile(File file, Component owner) {
    open(file.getAbsolutePath(), owner, CANNOT_OPEN, CANNOT_OPEN_TITLE);
  }

  public static void openContainingFolder(File file, Component owner) {
    if(MacIntegration.isRevealInFinderSupported()) {
      MacIntegration.revealInFinder(file,
        new MessageDialogHandler(file.getAbsolutePath(), owner, CANNOT_OPEN_FOLDER, CANNOT_OPEN_FOLDER_TITLE));
    } else if(Env.isMacSnowLeopardOrNewer()) {
      macRevealInFinder(file, owner);
    } else if(Env.isWindows()) {
      winSelectInExplorer(file, owner);
    } else if(isDesktopOpenSupported()) {
      desktopOpenFolder(file, owner);
    }
  }

  public static void highlightFolderOrShowContents(File folder, Component owner) {
    if(canHighlightFiles()) {
      openContainingFolder(folder, owner);
    } else {
      openFile(folder, owner);
    }
  }

  public static boolean canHighlightFiles() {
    return MacIntegration.isRevealInFinderSupported() || Env.isMacSnowLeopardOrNewer() || Env.isWindows();
  }

  private static void macRevealInFinder(File file, Component owner) {
    exec(
      new String[] { "open", "-R", file.getAbsolutePath() },
      owner, CANNOT_OPEN_FOLDER, CANNOT_OPEN_FOLDER_TITLE);
  }

  private static void winSelectInExplorer(File file, Component owner) {
    exec(
      new String[] { "explorer", "/select,", file.getAbsolutePath() },
      owner, CANNOT_OPEN_FOLDER, CANNOT_OPEN_FOLDER_TITLE);
  }

  private static void desktopOpenFolder(File file, Component owner) {
    open(file.getParent(), owner, CANNOT_OPEN_FOLDER, CANNOT_OPEN_FOLDER_TITLE);
  }

  public static void openAs(File file, Component owner) {
    if(Env.isWindows()) {
      winOpenAs(file, owner);
    }
  }

  private static void winOpenAs(File file, Component owner) {
    exec(
      new String[] { "rundll32.exe", "shell32.dll,OpenAs_RunDLL", file.getAbsolutePath() },
      owner, CANNOT_OPEN_AS, CANNOT_OPEN_AS_TITLE);
  }

  public static boolean isDesktopOpenSupported() {
    return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN);
  }

  private static void open(String path, Component owner, LText2<String, String> errorMessage, LText errorTitle) {
    if(!isDesktopOpenSupported()) {
      return;
    }
    JRootPane root = SwingTreeUtil.findAncestorOfType(owner, JRootPane.class);
    if (owner != null && root == null) LogHelper.warning("No root pane", owner);
    else owner = root;
    final File file = new File(path);
    try {
      Desktop.getDesktop().open(file);
    } catch (IOException ex) {
      JOptionPane.showMessageDialog(
        owner, adjustJOptionPaneMessage(errorMessage.format(path, ex.getMessage())),
        errorTitle.format(), JOptionPane.ERROR_MESSAGE);
    } catch (IllegalArgumentException ex) {
      JOptionPane.showMessageDialog(
        owner, adjustJOptionPaneMessage(errorMessage.format(path, ex.getMessage())),
        errorTitle.format(), JOptionPane.ERROR_MESSAGE);
    }
  }

  private static void exec(String[] cmd, Component owner, LText2<String, String> errorMessage, LText errorTitle) {
    try {
      Runtime.getRuntime().exec(cmd);
    } catch(IOException ex) {
      JOptionPane.showMessageDialog(
        owner, adjustJOptionPaneMessage(errorMessage.format(cmd[cmd.length - 1], ex.getMessage())),
        errorTitle.format(), JOptionPane.ERROR_MESSAGE);
    }
  }

  public static enum Action {
    OPEN_AS {
      @Override
      boolean isSupported() {
        return Env.isWindows();
      }
      @Override
      void perform(File file, Component owner) {
        openAs(file, owner);
      }
    },

    OPEN_CONTAINING_FOLDER {
      @Override
      boolean isSupported() {
        return canHighlightFiles() || isDesktopOpenSupported();
      }
      @Override
      void perform(File file, Component owner) {
        openContainingFolder(file, owner);
      }
    },

    OPEN {
      @Override
      boolean isSupported() {
        return isDesktopOpenSupported() || Env.isWindows();
      }
      @Override
      void perform(File file, Component owner) {
        openFile(file, owner);
      }
    };

    abstract boolean isSupported();
    abstract void perform(File file, Component owner);
  }

  private static class MessageDialogHandler implements ExceptionHandler {
    private final String myFilePath;
    private final Component myOwner;
    private final LText2<String, String> myErrorMessage;
    private final LText myErrorTitle;

    private MessageDialogHandler(
      String filePath, Component owner, LText2<String, String> errorMessage, LText errorTitle)
    {
      myFilePath = filePath;
      myOwner = owner;
      myErrorMessage = errorMessage;
      myErrorTitle = errorTitle;
    }

    @Override
    public Object handle(Exception e) {
      final Throwable wrapped = (e instanceof InvocationTargetException) ? e.getCause() : e;
      JOptionPane.showMessageDialog(
        myOwner, adjustJOptionPaneMessage(myErrorMessage.format(myFilePath, wrapped.getMessage())),
        myErrorTitle.format(), JOptionPane.ERROR_MESSAGE);
      return null;
    }
  }
}
