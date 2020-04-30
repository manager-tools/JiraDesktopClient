package com.almworks.util.ui.errors;

import com.almworks.util.Env;

public class ProblemMacNPEWhenDragging extends KnownProblem {
  private static final String[] TRACE_SIGNATURE = new String[] {
    "apple.laf.AquaTreeUI$TreeArrowMouseInputHandler", "paintOneControl",
    "apple.laf.AquaTreeUI$TreeArrowMouseInputHandler", "mouseDragged"};

  public ProblemMacNPEWhenDragging() {
    super("mac:AquaTreeUI$TreeArrowMouseInputHandler:paint_while_dragging");
  }

  protected boolean isProblem(Throwable throwable, StackTraceElement[] trace) {
    if (!Env.isMac())
      return false;
    return checkException(throwable, trace, NullPointerException.class, TRACE_SIGNATURE);
  }
}
