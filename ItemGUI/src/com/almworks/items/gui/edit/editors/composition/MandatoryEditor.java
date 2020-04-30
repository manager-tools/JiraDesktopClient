package com.almworks.items.gui.edit.editors.composition;

import com.almworks.items.gui.edit.*;
import com.almworks.items.sync.VersionSource;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MandatoryEditor extends DelegatingFieldEditor<FieldEditor> {
  private final FieldEditor myEditor;
  private final String myEmptyValueError;

  public MandatoryEditor(FieldEditor editor, String emptyValueError) {
    myEditor = editor;
    myEmptyValueError = emptyValueError;
  }

  @Override
  protected FieldEditor getDelegate(VersionSource source, EditModelState model) {
    return myEditor;
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    List<? extends ComponentControl> components = super.createComponents(life, model);
    if (components.isEmpty()) return components;
    return ComponentControl.EnableWrapper.wrapAll(true, components);
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
    ModelWrapper<FieldEditor> wrapper = getWrapperModel(verifyContext.getModel());
    if (wrapper == null) return;
    DataVerification subContext = verifyContext.subContext(wrapper);
    FieldEditor editor = wrapper.getEditor();
    editor.verifyData(subContext);
    if (subContext.hasErrors()) return;
    if (!editor.hasValue(wrapper)) subContext.addError(editor, myEmptyValueError);
  }
}
