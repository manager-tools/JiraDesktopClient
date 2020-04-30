package com.almworks.items.gui.edit.util;

import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.sync.VersionSource;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;

import java.util.HashSet;

public class CommonValueKey<T> {
  private final TypedKey<Boolean> myAllSame;
  private final TypedKey<Boolean> myHasNullValue;
  private final TypedKey<T> myCommonValue;

  public CommonValueKey(String debugName) {
    myAllSame = TypedKey.create(debugName + "/allSameValue");
    myHasNullValue = TypedKey.create(debugName + "/someHasNull");
    myCommonValue = TypedKey.create(debugName + "/commonValue");
  }

  public T loadValue(VersionSource source, EditModelState model, DBAttribute<T> attribute, LongList items) {
    HashSet<T> allValues = Collections15.hashSet();
    for (LongIterator cursor : items) {
      allValues.add(source.forItem(cursor.value()).getValue(attribute));
    }
    model.putHint(myAllSame, allValues.size() <= 1);
    model.putHint(myHasNullValue, allValues.contains(null));
    T commonValue;
    if (allValues.size() == 1) commonValue = allValues.iterator().next();
    else {
      allValues.remove(null);
      commonValue = allValues.size() == 1 ? allValues.iterator().next() : null;
    }
    model.putHint(myCommonValue, commonValue);
    return commonValue;
  }

  public void setValueIsCommon(EditItemModel model) {
    model.putValue(myAllSame, true);
  }

  public ComponentControl.Enabled getComponentEnabledState(EditModelState model, boolean forbidNull) {
    if (model.getEditingItems().size() < 2) return ComponentControl.Enabled.NOT_APPLICABLE;
    if (forbidNull && hasNull(model)) return ComponentControl.Enabled.NOT_APPLICABLE;
    boolean allSame = Boolean.TRUE.equals(model.getValue(myAllSame));
    return allSame ? ComponentControl.Enabled.ENABLED : ComponentControl.Enabled.DISABLED;
  }

  public boolean hasNull(EditModelState model) {
    return Boolean.TRUE.equals(model.getValue(myHasNullValue));
  }

  public boolean isAllSame(EditModelState model) {
    return Boolean.TRUE.equals(model.getValue(myAllSame));
  }

  public T getCommonValue(EditModelState model) {
    return model.getValue(myCommonValue);
  }
}
