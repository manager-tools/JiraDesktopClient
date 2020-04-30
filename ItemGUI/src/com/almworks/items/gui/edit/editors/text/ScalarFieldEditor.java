package com.almworks.items.gui.edit.editors.text;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.util.BaseScalarFieldEditor;
import com.almworks.items.gui.edit.util.CommonValueKey;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class ScalarFieldEditor<T> extends BaseScalarFieldEditor<T> {
  private final ScalarValueKey<T> myKey;
  private final CommonValueKey<T> myCommonValue;
  private final EditorKind myKind;

  private ScalarFieldEditor(NameMnemonic labelText, DBAttribute<T> attribute, EditorKind kind, ScalarValueKey<T> key) {
    super(labelText, attribute);
    myCommonValue = new CommonValueKey<T>(attribute.getName() + "/common");
    myKind = kind;
    myKey = key;
  }

  public static ScalarFieldEditor<String> shortText(NameMnemonic labelText, DBAttribute<String> attribute, boolean allowEmpty) {
    return new ScalarFieldEditor<String>(labelText, attribute, EditorKind.TEXT_FIELD, new ScalarValueKey.Text(attribute.getId(), allowEmpty));
  }

  public static ScalarFieldEditor<String> textPane(NameMnemonic labelText, DBAttribute<String> attribute) {
    return new ScalarFieldEditor<String>(labelText, attribute, EditorKind.TEXT_PANE, new ScalarValueKey.Text(attribute.getId(), true));
  }

  public static ScalarFieldEditor<String> textPane(NameMnemonic labelText, DBAttribute<String> attribute, int prefHeight) {
    return new ScalarFieldEditor<String>(labelText, attribute, new EditorKind.TextPane(prefHeight), new ScalarValueKey.Text(attribute.getId(), true));
  }

  public static FieldEditor decimal(NameMnemonic labelText, DBAttribute<BigDecimal> attribute) {
    return new ScalarFieldEditor<BigDecimal>(labelText, attribute, EditorKind.TEXT_FIELD, new ScalarValueKey.Decimal(attribute.getId()));
  }

  public static ScalarFieldEditor<Integer> timeInterval(NameMnemonic labelText, DBAttribute<Integer> seconds, @Nullable String defaultUnitSuffix,  boolean mandatory) {
    ScalarValueKey<Integer> key = new TimeIntervalValue(seconds.getId(), defaultUnitSuffix, mandatory);
    return new ScalarFieldEditor<Integer>(labelText, seconds, EditorKind.TEXT_FIELD, key);
  }

  @Override
  public String convertToText(T value) {
    return myKey.toText(value);
  }

  @Override
  public void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
    model.registerEditor(this);
    T value = myCommonValue.loadValue(source, model, getAttribute(), model.getEditingItems());
    if (value != null) myKey.setValue(model, value);
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    ComponentControl component = myKind.createComponent(life, this, model, myCommonValue.getComponentEnabledState(model, false));
    FieldEditorUtil.registerComponent(model, this, component.getComponent());
    return Collections.singletonList(attachModel(life, model, component));
  }

  public ComponentControl attachComponent(Lifespan life, EditItemModel model, JComponent c) {
    FieldEditorUtil.registerComponent(model, this, c);
    return attachModel(life, model, myKind.setupComponent(life, this, model, c, myCommonValue.getComponentEnabledState(model, false)));
  }

  private ComponentControl attachModel(Lifespan life, final EditItemModel model, ComponentControl control) {
    final JTextComponent textComponent = myKind.getTextComponent(control);
    if (textComponent == null) return null;
    textComponent.setText(myKey.getText(model));
    final boolean[] duringUpdate = myKey.listenTextComponent(life, model, textComponent);
    model.addAWTChangeListener(life, new ChangeListener() {
      private T myPrevValue = myKey.getValue(model);

      @Override
      public void onChange() {
        if (duringUpdate[0]) return;
        T value = myKey.getValue(model);
        if (Util.equals(value, myPrevValue)) return;
        myPrevValue = value;
        duringUpdate[0] = true;
        try {
          textComponent.setText(myKey.getText(model));
        } finally {
          duringUpdate[0] = false;
        }
      }
    });
    return control;
  }

  @Override
  public boolean isChanged(EditItemModel model) {
    return myKey.isChanged(model);
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
    myKey.verifyData(verifyContext, this);
  }

  @Override
  public void commit(CommitContext context) {
    myKey.commitValue(context, getAttribute());
  }

  public T getCurrentValue(EditModelState model) {
    return myKey.getValue(model);
  }

  @NotNull
  public String getCurrentTextValue(EditModelState model) {
    return myKey.getText(model);
  }

  @Nullable
  public T getInitialValue(EditModelState model) {
    return myKey.getInitialValue(model);
  }

  public void setValue(EditModelState model, T value) {
    myKey.setValue(model, value);
  }

  public void setTextValue(EditModelState model, String text) {
    myKey.setText(model, text);
  }

  @Override
  public boolean hasValue(EditModelState model) {
    return myKey.hasValue(model);
  }

  public void syncValue(Lifespan life, final EditItemModel model, final Configuration config, final String setting, String defaultText) {
    final String text = config.getSetting(setting, defaultText);
    setTextValue(model, text);
    model.addAWTChangeListener(life, new ChangeListener() {
      private String myLastText = text;

      @Override
      public void onChange() {
        String current = getCurrentTextValue(model);
        if (Util.equals(current, myLastText)) return;
        if (!model.isEnabled(ScalarFieldEditor.this) || hasErrors(model)) return;
        myLastText = current;
        config.setSetting(setting, current);
      }
    });
  }
}
