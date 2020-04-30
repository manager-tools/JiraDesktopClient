package com.almworks.timetrack.gui.timesheet;

import com.almworks.api.application.ItemKey;
import org.almworks.util.Collections15;

import java.util.Collections;
import java.util.List;

public class GroupTaskEntry extends TaskEntry {
  private final ItemKey myGroupValue;
  private final List<TaskEntry> myChildren = Collections15.arrayList();

  public GroupTaskEntry(GroupTaskEntry parent, ItemKey groupValue) {
    super(parent);
    myGroupValue = groupValue;
  }

  public ItemKey getGroupValue() {
    return myGroupValue;
  }

  public void addChild(TaskEntry taskEntry) {
    myChildren.add(taskEntry);
  }

  public List<TaskEntry> getChildren() {
    return Collections.unmodifiableList(myChildren);
  }

  @Override
  public String toString() {
    return myGroupValue.getDisplayName();
  }
}
