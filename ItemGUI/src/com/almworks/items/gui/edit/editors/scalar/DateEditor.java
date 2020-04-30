package com.almworks.items.gui.edit.editors.scalar;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.util.BaseScalarFieldEditor;
import com.almworks.items.gui.edit.util.CommonValueKey;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.edit.util.SimpleComponentControl;
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

public class DateEditor<D> extends BaseScalarFieldEditor<D> {
  @NotNull 
  private final DateEditorKind<D> myDateKind;
  private final TypedKey<D> myKey;
  private final TypedKey<Boolean> myManuallyEdited;
  private final TypedKey<Boolean> myEditModelUpdate;
  private final CommonValueKey<D> myCommonValue;

  public DateEditor(NameMnemonic labelText, DBAttribute<D> attribute, @NotNull DateEditorKind<D> dateKind) {
    super(labelText, attribute);
    myDateKind = dateKind;
    myKey = TypedKey.create(attribute.getId() + "/val");
    myCommonValue = new CommonValueKey<D>(attribute.getId() + "/common");
    myManuallyEdited = TypedKey.create(attribute.getId() + "/manualEdit");
    myEditModelUpdate = TypedKey.create(attribute.getId() + "/editModelUpdate");
  }

  public static DateEditor<Date> createDateTime(NameMnemonic labelText, DBAttribute<Date> attribute) {
    return new DateEditor<Date>(labelText, attribute, DateEditorKind.DATE_TIME);
  }

  public static DateEditor<Integer> createDate(NameMnemonic labelText, DBAttribute<Integer> attribute) {
    return new DateEditor<Integer>(labelText, attribute, DateEditorKind.DAY);
  }

  @Override
  public String convertToText(D value) {
    return value != null ? myDateKind.format(value) : null;
  }

  @Override
  public void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
    model.registerEditor(this);
    setValue(model, myCommonValue.loadValue(source, model, getAttribute(), model.getEditingItems()));
  }

  @NotNull
  @Override
  public final List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    ADateField field = createComponent();
    return Collections.singletonList(attachField(life, model, field));
  }

  public ADateField createComponent() {
    return new ADateField(myDateKind.getEditorPrecision());
  }

  private boolean isDuringUpdate(EditModelState model) {
    return Boolean.TRUE.equals(model.getValue(myEditModelUpdate));
  }

  public ComponentControl attachField(Lifespan life, final EditItemModel model, final ADateField component) {
    final D dateRaw = model.getValue(myKey);
    component.setDate(myDateKind.toDate(dateRaw));
    component.getDateModel().addAWTChangeListener(life, new ChangeListener() {
      @Override
      public void onChange() {
        if (isDuringUpdate(model)) return;
        model.putHint(myEditModelUpdate, true);
        try {
          Date date = component.getDateModel().getValue();
          model.putHint(myManuallyEdited, true);
          setValue(model, myDateKind.createModelValue(date));
        } finally {
          model.putHint(myEditModelUpdate, null);
        }
      }
    });
    model.addAWTChangeListener(life, new ChangeListener() {
      private D myPrevRaw = dateRaw;
      @Override
      public void onChange() {
        if (isDuringUpdate(model)) return;
        D newDateRaw = model.getValue(myKey);
        if (Util.equals(myPrevRaw, newDateRaw)) return;
        myPrevRaw = newDateRaw;
        model.putHint(myEditModelUpdate, true);
        try {
          component.getDateModel().setValue(myDateKind.toDate(newDateRaw));
        } finally {
          model.putHint(myEditModelUpdate, null);
        }
      }
    });
    FieldEditorUtil.registerComponent(model, this, component);
    return SimpleComponentControl.singleLine(component, this, model, myCommonValue.getComponentEnabledState(model, false));
  }

  public boolean isManuallyEdited(EditModelState model) {
    return Boolean.TRUE.equals(model.getValue(myManuallyEdited));
  }

  @Override
  public boolean isChanged(EditItemModel model) {
    return !model.isEqualValue(myKey);
  }

  @Override
  public void commit(CommitContext context) {
    D value = context.getModel().getValue(myKey);
    value = myDateKind.processForCommit(value);
    context.getCreator().setValue(getAttribute(), value);
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
  }

  @Override
  public boolean hasValue(EditModelState model) {
    return model.getValue(myKey) != null;
  }
  
  public void setValue(EditModelState model, D dateRaw) {
    model.putValue(myKey, dateRaw);
  }
}
