package com.almworks.items.gui.edit.util;

import com.almworks.items.gui.edit.EditModelState;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

public class PerItemModelValue<T> {
  private final TypedKey<TLongObjectHashMap<T>> myKey;
  private final boolean myHint;

  public PerItemModelValue(String debugName, boolean hint) {
    myKey = TypedKey.create(debugName);
    myHint = hint;
  }

  public static PerItemModelValue<Long> hint(String debugName) {
    return new PerItemModelValue<Long>(debugName, true);
  }

  public void put(EditModelState model, long item, T value) {
    TLongObjectHashMap<T> map = model.getValue(myKey);
    if (map == null) {
      map = new TLongObjectHashMap<>();
      if (myHint) model.putHint(myKey, map);
      else model.putValue(myKey, map);
    }
    if (value != null) map.put(item, value);
    else map.remove(item);
    if (!myHint) model.fireChanged();
  }

  @Nullable
  public T get(EditModelState model, long item) {
    TLongObjectHashMap<T> map = model.getValue(myKey);
    return map != null ? map.get(item) : null;
  }
}
