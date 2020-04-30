package com.almworks.jira.provider3.remotedata.issue.fields;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.sync.download2.rest.LoadedEntity;
import com.almworks.util.Pair;
import com.almworks.util.collections.LongSet;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public abstract class EnumDifferenceValue extends DifferenceValue<LoadedEntity> {
  public EnumDifferenceValue(List<LoadedEntity> add, List<LoadedEntity> remove, List<LoadedEntity> newValue) {
    super(add, remove, newValue);
  }

  @Override
  protected String extractFormId(@NotNull LoadedEntity value) {
    return value.getFormValueId();
  }

  /**
   * @return editable sets of items to add and to remove
   */
  public static Pair<LongSet, LongSet> readAddRemove(DBAttribute<Set<Long>> attribute, ItemVersion trunk, ItemVersion base) {
    LongList change = trunk.getLongSet(attribute);
    LongList original = base.getLongSet(attribute);
    LongSet add = LongSet.setDifference(change, original);
    LongSet remove = LongSet.setDifference(original, change);
    return Pair.create(add, remove);
  }
}
