package com.almworks.util.ui.errors;

import com.almworks.util.Env;
import org.almworks.util.Util;

public class ProblemMacPaintsHeaderOfDraggedColumnEvenIfItIsRemoved extends KnownProblem {
  private static final String[] TRACE_SIGNATURE = {
    "java.util.Vector", "elementAt", "javax.swing.table.DefaultTableColumnModel", "getColumn",
    "javax.swing.plaf.basic.BasicTableHeaderUI", "getHeaderRenderer",
    "javax.swing.plaf.basic.BasicTableHeaderUI", "paintCell"
  };

  public ProblemMacPaintsHeaderOfDraggedColumnEvenIfItIsRemoved() {
    super("mac:painting_dragged_removed_column:http://bugzilla.almworks.com/show_bug.cgi?id=641");
  }

  protected boolean isProblem(Throwable throwable, StackTraceElement[] trace) {
    if (!Env.isMac())
      return false;
    if (!checkException(throwable, trace, ArrayIndexOutOfBoundsException.class, TRACE_SIGNATURE))
      return false;
    if (!"-1".equals(Util.NN(throwable.getLocalizedMessage()).trim()))
      return false;
    return true;
  }
}
