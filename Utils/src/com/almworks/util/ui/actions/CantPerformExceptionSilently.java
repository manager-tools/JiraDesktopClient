package com.almworks.util.ui.actions;

import java.awt.*;

public class CantPerformExceptionSilently extends CantPerformExceptionExplained {
  public CantPerformExceptionSilently(String message) {
    super(message);
  }

  protected void explainImpl(Component parentComponent, String actionName) {
  }
}
