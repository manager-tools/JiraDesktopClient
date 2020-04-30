package com.almworks.util.ui.macosx;

import org.almworks.util.Util;

import java.awt.*;
import java.util.EventObject;

public class FullScreenEvent extends EventObject {
  private final Type myType;

  public FullScreenEvent(Window source, Type type) {
    super(source);
    myType = type;
  }

  public Window getWindow() {
    return Util.castNullable(Window.class, getSource());
  }

  public Type getType() {
    return myType;
  }

  public static enum Type {
    ENTERING, ENTERED, EXITING, EXITED
  }

  public static interface Listener {
    void onFullScreenEvent(FullScreenEvent e);
  }
}
