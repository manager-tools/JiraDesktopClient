package com.almworks.items.gui.edit.editors.enums.single;

import com.almworks.api.application.ItemKey;
import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.editors.composition.SingleEnumDelegatingEditor;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.util.Pair;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SingleEnumWithDefaultValueEditor<E extends SingleEnumFieldEditor> extends SingleEnumDelegatingEditor<E> {
  private final TypedKey<ItemKey> myWrappedOriginalValue;
  private final E myEditor;
  private final SingleEnumDefaultValue myValue;

  public SingleEnumWithDefaultValueEditor(E editor, SingleEnumDefaultValue value) {
    super(editor.getAttribute(), editor.getVariants());
    myEditor = editor;
    myValue = value;
    myWrappedOriginalValue = TypedKey.create(editor.getAttribute().getName() + "/wrappedOriginal");
  }

  public static <E extends SingleEnumFieldEditor> SingleEnumWithDefaultValueEditor<E> create(E editor, SingleEnumDefaultValue value) {
    return new SingleEnumWithDefaultValueEditor<E>(editor, value);
  }

  @Override
  protected E getDelegate(VersionSource source, EditModelState model) {
    return myEditor;
  }

  @Override
  protected void prepareWrapper(VersionSource source, ModelWrapper<E> wrapper, EditPrepare editPrepare) {
    super.prepareWrapper(source, wrapper, editPrepare);
    myValue.prepare(source, wrapper.getOriginalModel());
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    setupDefaultValue(model);
    return super.createComponents(life, model);
  }

  @Override
  public boolean hasDataToCommit(EditItemModel model) {
    if (super.hasDataToCommit(model)) return true;
    ItemKey originalValue = model.getValue(myWrappedOriginalValue);
    ModelWrapper<E> wrapper = getWrapperModel(model);
    if (wrapper == null) return false;
    return originalValue != null && !originalValue.equals(myEditor.getValue(wrapper));
  }

  private void setupDefaultValue(EditItemModel model) {
    ModelWrapper<E> wrapper = getWrapperModel(model);
    if (wrapper == null) return;
    EditItemModel valueModel = wrapper.getOriginalModel();
    Pair<? extends ItemKey,Long> pair = myValue.isEnabled(valueModel) ? myValue.getValue(valueModel) : null;
    if (pair == null) return;
    ItemKey itemKey = pair.getFirst();
    Long itemObj = pair.getSecond();
    long item = itemObj != null && itemObj > 0 ? itemObj : 0;
    if (itemKey != null || item > 0) {
      ItemKey originalKey = myEditor.getValue(wrapper);
      model.putHint(myWrappedOriginalValue, originalKey);
      if (itemKey != null) myEditor.setValue(wrapper, itemKey);
      else if (item > 0) myEditor.setValueItem(wrapper, item);
    }
  }
}
