package com.almworks.items.impl.dbadapter;

import com.almworks.integers.IntList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * For each grouping, distribution of items between groups is stored in IntList.
 * @author igor baltiyskiy
 */
public class GroupsDist extends AbstractGroupsDist {
  /**
   * List of groupings. Each grouping consists of group IDs for each item.
   */
  private final List<? extends IntList> myGroups;

  public GroupsDist(@NotNull List<? extends IntList> groups) {
    assert groups != null;
    myGroups = groups;
  }

  @NotNull
  protected List<? extends IntList> getBackingList() {
    return myGroups;
  }

  @NotNull
  @Override
  public AbstractGroupsDist undecorate() {
    return this;
  }
}
