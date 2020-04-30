package com.almworks.items.gui.meta;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemOrder;
import com.almworks.api.application.ResolvedItem;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.util.TypedMap;
import org.almworks.util.ArrayUtil;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Comparator;

public class LoadedItemKey extends ResolvedItem implements TypedMap {
  private final Object[] myAttributes;
  private final Object[] myValues;
  private final DBItemType myType;

  LoadedItemKey(
    long item, DBItemType type, @NotNull String representation, ItemOrder order, @Nullable String uniqueKey,
    @Nullable Icon icon, long connectionItem, Object[] attributes, Object[] values)
  {
    super(item, representation, order, uniqueKey, icon, connectionItem);
    myType = type;
    assert (attributes == null) == (values == null);
    assert attributes == null || attributes.length == values.length;
    myAttributes = attributes;
    myValues = values;
  }

  private Object getValue0(Object attribute) {
    if(myAttributes != null) {
      final int index = ArrayUtil.indexOf(myAttributes, attribute);
      if(index >= 0) {
        return myValues[index];
      }
    }
    return null;
  }

  public DBItemType getType() {
    return myType;
  }

  @SuppressWarnings({"unchecked"})
  public <T> T getValue(DBAttribute<T> attribute) {
    return (T)getValue0(attribute);
  }

  public int getValueCount() {
    return myAttributes != null ? myAttributes.length : 0;
  }

  public Object getAttributeAt(int index) {
    return myAttributes[index];
  }

  public Object getValueAt(int index) {
    return myValues[index];
  }

  @Override
  @SuppressWarnings({"unchecked"})
  public <T> T getValue(TypedKey<T> key) {
    return (T)getValue0(key);
  }

  @Override
  public boolean isSame(ResolvedItem that) {
    if (!super.isSame(that)) return false;
    LoadedItemKey other = Util.castNullable(LoadedItemKey.class, that);
    if (other == null) return false;
    int count = getValueCount();
    if (count != other.getValueCount()) return false;
    for (int i = 0; i < count; i++)
      if (!Util.equals(getAttributeAt(i), other.getAttributeAt(i)) || !Util.equals(getValueAt(i), other.getValueAt(i)))
        return false;
    return true;
  }

  public static Comparator<ItemKey> compareLoadedItems(Comparator<LoadedItemKey> comparator) {
    return (o1, o2) -> {
      LoadedItemKey i1 = Util.cast(LoadedItemKey.class, o1);
      LoadedItemKey i2 = Util.cast(LoadedItemKey.class, o2);
      if (i1 == null || i2 == null) {
        if (i1 == null && i2 == null) return ItemKey.COMPARATOR.compare(o1, o2);
        return i1 == null ? -1 : 1;
      }
      int cmp = comparator.compare(i1, i2);
      if (cmp != 0) return cmp;
      return ItemKey.COMPARATOR.compare(o1, o2);
    };
  }
}
