package com.almworks.util.ui.actions.dnd;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

public class DndEvent {
  private final boolean myDndActive;
  private final DragContext myContext;
  private final InputEvent myEvent;

  public DndEvent(boolean dndActive, DragContext context, InputEvent event) {
    myDndActive = dndActive;
    myContext = context;
    myEvent = event;
  }

  public DragContext getContext() {
    return myContext;
  }

  public boolean isDndActive() {
    return myDndActive;
  }

  @Nullable
  public InputEvent getInputEvent() {
    return myEvent;
  }

  @Nullable
  public Point getMousePointFor(Component c) {
    InputEvent event = myEvent;
    if (event instanceof MouseEvent) {
      Point p = ((MouseEvent) event).getPoint();
      Component source = event.getComponent();
      if (p != null && source != null) {
        return SwingUtilities.convertPoint(source, p, c);
      }
    }
    return null;
  }
}
