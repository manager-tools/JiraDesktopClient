package com.almworks.util.ui.errors;

public class ProblemDraggableTreeExpansionInJdk16 extends KnownProblem {
  private static final String[] TRACE_SIGNATURE = {
    "javax.swing.plaf.basic.BasicTreeUI$Handler", "isActualPath", "javax.swing.plaf.basic.BasicTreeUI$Handler",
    "mouseReleasedDND"};

  public ProblemDraggableTreeExpansionInJdk16() {
    super("http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6505523");
  }

  protected boolean isProblem(Throwable throwable, StackTraceElement[] trace) {
    return checkException(throwable, trace, NullPointerException.class, TRACE_SIGNATURE);
  }
}
