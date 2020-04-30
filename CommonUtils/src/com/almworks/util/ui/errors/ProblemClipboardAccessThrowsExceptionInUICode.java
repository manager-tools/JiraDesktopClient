package com.almworks.util.ui.errors;

public class ProblemClipboardAccessThrowsExceptionInUICode extends KnownProblem {
  private static final String[] SIGNATURE = {
    "sun.awt.windows.WClipboard", "openClipboard",
    "sun.awt.datatransfer.ClipboardTransferable", null,
    "sun.awt.datatransfer.SunClipboard", "getContents",
    "net.java.plaf.windows.common.TextComponentMenu", "updateActionEnablement"
  };

  public ProblemClipboardAccessThrowsExceptionInUICode() {
    super("clipboard_access_from_ui:http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4464162");
  }

  protected boolean isProblem(Throwable throwable, StackTraceElement[] trace) {
    return checkException(throwable, trace, IllegalStateException.class, SIGNATURE);
  }
}
