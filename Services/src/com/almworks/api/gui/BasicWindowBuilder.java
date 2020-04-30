package com.almworks.api.gui;

import com.almworks.api.container.MutableComponentContainer;
import com.almworks.util.config.Configuration;
import com.almworks.util.properties.Role;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.WindowUtil;
import com.almworks.util.ui.actions.AnActionListener;
import com.almworks.util.ui.actions.DataProvider;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author : Dyoma
 */
public interface BasicWindowBuilder<C extends UIComponentWrapper> {
  void setTitle(String title);

  WindowController showWindow();

  WindowController showWindow(Detach disposeNotification);

  /**
   * @param size used by WindowUtil#setupWindow(org.almworks.util.detach.Lifespan, java.awt.Window, com.almworks.util.config.Configuration, boolean, java.awt.Dimension, boolean, java.awt.GraphicsConfiguration, com.almworks.util.ui.WindowUtil.WindowPositioner)
   */
  void setPreferredSize(@Nullable Dimension size);

  MutableComponentContainer getWindowContainer();

  Configuration getConfiguration();

  void setContent(C content);

  void setContentClass(Class<? extends C> contentClass);

  Role<? extends C> getContentRole();

  void setInitialFocusOwner(Component component);

  void detachOnDispose(Detach detach);

  void setCloseConfirmation(@NotNull AnActionListener confirmation);

  void addProvider(DataProvider provider);

  void setIgnoreStoredSize(boolean ignore);

  void setContent(JComponent content);

  @Nullable
  Component getInitialFocusOwner();

  void setWindowPositioner(WindowUtil.WindowPositioner adjuster);

  void setActionScope(String scope);

  void setDefaultButton(JButton button);

  boolean isModal();

  /**
   * Instruct the builder to install a {@code GlobalDataRoot}
   * on the frame's root pane upon creating the frame.
   * By default it is not installed.
   */
  void addGlobalDataRoot();
}
