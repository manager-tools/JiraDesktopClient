package com.almworks.items.gui.edit.editors;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.util.collections.Convertor;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

import java.util.List;

/**
 * Provides value for the attribute, does not add any UI, does not modify model<br>
 * Use this editor to make attribute value available to other editors when no actual editor is required.<br>
 * <b>UseCase:</b> some editor value set depends on another attribute value. If an editor for the attribute presents in the model it provide the value, but if such editor is absent
 * then dependent editor cannot obtain the attribute value. Use this editor in the case to just provide a value.
 */
public class LoadAttribute extends MockEditor {
  private final DBAttribute<Long> myAttribute;
  private final TypedKey<TLongObjectHashMap<Long>> myAllValues;
  private final Convertor<EditModelState,LongList> myConvertor;

  public LoadAttribute(DBAttribute<Long> attribute) {
    myAttribute = attribute;
    myAllValues = TypedKey.create(attribute.getName() + "/allValues");
    myConvertor = new Convertor<EditModelState, LongList>() {
      @Override
      public LongList convert(EditModelState model) {
        TLongObjectHashMap<Long> allValues = model.getValue(myAllValues);
        if (allValues == null || allValues.isEmpty()) return null;
        LongArray result = new LongArray();
        for (Object obj : allValues.getValues()) {
          Long value = (Long) obj;
          if (value == null || value <= 0) continue;
          result.add(value);
        }
        result.sortUnique();
        return result;
      }
    };
  }

  @Override
  public void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
    LongList items = model.getEditingItems();
    TLongObjectHashMap<Long> allValues = new TLongObjectHashMap<>();
    List<Long> values = source.collectValues(myAttribute, items);
    for (int i = 0; i < values.size(); i++) {
      Long v = values.get(i);
      if (v != null && v <= 0) v = null;
      allValues.put(items.get(i), v);
    }
    model.putHint(myAllValues, allValues);
    model.registerEditor(this);
    model.registerSingleEnum(myAttribute, myConvertor);
  }

  @Override
  public void onItemsChanged(EditItemModel model, TLongObjectHashMap<ItemValues> newValues) {
    TLongObjectHashMap<Long> values = model.getValue(myAllValues);
    if (values == null) return;
    TLongObjectHashMap<Long> copy = null;
    for (long item : values.keys()) {
      ItemValues itemValues = newValues.get(item);
      if (itemValues == null || itemValues.indexOf(myAttribute) < 0) continue;
      Long replacement = itemValues.getValue(myAttribute);
      if (replacement != null && replacement <= 0) replacement = null;
      Long current = values.get(item);
      if (Util.equals(current, replacement)) continue;
      if (copy == null) copy = values.clone();
      copy.put(item, replacement);
    }
    if (copy != null) model.putValue(myAllValues, copy);
  }
}
