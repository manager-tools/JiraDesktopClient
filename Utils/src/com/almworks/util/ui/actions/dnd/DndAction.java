package com.almworks.util.ui.actions.dnd;

import java.awt.dnd.DnDConstants;

public enum DndAction {
  COPY(DnDConstants.ACTION_COPY),
  MOVE(DnDConstants.ACTION_MOVE),
  LINK(DnDConstants.ACTION_LINK);

  private int myAwtCode;

  DndAction(int awtCode) {
    myAwtCode = awtCode;
  }

  public int getAwtCode() {
    return myAwtCode;
  }
}
