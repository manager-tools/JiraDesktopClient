package com.almworks.items.sync.util.merge;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.sync.AutoMergeData;
import com.almworks.items.sync.ItemAutoMerge;
import com.almworks.items.sync.ModifiableDiff;
import com.almworks.util.collections.Containers;
import org.almworks.util.ArrayUtil;

import java.util.Collection;

public class AutoMergeConflictGroup implements ItemAutoMerge {
  private final DBAttribute<?>[] myAttributes;

  public AutoMergeConflictGroup(DBAttribute<?> ... attributes) {
    myAttributes = ArrayUtil.arrayCopy(attributes);
  }

  @Override
  public void preProcess(ModifiableDiff local) {
    if (hasAny(local.getChanged())) local.addChange(myAttributes);
  }

  public boolean hasAny(Collection<? extends DBAttribute<?>> changes) {
    return Containers.intersects(changes, myAttributes);
  }

  @Override
  public void resolve(AutoMergeData data) {}
}
