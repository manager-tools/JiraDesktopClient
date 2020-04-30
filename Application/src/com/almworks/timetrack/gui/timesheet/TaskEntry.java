package com.almworks.timetrack.gui.timesheet;

public abstract class TaskEntry {
  private final GroupTaskEntry myParent;
  private final int myDepth;

  protected TaskEntry(GroupTaskEntry parent) {
    myParent = parent;
    myDepth = parent == null ? 0 : parent.getDepth() + 1;
    if (parent != null) {
      parent.addChild(this);
    }
  }

  public GroupTaskEntry getParent() {
    return myParent;
  }

  public int getDepth() {
    return myDepth;
  }
}
