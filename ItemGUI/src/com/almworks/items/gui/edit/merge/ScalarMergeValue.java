package com.almworks.items.gui.edit.merge;

import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.util.BaseScalarFieldEditor;
import com.almworks.util.LogHelper;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.renderer.CellState;

public class ScalarMergeValue<T>  extends BaseScalarMergeValue<T>{
  private final BaseScalarFieldEditor<T> myEditor;

  private ScalarMergeValue(String displayName, EditItemModel model, BaseScalarFieldEditor<T> editor, T[] values, long item) {
    super(displayName, item, values, model);
    myEditor = editor;
  }

  @Override
  public void render(CellState state, Canvas canvas, int version) {
    T value = getValue(version);
    String text = myEditor.convertToText(value);
    if (text != null) canvas.appendText(text);
  }

  @Override
  protected void doSetResolution(int version) {
    myEditor.setValue(getModel(), getValue(version));
  }

  @Override
  protected FieldEditor getEditor() {
    return myEditor;
  }

  public static <T> MergeValue load(DBReader reader, EditItemModel model, BaseScalarFieldEditor<T> editor) {
    LogHelper.assertError(model.getAllEditors().contains(editor), editor);
    long item = getSingleItem(model);
    if (item <= 0) return null;
    String displayName = editor.getLabelText(model).getText();
    T[] values = (T[]) new Object[3];
    for (int i = 0; i < 3; i++) values[i] = loadValue(reader, item, editor.getAttribute(), i);
    return new ScalarMergeValue<T>(displayName, model, editor, values, item);
  }
}
