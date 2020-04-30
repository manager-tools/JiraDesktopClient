package com.almworks.jira.provider3.links.actions;

import com.almworks.explorer.PrimaryItemKeyTransferHandler;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.MockEditor;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.edit.util.ModelAttachListener;
import com.almworks.items.gui.edit.util.NestedComponent;
import com.almworks.items.gui.edit.util.SimpleComponentControl;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.text.TextUtil;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.util.Collections;
import java.util.List;

/**
 * An editor to enter list of issue keys into text component<br>
 * This is NOT a generic editor. It does not support any modification of any issue. It just implement {@link FieldEditor}
 * to provide UI facility to edit issue keys list.
 */
class KeyListEditor extends MockEditor {
  private final TypedKey<String> myTextKey = TypedKey.create("keysText");
  private final boolean mySingleKey;

  public KeyListEditor(NameMnemonic labelText, boolean singleKey) {
    super(labelText);
    mySingleKey = singleKey;
  }

  @Override
  public void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
    model.registerEditor(this);
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    JTextArea area = new JTextArea();
    attachTextComponent(life, model, area);
    JScrollPane scrollPane = new JScrollPane(area);
    return Collections.singletonList(
      new NestedComponent(scrollPane, area, ComponentControl.Dimensions.TALL, this, model,
        ComponentControl.Enabled.ALWAYS_ENABLED));
  }

  public ComponentControl attachTextComponent(Lifespan life, final EditItemModel model, final JTextComponent component,
    @Nullable ComponentControl.Dimensions dimension) {
    if (dimension == null) {
      if (component instanceof JTextField) dimension = ComponentControl.Dimensions.SINGLE_LINE;
      else if (component instanceof JTextArea) dimension = ComponentControl.Dimensions.TALL;
      else dimension = ComponentControl.Dimensions.SINGLE_LINE;
    }
    attachTextComponent(life, model, component);
    return SimpleComponentControl.create(component, dimension, this, model, ComponentControl.Enabled.NOT_APPLICABLE);
  }

  private void attachTextComponent(Lifespan life, final EditItemModel model, final JTextComponent component) {
    component.setTransferHandler(PrimaryItemKeyTransferHandler.getInstance(mySingleKey));
    new ModelAttachListener(model) {
      @Override
      protected void onModelChanged(EditItemModel model) {
        component.setText(Util.NN(model.getValue(myTextKey)).trim());
      }

      @Override
      protected void onComponentChanged(EditItemModel model) {
        model.putValue(myTextKey, component.getText().trim());
      }
    }.attachModel(life, model).listenTextComponent(life, component);
    FieldEditorUtil.registerComponent(model, this, component);
  }

  @Override
  public boolean hasValue(EditModelState model) {
    return !getIssueKeys(model).isEmpty();
  }

  @Override
  public boolean isChanged(EditItemModel model) {
    List<String> currentKeys = getIssueKeys(model);
    List<String> initialKeys = Issue.extractIssueKeys(model.getInitialValue(myTextKey));
    //noinspection SimplifiableIfStatement
    if (currentKeys.size() != initialKeys.size()) return true;
    return !Collections15.hashSet(currentKeys).equals(Collections15.hashSet(initialKeys));
  }

  @Override
  public boolean hasDataToCommit(EditItemModel model) {
    return hasValue(model);
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
    EditItemModel model = verifyContext.getModel();
    String text = model.getValue(myTextKey);
    String badKeys = Issue.extractBadKeys(text);
    if (mySingleKey) {
      List<String> keys = getIssueKeys(model);
      if (keys.size() > 1) {
        verifyContext.addError(this, "Expected single issue key");
        return;
      }
    }
    if (badKeys == null) return;
    if (badKeys.isEmpty()) verifyContext.addError(this, "Missing target issue key");
    else verifyContext.addError(this, "Illegal issue keys (" + badKeys + ")");
  }

  @NotNull
  public List<String> getIssueKeys(EditModelState model) {
    return Issue.extractIssueKeys(model.getValue(myTextKey));
  }
  
  public void setIssueKeys(EditItemModel model, List<String> keys) {
    model.putValue(myTextKey, TextUtil.separateToString(keys, " "));
  }

  @NotNull
  public LongList getIssueItems(DBReader reader, long connection, EditModelState model) {
    List<String> keys = getIssueKeys(model);
    return AddLinksEditor.resolveIssues(reader, connection, keys);
  }
}
