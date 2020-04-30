package com.almworks.util.components;

import org.almworks.util.detach.Lifecycle;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author dyoma
 */
public class InputEventController {
  private final Lifecycle myHandlerCycle = new Lifecycle();
  private final JComponent myHost;
  private Cursor mySavedCursor = null;
  private boolean myCursorSet = false;

  public InputEventController(JComponent host) {
    myHost = host;
  }

  public void setHandler(ComponentInputHandler handler) {
    myHandlerCycle.cycle();
    resetState();
    handler.subscribe(myHandlerCycle.lifespan(), myHost, this);
  }

  private void resetState() {
    setCursor(null);
  }

  public void setCursor(@Nullable Cursor cursor) {
    if (cursor == null) {
      if (myCursorSet) {
        assert mySavedCursor != null;
        myCursorSet = false;
        myHost.setCursor(mySavedCursor);
        mySavedCursor = null;
      } else
        assert mySavedCursor == null;
      return;
    }
    Cursor currentCursor = myHost.getCursor();
    if (mySavedCursor == null)
      mySavedCursor = currentCursor;
    myCursorSet = true;
    if (cursor.equals(currentCursor))
      return;
    myHost.setCursor(cursor);
  }
}
