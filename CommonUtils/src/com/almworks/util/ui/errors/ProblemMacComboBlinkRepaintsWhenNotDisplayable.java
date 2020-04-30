package com.almworks.util.ui.errors;

import com.almworks.util.Env;

public class ProblemMacComboBlinkRepaintsWhenNotDisplayable extends KnownProblem {
  private static final String[] TRACE_TOP = {
    "com.apple.laf.AquaListUI", "repaintCell",
    "com.apple.laf.AquaComboBoxUI$CoreAquaItemListener$1", "paintSelected",
    "com.apple.laf.AquaUtils", "blinkMenu",
    "com.apple.laf.AquaComboBoxUI$CoreAquaItemListener", "itemStateChanged" };

  public ProblemMacComboBlinkRepaintsWhenNotDisplayable() {
    super("mac:repainting_not_displayable_combo_popup");
  }

  @Override
  protected boolean isProblem(Throwable throwable, StackTraceElement[] trace) {
    return Env.isMac() && throwable != null
      && checkException(throwable, throwable.getStackTrace(), NullPointerException.class, TRACE_TOP);
  }
}
