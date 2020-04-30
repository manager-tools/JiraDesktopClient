package com.almworks.util.ui.actions;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Utility class for setting and retrieving window actins scope identifiers.
 * @author Pavel Zvyagin
 */
public class ActionScope {
  private static final String ACTION_SCOPE = "windowActionScope";

  /**
   * Set the given action scope identifier for the given window.
   * @param window The window.
   * @param scope The scope identifier. {@code null} or empty string remove the scope ID.
   */
  public static void set(@NotNull Window window, String scope) {
    assert window instanceof RootPaneContainer;

    if(window instanceof RootPaneContainer) {
      ((RootPaneContainer)window).getRootPane().putClientProperty(ACTION_SCOPE, scope.length() == 0 ? null : scope);
    }
  }

  /**
   * Retrieve the action scope identifier for the given window.
   * @param window The window.
   * @return The scope identifier, or empty string if none is set on the window.
   */
  @NotNull
  public static String get(@NotNull Window window) {
    assert window instanceof RootPaneContainer;

    if(window instanceof RootPaneContainer) {
      final String result = (String) ((RootPaneContainer) window).getRootPane().getClientProperty(ACTION_SCOPE);
      return result == null ? "" : result;
    }

    return "";
  }
}
