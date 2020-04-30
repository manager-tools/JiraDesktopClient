package com.almworks.items.impl.dbadapter;

import com.almworks.integers.IntList;
import com.almworks.integers.IntListInsertingDecorator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Decorates insertions into distribution of items between groups.
 *
 * @author igor baltiyskiy
 */
public class GroupsDistInsertingDecorator extends AbstractGroupsDist {
  private final List<IntListInsertingDecorator> myGroupsDecorator;

  @NotNull
  protected List<? extends IntList> getBackingList() {
    return myGroupsDecorator;
  }

  public GroupsDistInsertingDecorator(@NotNull AbstractGroupsDist groups) {
    assert groups != null;
    myGroupsDecorator = new ArrayList<IntListInsertingDecorator>(groups.groupingsCount());
    for (int i = 0; i < groups.groupingsCount(); ++i) {
      myGroupsDecorator.add(new IntListInsertingDecorator(groups.getGrouping(i)));
    }
  }

  @NotNull
  public AbstractGroupsDist undecorate() {
    return defaultUndecorate(this);
  }

  /**
   * Inserts group IDs for all specified groupings for the specified item index.
   *
   * @param itemIndex
   * @param groupIds
   */
  public void insert(int itemIndex, IntList groupIds) {
    assert groupIds.size() == groupingsCount() : groupIds.size() + " " + groupingsCount();
    for (int i = 0; i < groupIds.size(); ++i) {
      myGroupsDecorator.get(i).insert(itemIndex, groupIds.get(i));
    }
  }
}
