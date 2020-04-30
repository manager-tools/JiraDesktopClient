package com.almworks.gui;

import com.almworks.api.gui.WindowController;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

/**
 * @author dyoma
 */
public class WindowControllerImpl implements WindowController {
  private static final ComponentProperty<WindowControllerImpl> CONTROLLER = ComponentProperty.createProperty("windowController");

  private final Window myWindow;
  private final AnActionListener myCloseListener;
  private boolean myDisposed = false;

  private boolean myHideOnClose;
  /**
   * TRUE: close without dialog, FALSE: don't close, no dialog, null : ask
   */
  private Boolean myCloseSilently = null;
  private final Lifespan myShowLife;

  public WindowControllerImpl(Window window, AnActionListener closeListener, Lifespan showLife) {
    myShowLife = showLife;
    assert closeListener != null;
    //noinspection ChainOfInstanceofChecks
    if (window instanceof JDialog)
      ((JDialog) window).setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    else if (window instanceof JFrame)
      ((JFrame) window).setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    else
      assert false : window;
    myWindow = window;
    CONTROLLER.putClientValue(((RootPaneContainer)myWindow).getRootPane(), this);
    myCloseListener = closeListener;

    myWindow.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        if (myHideOnClose) {
          hide();
          return;
        }
        CantPerformExceptionExplained cantClose = close();
        if (cantClose != null)
          cantClose.explain(getTitle(), new DefaultActionContext(getContentComponent()));
        else
          myWindow.removeWindowListener(this);
      }
    });
  }

  private static WindowControllerImpl getController(Window window) {
    RootPaneContainer container = Util.castNullable(RootPaneContainer.class, window);
    return container == null ? null : CONTROLLER.getClientValue(container.getRootPane());
  }

  @Nullable
  public CantPerformExceptionExplained close() {
    CantPerformExceptionExplained explained = checkCanClose();
    if (explained != null) return explained;
    disposeWindow();
    return null;
  }

  @Nullable("When safe to close")
  private CantPerformExceptionExplained checkCanClose() {
    if (Boolean.FALSE.equals(myCloseSilently)) return new CantPerformExceptionSilently("cannot close");
    if (myWindow.isDisplayable() && !Boolean.TRUE.equals(myCloseSilently)) {
      CantPerformExceptionExplained explained = ActionUtil.performSafe(myCloseListener, getContentComponent());
      if (explained != null) return explained;
    }
    for (Window window : myWindow.getOwnedWindows()) {
      WindowControllerImpl controller = getController(window);
      if (controller == null) continue;
      CantPerformExceptionExplained explained = controller.checkCanClose();
      if (explained != null) return explained;
    }
    return null;
  }

  @Override
  public void disableCloseConfirmation(boolean allowClose) {
    myCloseSilently = allowClose;
  }

  @Override
  public void enableCloseConfirmation() {
    myCloseSilently = null;
  }

  private void disposeWindow() {
    if (myDisposed) return;
    myDisposed = true;
    int defaultCloseOperation = UIUtil.getDefaultCloseOperation(myWindow);
    assert defaultCloseOperation == WindowConstants.DO_NOTHING_ON_CLOSE;
    myWindow.dispose();
  }

  public void show() {
    try {
      myWindow.setVisible(true);
    } catch (ArrayIndexOutOfBoundsException e) {
      Log.warn("swing bug detected (multi-monitor configuration update)");
      // see #1351 and corresponding Swing bugs
      // what else can we do?
      throw e;
    }
    myWindow.invalidate();
    myWindow.validate();
  }

  @Override
  public Lifespan getShowLife() {
    return myShowLife;
  }

  /** @noinspection ChainOfInstanceofChecks*/
  public void setTitle(String title) {
    if (myWindow instanceof Dialog)
      ((Dialog) myWindow).setTitle(title);
    else if (myWindow instanceof Frame)
      ((Frame) myWindow).setTitle(title);
    else
      assert false : myWindow;
  }

  public boolean isVisible() {
    return myWindow.isVisible();
  }

  public boolean activate() {
    if (!myWindow.isVisible()) {
      myWindow.setVisible(true);
      return true;
    } else {
      myWindow.requestFocus();
      myWindow.toFront();
      return false;
    }
  }

  public final void addDataProvider(DataProvider provider) {
    JRootPane rootPane = ((RootPaneContainer) myWindow).getRootPane();
    DataProvider.DATA_PROVIDER.putClientValue(rootPane, provider);
  }

  /**
   * @deprecated
   */
  public Window getWindow() {
    return myWindow;
  }

  /** @noinspection ChainOfInstanceofChecks*/
  public Component getContentComponent() {
    if (myWindow instanceof JDialog)
      return ((RootPaneContainer) myWindow).getContentPane();
    else if (myWindow instanceof JFrame)
      return ((RootPaneContainer) myWindow).getContentPane();
    assert false : myWindow;
    return null;
  }

  /** @noinspection ChainOfInstanceofChecks*/
  public String getTitle() {
    if (myWindow instanceof Dialog)
      return ((Dialog) myWindow).getTitle();
    else if (myWindow instanceof Frame)
      ((Frame) myWindow).getTitle();
    else
      assert false : myWindow;
    return "";
  }

  public void toFront() {
    myWindow.toFront();
  }

  public void hide() {
    if (myWindow instanceof JFrame) {
      UIUtil.minimizeFrame(((JFrame) myWindow));
    } else {
      myWindow.setVisible(false);
    }
  }

  public void setHideOnClose(boolean hideOnClose) {
    myHideOnClose = hideOnClose;
  }

  @Override
  public Detach addWindowFocusListener(final WindowFocusListener listener) {
    myWindow.addWindowFocusListener(listener);
    return new Detach() {
      @Override
      protected void doDetach() throws Exception {
        myWindow.removeWindowFocusListener(listener);
      }
    };
  }
}

