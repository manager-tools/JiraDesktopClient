package com.almworks.engine.gui;

import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnActionListener;
import com.almworks.util.ui.actions.CantPerformException;

import javax.swing.*;

public class CellAction implements AnActionListener {
  private final Icon myIcon;
  private final String myTooltip;
  private final AnActionListener myListener;

  public CellAction(Icon icon, String tooltip, AnActionListener listener) {
    myIcon = icon;
    myTooltip = tooltip;
    myListener = listener;
  }

  public CellAction(Icon icon, String tooltip) {
    myIcon = icon;
    myTooltip = tooltip;
    myListener = null;
  }

  public void perform(ActionContext context) throws CantPerformException {
    if (myListener != null) {
      myListener.perform(context);
    }
  }

  public Icon getIcon() {
    return myIcon;
  }

  public String getTooltip() {
    return myTooltip;
  }
}
