package com.almworks.api.gui;

import com.almworks.util.commons.Factory;
import com.almworks.util.model.ValueModel;
import com.almworks.util.properties.Role;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface MainWindowManager {
  Role<MainWindowManager> ROLE = Role.role("mainWindowManager", MainWindowManager.class);

  void setContentComponent(@Nullable JComponent component);

  void showWindow(boolean show);

  StatusBar getStatusBar();

  // kludge: dyoma review - see AboutDialog
  JFrame getMainFrame();

  void bringToFront();

  void minimize();

  void setHideOnMinimizeAndClose(boolean hide);

  interface WindowDescriptor {
    Role<WindowDescriptor> ROLE = Role.role(WindowDescriptor.class);

    ValueModel<String> getWindowTitle();

    Factory<JMenuBar> getMainMenuFactory();
  }
}
