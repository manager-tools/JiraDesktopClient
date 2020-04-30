package com.almworks.actions.console;

import com.almworks.util.Env;
import com.almworks.util.LogHelper;
import com.almworks.util.ui.swing.Shortcuts;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Extracted from {@link com.almworks.util.components.DropDownListener}
 */
public class PopupController {
  public static final String HIDE_POPUP = "PopupContoller.hidePopupAction";
  private final JComponent myContent;
  private DropDownWindow myWindow;
  private boolean myHideOnEscape;
  private boolean myListenFocus = true;
  private final AbstractAction myHideAction = new AbstractAction() {
    public void actionPerformed(ActionEvent e) {
      hide();
    }
  };

  public PopupController(JComponent content) {
    myContent = content;
  }

  public JWindow showAt(@Nullable Window parentWindow, Rectangle screenLocation) {
    hide();
    myWindow = new DropDownWindow(parentWindow);
    myWindow.getRootPane().getActionMap().put(HIDE_POPUP, getHideAction());
    myWindow.getContentPane().add(myContent);
    myWindow.setSize(screenLocation.getSize());
    myWindow.setLocation(screenLocation.getLocation());
    myWindow.invalidate();
    if (myListenFocus) FocusTracker.disposeWhenFocusLost(myWindow);
    if (myHideOnEscape) myWindow.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(Shortcuts.ESCAPE, HIDE_POPUP);
    showAndRequestFocus();
    return myWindow;
  }

  public AbstractAction getHideAction() {
    return myHideAction;
  }

  private void showAndRequestFocus() {
    myWindow.setVisible(true);
    myWindow.toFront();
    if (myListenFocus) {
      myWindow.requestFocus();
      Component component = myContent.getFocusCycleRootAncestor().getFocusTraversalPolicy().getInitialComponent(myWindow);
      if (component != null) component.requestFocusInWindow();
    }
  }

  public void setHideOnEscape(boolean hide) {
    myHideOnEscape = hide;
  }

  public void setListenFocus(boolean listenFocus) {
    myListenFocus = listenFocus;
  }

  public void hide() {
    if (myWindow == null) return;
    KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    Component focusOwner = manager.getFocusOwner();
    if (focusOwner != null && SwingTreeUtil.isAncestor(myWindow, focusOwner)) {
      if (Env.isLinux()) { // Checked that this code is required on Linux and leads to wrong focus on Windows. See static init of OperationConsoleAction
        Window parentWindow = Util.castNullable(Window.class, myWindow.getParent());
        Component returnFocus = parentWindow != null ? parentWindow.getMostRecentFocusOwner() : null;
        if (returnFocus != null) returnFocus.requestFocus();
      }
    }
    myWindow.dispose();
  }

  public boolean isShowing() {
    return myWindow != null && myWindow.isShowing();
  }

  @Nullable
  public JWindow getShowingPopup() {
    return isShowing() ? myWindow : null;
  }

  private class DropDownWindow extends JWindow {
    public DropDownWindow(Window parentWindow) {
      super(parentWindow);
    }

    public void dispose() {
      super.dispose();
      if (this != myWindow)
        // Dispose may be call several times when the window is already disposed and parent window disposed all owned children (including already disposed)
        LogHelper.assertError(myWindow == null && getPeer() == null, "Wrong popup window", myWindow, myContent);
      else {
        myWindow = null;
      }
    }
  }
}
