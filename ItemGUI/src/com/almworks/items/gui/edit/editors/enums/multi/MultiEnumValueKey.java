package com.almworks.items.gui.edit.editors.enums.multi;

import com.almworks.api.application.ItemKey;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.util.AttributeMap;
import com.almworks.util.Pair;
import com.almworks.util.collections.LongSet;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MultiEnumValueKey {
  private final TypedKey<LongList> myDBKey;
  private final TypedKey<List<ItemKey>> myItemKey;
  private final TypedKey<Boolean> myCommonValues;
  private final DBAttribute<Set<Long>> myAttribute;

  public MultiEnumValueKey(DBAttribute<Set<Long>> attribute) {
    myAttribute = attribute;
    String debugName = attribute.getName();
    myDBKey = TypedKey.create(debugName + "/db");
    myItemKey = TypedKey.create(debugName + "/key");
    myCommonValues = TypedKey.create(debugName + "/common");
  }

  public DBAttribute<Set<Long>> getAttribute() {
    return myAttribute;
  }

  public void loadValue(VersionSource source, EditItemModel model) {
    Pair<LongList,Boolean> commonValue = prepareCommonValue(source, model);
    setValue(model, commonValue.getFirst());
    setAllCommonValue(model, commonValue.getSecond());
  }

  @NotNull
  private Pair<LongList,Boolean> prepareCommonValue(VersionSource source, EditItemModel model) {
    if (model.isNewItem()) {
      AttributeMap defaults = model.getValue(EditItemModel.DEFAULT_VALUES);
      Set<Long> values = defaults != null ? defaults.get(myAttribute) : null;
      if (values == null) return Pair.create(LongList.EMPTY, true);
      LongSet longList = LongSet.create(values);
      longList.remove(0);
      return Pair.<LongList,Boolean>create(longList, true);
    }
    LongList items = model.getEditingItems();
    Pair<LongList, LongList> commonSet = FieldEditorUtil.getCommonSet(source, myAttribute, items);
    if (items.size() < 2) return commonSet != null ? Pair.create(commonSet.getFirst(), true) : Pair.create(LongList.EMPTY, true);
    if (commonSet == null) return Pair.create(LongList.EMPTY, false);
    return Pair.create(commonSet.getFirst(), commonSet.getSecond().isEmpty());
  }

  public void setAllCommonValue(EditModelState model, boolean common) {
    model.putHint(myCommonValues, common);
  }
  
  public boolean isAllCommonValues(EditModelState model) {
    return Boolean.TRUE.equals(model.getValue(myCommonValues));
  }

  @NotNull
  public List<ItemKey> getItemKeysValue(EditModelState model, EnumVariantsSource variants) {
    List<ItemKey> keys = model.getValue(myItemKey);
    if (keys == null) {
      LongList dbValues = model.getValue(myDBKey);
      if (dbValues == null || dbValues.isEmpty()) keys = Collections.emptyList();
      else {
        keys = Collections15.arrayList();
        for (LongIterator it : dbValues) {
          LoadedItemKey key = variants.getResolvedItem(model, it.value());
          if (key != null) keys.add(key);
        }
      }
    }
    return keys.isEmpty() ? Collections.<ItemKey>emptyList() : Collections.unmodifiableList(keys);
  }

  @NotNull
  public List<ItemKey> getSelectedItemKeys(EditModelState model) {
    return Collections.unmodifiableList(Util.NN(model.getValue(myItemKey), Collections.<ItemKey>emptyList()));
  }

  @NotNull
  public LongList getSelectedItems(EditModelState model) {
    return Util.NN(model.getValue(myDBKey), LongList.EMPTY);
  }

  public void setValue(EditModelState model, List<ItemKey> itemKeys) {
    LongArray items = new LongArray();
    itemKeys = Collections15.arrayList(itemKeys);
    for (Iterator<ItemKey> it = itemKeys.iterator(); it.hasNext();) {
      ItemKey key = it.next();
      if (key == null) continue;
      long item = key.getItem();
      if (item > 0) {
        if (items.contains(item)) it.remove();
        else items.add(item);
      }
    }
    items.sortUnique();
    model.putValues(myDBKey, items, myItemKey, itemKeys);
  }

  public void setValue(EditModelState model, LongList items) {
    LongArray copy = LongArray.copy(items);
    copy.sortUnique();
    model.putValues(myDBKey, copy, myItemKey, null);
  }

  public boolean isChanged(EditItemModel model) {
    LongList originalItems = Util.NN(model.getInitialValue(myDBKey), LongList.EMPTY);
    List<ItemKey> keys = model.getValue(myItemKey);
    if (keys != null) {
      LongArray resolved = new LongArray();
      for (ItemKey key : keys) {
        long item = key.getResolvedItem();
        if (item <= 0) return true;
        resolved.add(item);
      }
      resolved.sortUnique();
      return !resolved.equals(originalItems);
    }
    return !getSelectedItems(model).equals(originalItems);
  }

  public boolean hasValue(EditModelState model) {
    return !getSelectedItems(model).isEmpty() || !getSelectedItemKeys(model).isEmpty();
  }
}
