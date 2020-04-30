package com.almworks.items.gui.edit.editors.composition;

import com.almworks.api.application.ItemKey;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.items.gui.edit.editors.enums.single.SingleEnumFieldEditor;
import com.almworks.util.LogHelper;

public abstract class SingleEnumDelegatingEditor<E extends SingleEnumFieldEditor> extends DelegatingFieldEditor<E> implements SingleEnumFieldEditor {
  private final DBAttribute<Long> myAttribute;
  private final EnumVariantsSource myVariants;

  protected SingleEnumDelegatingEditor(DBAttribute<Long> attribute, EnumVariantsSource variants) {
    myAttribute = attribute;
    myVariants = variants;
  }

  @Override
  public DBAttribute<Long> getAttribute() {
    return myAttribute;
  }

  @Override
  public EnumVariantsSource getVariants() {
    return myVariants;
  }

  @Override
  public void setValue(EditModelState model, ItemKey value) {
    ModelWrapper<E> wrapper = getWrapperModel(model);
    if (wrapper == null) {
      LogHelper.error("Missing wrapper model");
      return;
    }
    wrapper.getEditor().setValue(wrapper, value);
  }

  @Override
  public void setValueItem(EditModelState model, Long item) {
    ModelWrapper<E> wrapper = getWrapperModel(model);
    if (wrapper == null) {
      LogHelper.error("Missing wrapper model");
      return;
    }
    wrapper.getEditor().setValueItem(wrapper, item);
  }

  @Override
  public ItemKey getValue(EditModelState model) {
    ModelWrapper<E> wrapper = getWrapperModel(model);
    if (wrapper == null) {
      LogHelper.error("Missing wrapper model");
      return null;
    }
    return wrapper.getEditor().getValue(wrapper);
  }
}
