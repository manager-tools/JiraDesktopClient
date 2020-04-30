package com.almworks.items.sync.util.merge;

import com.almworks.items.sync.ItemAutoMerge;
import com.almworks.items.sync.ModifiableDiff;

public abstract class SimpleAutoMerge implements ItemAutoMerge {
  @Override
  public final void preProcess(ModifiableDiff local) {}
}
