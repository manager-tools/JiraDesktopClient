package com.almworks.api.application.tree;

import com.almworks.api.application.UiItem;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.sync.util.AggregatingEditCommit;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;

import javax.swing.*;
import java.util.Collection;

public interface TagNode extends RenamableNode {
  String getName();

  boolean isEditable();

  boolean editNode(ActionContext context) throws CantPerformException;

  String getIconPath();

  Icon getIcon();

  DBIdentifiedObject getTagDbObj();

  @ThreadAWT
  long getTagItem();

  /**
   * Sets or clears corresponding tag for the specified items.
   * These actions are attached to the specified commit procedure. 
   *
   * @param set true: set, false: clear
   * @param commit commit procedure
   * @return the same commit procedure
   */
  @ThreadAWT
  AggregatingEditCommit setOrClearTag(Collection<? extends UiItem> items, boolean set, AggregatingEditCommit commit);
}
