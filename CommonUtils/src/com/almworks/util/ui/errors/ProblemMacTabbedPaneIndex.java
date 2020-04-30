package com.almworks.util.ui.errors;

import com.almworks.util.Env;

public class ProblemMacTabbedPaneIndex extends KnownProblem {
  private static final String[] PROBLEM_SIGNATURE = {
    "javax.swing.JTabbedPane", "checkIndex", "javax.swing.JTabbedPane", "setSelectedIndex",
    "apple.laf.AquaTabbedPaneUI$MouseHandler", "mouseReleased" 
  };

  public ProblemMacTabbedPaneIndex() {
    super("mac:tabbedPaneIndex");
  }

  protected boolean isProblem(Throwable throwable, StackTraceElement[] trace) {
    return Env.isMac() && checkException(throwable, trace, IndexOutOfBoundsException.class, PROBLEM_SIGNATURE);
  }
}
