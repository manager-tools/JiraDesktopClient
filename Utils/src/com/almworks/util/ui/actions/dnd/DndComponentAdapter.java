package com.almworks.util.ui.actions.dnd;

public interface DndComponentAdapter<H extends DropHint> {
  void setDndActive(boolean dndActive, boolean dndEnabled);

  boolean isDndWorking();

  void setDropHint(H hint);
}
