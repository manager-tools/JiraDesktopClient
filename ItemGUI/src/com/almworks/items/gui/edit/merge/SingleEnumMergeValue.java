package com.almworks.items.gui.edit.merge;

import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.editors.enums.single.SingleEnumFieldEditor;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.util.LogHelper;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.renderer.CellState;
import org.jetbrains.annotations.Nullable;

public class SingleEnumMergeValue extends MergeValue.Simple {
  private final EditItemModel myModel;
  private final long[] myVersions;
  private final SingleEnumFieldEditor myEditor;

  private SingleEnumMergeValue(String displayName, EditItemModel model, SingleEnumFieldEditor editor, long[] versions, long item)
  {
    super(displayName, item);
    myModel = model;
    myEditor = editor;
    myVersions = versions;
  }

  public static MergeValue load(DBReader reader, EditItemModel model, SingleEnumFieldEditor editor) {
    LogHelper.assertError(model.getAllEditors().contains(editor), editor);
    long item = getSingleItem(model);
    if (item <= 0) return null;
    String displayName = editor.getLabelText(model).getText();
    long[] versions = new long[3];
    for (int i = 0; i < 3; i++) {
      Long enumValue = loadValue(reader, item, editor.getAttribute(), i);
      long versionValue;
      if (enumValue == null || enumValue <= 0) versionValue = 0;
      else versionValue = enumValue;
      versions[i] = versionValue;
    }
    return new SingleEnumMergeValue(displayName, model, editor, versions, item);
  }

  @Nullable
  private LoadedItemKey getItemKey(long item) {
    return myEditor.getVariants().getResolvedItem(myModel, item);
  }

  @Override
  public void render(CellState state, Canvas canvas, int version) {
    long item = myVersions[version];
    if (item <= 0) return;
    LoadedItemKey itemKey = getItemKey(item);
    if (itemKey == null) canvas.appendText("???");
    else itemKey.renderOn(canvas, state);
  }

  @Override
  protected void doSetResolution(int version) {
    long item = myVersions[version];
    LoadedItemKey itemKey = getItemKey(item);
    if (itemKey != null) myEditor.setValue(myModel, itemKey);
    else myEditor.setValueItem(myModel, item);
  }

  @Override
  public boolean isConflict() {
    long local = myVersions[LOCAL];
    long base = myVersions[BASE];
    long remote = myVersions[REMOTE];
    return local != base && remote != base && local != remote;
  }

  @Override
  public boolean isChanged(boolean remote) {
    long base = myVersions[BASE];
    long notBase = myVersions[remote ? REMOTE : LOCAL];
    return base != notBase;
  }

  @Override
  public Object getValue(int version) {
    long item = myVersions[version];
    LoadedItemKey key = getItemKey(item);
    return key != null ? key : (item <= 0 ? null : item);
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
