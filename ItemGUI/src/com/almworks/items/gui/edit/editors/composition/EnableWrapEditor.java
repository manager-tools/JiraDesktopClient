package com.almworks.items.gui.edit.editors.composition;

import com.almworks.items.gui.edit.*;
import com.almworks.items.sync.VersionSource;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EnableWrapEditor extends DelegatingFieldEditor<FieldEditor> {
  private final FieldEditor myEditor;
  private final boolean myEnabled;

  private EnableWrapEditor(FieldEditor editor, boolean enabled) {
    myEditor = editor;
    myEnabled = enabled;
  }

  public static FieldEditor disabled(FieldEditor editor) {
    return new EnableWrapEditor(editor, false);
  }

  public static FieldEditor alwaysEnabled(FieldEditor editor) {
    return new EnableWrapEditor(editor, true);
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
    return ComponentControl.EnableWrapper.wrapAll(myEnabled, components);
  }

  @Override
  public boolean isChanged(EditItemModel model) {
    return myEnabled && super.isChanged(model);
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
    if (!myEnabled) return;
    super.verifyData(verifyContext);
  }

  @Override
  public void commit(CommitContext context) throws CancelCommitException {
    if (myEnabled) super.commit(context);
  }
}
