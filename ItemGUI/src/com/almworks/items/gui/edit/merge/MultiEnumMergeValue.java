package com.almworks.items.gui.edit.merge;

import com.almworks.api.application.ItemKey;
import com.almworks.integers.LongIterator;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.editors.enums.multi.BaseMultiEnumEditor;
import com.almworks.util.collections.LongSet;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.renderer.CellState;
import org.almworks.util.Collections15;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MultiEnumMergeValue extends MergeValue.Simple {
  private final EditItemModel myModel;
  private final LongSet[] myVersions;
  private final BaseMultiEnumEditor myEditor;

  private MultiEnumMergeValue(String displayName, EditItemModel model, BaseMultiEnumEditor editor, LongSet[] versions, long item) {
    super(displayName, item);
    myModel = model;
    myEditor = editor;
    myVersions = versions;
  }

  public static MergeValue load(DBReader reader, EditItemModel model, BaseMultiEnumEditor editor) {
    long item = getSingleItem(model);
    if (item <= 0) return null;
    String displayName = editor.getLabelText(model).getText();
    LongSet[] versions = new LongSet[3];
    for (int i = 0; i < 3; i++) {
      DBAttribute<? extends Collection<Long>> attribute = editor.getAttribute();
      Collection<Long> items = loadValue(reader, item, attribute, i);
      LongSet set;
      if (items == null || items.isEmpty()) set = new LongSet(0);
      else set = LongSet.create(items);
      versions[i] = set;
    }
    return new MultiEnumMergeValue(displayName, model, editor, versions, item);
  }

  private ItemKey getItemKey(long item) {
    return myEditor.getVariants().getResolvedItem(myModel, item);
  }

  @Override
  public void render(CellState state, Canvas canvas, int version) {
    LongSet items = myVersions[version];
    if (items.isEmpty()) return;
    boolean first = true;
    for (LongIterator ii : items) {
      if (!first) canvas.appendText(",");
      first = false;
      long item = ii.value();
      if (item <= 0) continue;
      ItemKey itemKey = getItemKey(item);
      if (itemKey != null) itemKey.renderOn(canvas, state);
      else canvas.appendText("???");
    }
  }

  @Override
  protected void doSetResolution(int version) {
    LongSet values = myVersions[version];
    if (values.isEmpty()) myEditor.setValue(myModel, Collections15.<ItemKey>emptyList());
    else {
      List<ItemKey> keys = Collections15.arrayList();
      for (LongIterator li : values) {
        long item = li.value();
        ItemKey key;
        if (item <= 0) key = null;
        else {
          key = getItemKey(item);
          if (key == null) {
            keys = null;
            break;
          }
        }
        keys.add(key);
      }
      if (keys == null) myEditor.setItemValue(myModel, values);
      else myEditor.setValue(myModel, keys);
    }
  }

  @Override
  public boolean isConflict() {
    LongSet local = myVersions[LOCAL];
    LongSet base = myVersions[BASE];
    LongSet remote = myVersions[REMOTE];
    return !local.equals(base) && !remote.equals(base) && !local.equals(remote);
  }

  @Override
  public boolean isChanged(boolean remote) {
    LongSet base = myVersions[BASE];
    LongSet notBase = myVersions[remote ? REMOTE : LOCAL];
    return !base.equals(notBase);
  }

  @Override
  public Object getValue(int version) {
    ArrayList<Object> result = Collections15.arrayList();
    LongSet values = myVersions[version];
    for (LongIterator li : values) {
      ItemKey key = getItemKey(li.value());
      result.add(key);
    }
    return result;
  }

  @Override
  protected FieldEditor getEditor() {
    return myEditor;
  }

  @Override
  protected EditItemModel getModel() {
    return myModel;
  }
}
