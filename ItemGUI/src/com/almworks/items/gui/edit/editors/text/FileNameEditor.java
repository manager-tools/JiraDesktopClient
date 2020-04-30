package com.almworks.items.gui.edit.editors.text;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.composition.DelegatingFieldEditor;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.sync.VersionSource;
import com.almworks.util.Pair;
import com.almworks.util.files.FileUtil;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.Util;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class FileNameEditor extends DelegatingFieldEditor<ScalarFieldEditor<String>> {
  private final ScalarFieldEditor<String> myTextEditor;
  private final String myEmptyName;

  public FileNameEditor(NameMnemonic labelText, DBAttribute<String> attribute, String emptyName) {
    myEmptyName = Util.NN(emptyName);
    myTextEditor = ScalarFieldEditor.shortText(labelText, attribute, false);
  }

  @Override
  protected ScalarFieldEditor<String> getDelegate(VersionSource source, EditModelState model) {
    return myTextEditor;
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
    ModelWrapper<ScalarFieldEditor<String>> wrapper = getWrapperModel(verifyContext.getModel());
    String name = wrapper.getEditor().getCurrentTextValue(wrapper).trim();
    if (name.isEmpty()) {
      verifyContext.addError(this, "Cannot be empty");
      return;
    }
    String correctName = FileUtil.excludeForbddenChars(name);
    if (!correctName.equals(name)) {
      verifyContext.addError(this, "Cannot contain any of the following symbols: " + FileUtil.FORBIDDEN_CHARS_DISPLAYABLE);
    }
  }

  @Override
  public boolean isChanged(EditItemModel model) {
    String initialName = getInitialName(model);
    String name = getNameToCommit(model);
    return !name.equals(initialName);
  }

  @SuppressWarnings("SimplifiableIfStatement")
  @Override
  public boolean hasDataToCommit(EditItemModel model) {
    String name = getNameToCommit(model);
    if (model.isNewItem() && (!name.isEmpty() || !myEmptyName.isEmpty())) return true;
    if (name.isEmpty()) return false;
    return !Util.equals(getInitialName(model), name);
  }

  @Override
  public void commit(CommitContext context) throws CancelCommitException {
    String name = getNameToCommit(context.getModel());
    if (name.isEmpty()) name = myEmptyName;
    context.getCreator().setValue(myTextEditor.getAttribute(), name);
  }

  @NotNull
  private String getInitialName(EditItemModel model) {
    ModelWrapper<ScalarFieldEditor<String>> wrapper = getWrapperModel(model);
    ScalarFieldEditor<String> editor = wrapper.getEditor();
    return Util.NN(editor.getInitialValue(model));
  }

  public String getNameToCommit(EditItemModel ownModel) {
    ModelWrapper<ScalarFieldEditor<String>> wrapper = getWrapperModel(ownModel);
    ScalarFieldEditor<String> editor = wrapper.getEditor();
    String name = editor.getCurrentTextValue(ownModel).trim();
    Pair<String,String> nameAndExtension = FileUtil.getNameAndExtension(name, null, null);
    if (nameAndExtension.getSecond() == null) {
      String initialExt = FileUtil.getNameAndExtension(getInitialName(ownModel), null, null).getSecond();
      if (initialExt != null) name = nameAndExtension.getFirst() + "." + initialExt;
    }
    name = FileUtil.excludeForbddenChars(name);
    return name;
  }

  public void setValue(EditItemModel ownModel, String value) {
    ModelWrapper<ScalarFieldEditor<String>> wrapper = getWrapperModel(ownModel);
    wrapper.getEditor().setValue(wrapper, value);
  }

  public void attachComponent(DetachComposite life, EditItemModel ownModel, JTextField field) {
    ModelWrapper<ScalarFieldEditor<String>> wrapper = getWrapperModel(ownModel);
    wrapper.getEditor().attachComponent(life, wrapper, field);
    FieldEditorUtil.registerComponent(ownModel, this, field);
  }
}
