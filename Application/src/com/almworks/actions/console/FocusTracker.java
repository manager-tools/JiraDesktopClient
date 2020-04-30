package com.almworks.actions.console;

import com.almworks.util.LogHelper;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.List;

/**
 * Extracted from {@link com.almworks.util.components.DropDownListener}
 */
public class FocusTracker extends WindowAdapter {
  private final Window myAncestorWindow;
  private final WindowAdapter myChildHandler = new WindowAdapter() {
    @Override
    public void windowLostFocus(WindowEvent e) {
      if(!isDescendantWindow(e.getOppositeWindow())) {
        myDescendantWindowFocused = false;
        maybeFocusLost();
      }
    }

    @Override
    public void windowClosed(WindowEvent e) {
      e.getWindow().removeWindowFocusListener(this);
      e.getWindow().removeWindowListener(this);
    }
  };
  private final FocusAdapter myComponentHandler = new FocusAdapter() {
    @Override
    public void focusLost(FocusEvent e) {
      Component opposite = e.getOppositeComponent();
      if (myDependents.contains(opposite)) trackComponentFocus(opposite);
      else {
        Window window = SwingTreeUtil.getOwningWindow(opposite);
        if (isDescendantWindow(window)) trackComponentFocus(opposite);
        else {
          trackComponentFocus(null);
          maybeFocusLost();
        }
      }
    }
  };
  private final List<Component> myDependents = Collections15.arrayList();
  @Nullable
  private Runnable myOnFocusLost;
  private boolean myOwnsFocus = false;
  private boolean myDescendantWindowFocused = false;
  private Component myCurrentFocusOwner = null;

  public FocusTracker(Window ancestorWindow) {
    myAncestorWindow = ancestorWindow;
  }

  public static void disposeWhenFocusLost(final Window window) {
    FocusTracker tracker = new FocusTracker(window);
    tracker.disposeRootWhenFocusLost();
    tracker.start();
  }

  public void start() {
    LogHelper.assertError(myOnFocusLost != null);
    myAncestorWindow.addWindowFocusListener(this);
    myAncestorWindow.addWindowListener(this);
    checkOwnsFocus();
  }

  public void disposeRootWhenFocusLost() {
    myOnFocusLost = new Runnable() {
      @Override
      public void run() {
        myAncestorWindow.dispose();
      }
    };
  }

  private void checkOwnsFocus() {
    KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    Window focusedWindow = manager.getFocusedWindow();
    if (focusedWindow == null) return;
    Component focusOwner = manager.getFocusOwner();
    if (isDescendantWindow(focusedWindow)) myDescendantWindowFocused = true;
    else if (!myDependents.contains(focusOwner)) return;
    trackComponentFocus(focusOwner);
    myOwnsFocus = true;
  }

  private void trackComponentFocus(@Nullable Component component) {
    if (myCurrentFocusOwner != null) myCurrentFocusOwner.removeFocusListener(myComponentHandler);
    myCurrentFocusOwner = component;
    if (component != null) component.addFocusListener(myComponentHandler);
  }

  public void addComponents(JComponent ... components) {
    myDependents.addAll(Arrays.asList(components));
  }

  protected void maybeFocusLost() {
    if (!myOwnsFocus) return;
    if (myDescendantWindowFocused || myCurrentFocusOwner != null) return;
    removeAncestorWindowListeners();
    if (myOnFocusLost != null) myOnFocusLost.run();
  }

  private void removeAncestorWindowListeners() {
    myAncestorWindow.removeWindowFocusListener(this);
    myAncestorWindow.removeWindowListener(this);
  }

  @Override
  public void windowGainedFocus(WindowEvent e) {
    if(!isDescendantWindow(e.getOppositeWindow())) {
      myOwnsFocus = true;
    }
  }

  @Override
  public void windowLostFocus(WindowEvent e) {
    final Window opposite = e.getOppositeWindow();
    if(!isDescendantWindow(opposite)) {
      myDescendantWindowFocused = false;
      maybeFocusLost();
    } else {
      myDescendantWindowFocused = true;
      opposite.addWindowListener(myChildHandler);
      opposite.addWindowFocusListener(myChildHandler);
    }
  }

  @Override
  public void windowClosed(WindowEvent e) {
    trackComponentFocus(null);
    removeAncestorWindowListeners();
  }

  private boolean isDescendantWindow(Window child) {
    return isChildWindow(myAncestorWindow, child);
  }

  private boolean isChildWindow(Window parent, Window child) {
    if(child == null || parent == null) {
      return false;
    }

    if(parent == child) {
      return true;
    }

    for(final Window w : parent.getOwnedWindows()) {
      if(isChildWindow(w, child)) {
        return true;
      }
    }
    return false;
  }
}
