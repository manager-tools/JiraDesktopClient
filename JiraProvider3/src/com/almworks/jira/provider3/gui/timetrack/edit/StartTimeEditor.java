package com.almworks.jira.provider3.gui.timetrack.edit;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.editors.composition.DelegatingFieldEditor;
import com.almworks.items.gui.edit.editors.scalar.DateEditor;
import com.almworks.items.gui.edit.editors.scalar.TimeSpentEditor;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.ADateField;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Date;
import java.util.List;

class StartTimeEditor extends DelegatingFieldEditor<DateEditor> {
  private final DateEditor myEditor;
  private final TypedKey<Long> myBaseValue;
  private final TimeSpentEditor myControlling;

  public StartTimeEditor(NameMnemonic labelText, DBAttribute<Date> attribute, TimeSpentEditor controlling, long datePrecision) {
    myControlling = controlling;
    myEditor = DateEditor.createDateTime(labelText, attribute);
    myBaseValue = TypedKey.create(attribute.getId() + "/base");
  }

  @Override
  protected DateEditor getDelegate(VersionSource source, EditModelState model) {
    return myEditor;
  }

  @Override
  protected void prepareWrapper(VersionSource source, ModelWrapper<DateEditor> wrapper, EditPrepare editPrepare) {
    super.prepareWrapper(source, wrapper, editPrepare);
    if (wrapper.isNewItem()) {
      Date baseTime = new Date();
      wrapper.getOriginalModel().putHint(myBaseValue, baseTime.getTime());
    }
  }

  @Override
  public boolean isChanged(EditItemModel model) {
    return !isAutoUpdate(model) && super.isChanged(model);
  }

  private boolean isAutoUpdate(EditModelState model) {
    if (getBaseValue(model) == null) return false;
    ModelWrapper<DateEditor> wrapper = getWrapperModel(model);
    return wrapper != null && !myEditor.isManuallyEdited(wrapper);
  }

  private Long getBaseValue(EditModelState model) {
    return model.getValue(myBaseValue);
  }

  private Integer getControllingValue(EditModelState model) {
    return myControlling.getCurrentValue(model);
  }

  public ComponentControl attachField(Lifespan life, final EditItemModel model, ADateField field) {
    ModelWrapper<DateEditor> wrapper = getWrapperModel(model);
    ComponentControl control = myEditor.attachField(life, wrapper, field);
    UpdateTimeListener.attach(life, model, this);
    FieldEditorUtil.registerComponent(wrapper, this, field);
    return control;
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    ADateField field = myEditor.createComponent();
    ComponentControl control = attachField(life, model, field);
    return Collections.singletonList(control);
  }

  private void setValue(EditModelState model, Date date) {
    myEditor.setValue(model, date);
  }

  private static class UpdateTimeListener implements ChangeListener {
    private final StartTimeEditor myEditor;
    private final EditItemModel myModel;
    private boolean myUpdating = false;

    public UpdateTimeListener(EditItemModel model, StartTimeEditor editor) {
      myModel = model;
      myEditor = editor;
    }

    @Override
    public void onChange() {
      if (myUpdating) return;
      myUpdating = true;
      try {
        autoUpdate();
      } finally {
        myUpdating = false;
      }
    }

    private void autoUpdate() {
      if (!myEditor.isAutoUpdate(myModel)) return;
      Long baseTime = myEditor.getBaseValue(myModel);
      if (baseTime == null) return;
      int delta = Util.NN(myEditor.getControllingValue(myModel), 0);
      myEditor.setValue(myModel, new Date(baseTime - delta * 1000));
    }

    public static void attach(Lifespan life, EditItemModel model, StartTimeEditor editor) {
      if (!editor.isAutoUpdate(model)) return;
      final UpdateTimeListener listener = new UpdateTimeListener(model, editor);
      listener.autoUpdate();
      model.addAWTChangeListener(life, listener);
    }
  }
}
