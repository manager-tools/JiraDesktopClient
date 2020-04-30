package com.almworks.gui;

import com.almworks.api.container.ComponentContainer;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogEditorBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.MainWindowManager;
import com.almworks.util.commons.Factory;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.DialogsUtil;

import javax.swing.*;
import java.awt.*;

/**
 * @author : Dyoma
 */
public abstract class BaseDialogManager implements DialogManager {
  private static final Factory<Window> ourMainWindow = new Factory<Window>() {
    public Window create() {
      // "Main" window concept is not universal, however, currently it is quite credible.
      // Previous solution could return a window lifetime of which was shorter than that of the window that was created using this factory.
      // That led to the window created using this factory to be closed earlier than needed.      
      return Context.require(MainWindowManager.ROLE).getMainFrame();
    }
  };
  private final Configuration myConfiguration;
  private final ComponentContainer myContainer;
  private final Factory<Window> myDefaultOwnerWindow;

  protected BaseDialogManager(Configuration configuration, ComponentContainer container,
    Factory<Window> defaultOwnerWindow)
  {
    myConfiguration = configuration;
    myContainer = container;
    myDefaultOwnerWindow = defaultOwnerWindow;
  }

  public DialogBuilder createBuilder(String windowId) {
    return new DialogBuilderImpl2(myContainer.createSubcontainer(windowId), getConfiguration(windowId),
      myDefaultOwnerWindow);
  }

  public DialogEditorBuilder createEditor(String windowId) {
    return new DialogEditorBuilderImpl(createBuilder(windowId));
  }

  public void showErrorMessage(final String title, final String message) {
    showMessage(title, message, JOptionPane.ERROR_MESSAGE);
  }

  public void showMessage(final String title, final Object message, final int messageType) {
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        Window mainWindow = ourMainWindow.create();
        if (mainWindow == null)
          mainWindow = myDefaultOwnerWindow.create();
        DialogsUtil.showMessage(mainWindow, message, title, messageType);
      }
    });
  }

  public DialogBuilder createMainBuilder(String windowId) {
    return createDialogBuilder(windowId, ourMainWindow);
  }

  private DialogBuilder createDialogBuilder(String windowId, Factory<Window> owner) {
    return new DialogBuilderImpl2(myContainer.createSubcontainer(windowId), getConfiguration(windowId), owner);
  }

  private Configuration getConfiguration(String windowId) {
    assert windowId != null && windowId.length() != 0 : windowId;
    return myConfiguration.getOrCreateSubset(windowId);
  }
}
