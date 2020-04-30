package com.almworks.api.application.util;

import com.almworks.api.application.StateIcon;

import javax.swing.*;

public interface StateIconFactory {
  StateIcon createIcon(Icon icon, String attributeName, String valueName);

  public static class Default implements StateIconFactory {
    private final int myPriority;

    public Default(int priority) {
      myPriority = priority;
    }

    public StateIcon createIcon(Icon icon, String attributeName, String valueName) {
      return new StateIcon(icon, myPriority, attributeName + ": " + valueName);
    }
  }
}
