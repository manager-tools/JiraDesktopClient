package com.almworks.items.gui.edit.merge;

import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.util.LogHelper;
import org.almworks.util.Util;

public abstract class BaseScalarMergeValue<T> extends MergeValue.Simple {
  private final EditItemModel myModel;
  private final T[] myValues;

  public BaseScalarMergeValue(String displayName, long item, T[] values, EditItemModel model) {
    super(displayName, item);
    myValues = values;
    myModel = model;
  }

  @Override
  public boolean isConflict() {
    T local = getValue(LOCAL);
    T base = getValue(BASE);
    T remote = getValue(REMOTE);
    return !Util.equals(local, base) && !Util.equals(remote, base) && !Util.equals(local, remote);
  }

  @Override
  public boolean isChanged(boolean remote) {
    T base = getValue(BASE);
    T notBase = getValue(remote ? REMOTE : LOCAL);
    return !Util.equals(base, notBase);
  }

  public T getValue(int version) {
    if (version < 0 || version >= 3) {
      LogHelper.error("Illegal version", version);
      return null;
    }
    return myValues[version];
  }

  @Override
  protected EditItemModel getModel() {
    return myModel;
  }
}
