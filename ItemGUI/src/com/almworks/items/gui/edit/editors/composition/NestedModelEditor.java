package com.almworks.items.gui.edit.editors.composition;

import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.util.Pair;
import com.almworks.util.text.NameMnemonic;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Base class for editor that create nested model for complex editing<br><br>
 * Use cases:<br>
 * 1. Create/Edit separate item<br>
 * Such editors creates or edit item for each edited item of outer model.<br><br>
 * 2. Better control of inner editors<br>
 * Such editors requires to control commit process (perform pre commit checks or perform additional post commit actions or
 * control commit of particular editors) or special implementations of FieldEditor methods.<br>
 * The editors separate part of edit state to sub model. They should provide inner
 * attribute value providers to outer model by creating attribute providing child model.
 */
public abstract class NestedModelEditor implements FieldEditor {
  private final TypedKey<List<? extends FieldEditor>> myEditorsKey;
  private final TypedKey<DefaultEditModel.Child> mySlaveKey;
  private final NameMnemonic myLabelText;

  protected NestedModelEditor(@NotNull NameMnemonic labelText) {
    myLabelText = labelText;
    mySlaveKey = TypedKey.create(labelText + "/model");
    myEditorsKey = TypedKey.create(labelText + "/editors");
  }

  @Override
  @NotNull
  public NameMnemonic getLabelText(EditModelState model) {
    return myLabelText;
  }

  @Override
  public final void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
    Pair<DefaultEditModel.Child, ? extends List<? extends FieldEditor>> pair = createNestedModel(source, model, editPrepare);
    if (pair == null) return;
    DefaultEditModel.Child nestedModel = pair.getFirst();
    List<? extends FieldEditor> editors = pair.getSecond();
    if (nestedModel == null || editors == null) return;
    EngineConsts.setupNestedModel(model, nestedModel);
    model.putHint(myEditorsKey, editors);
    model.putHint(mySlaveKey, nestedModel);
    for (FieldEditor editor : editors) editor.prepareModel(source, nestedModel, editPrepare);
    model.registerEditor(this);
    afterChildPrepared(source, nestedModel, editPrepare);
  }

  /**
   * Called when preparation of all child model editors is done. This method can be used to perform additional preparation
   * that requires all editors prepared.<br>
   * Additional editors can be registered to the model but they are not returned by {@link #getNestedEditors(com.almworks.items.gui.edit.EditModelState)}
   * and not be used to create UI components. This additional editors work the same way as editors registered not by this
   * {@link NestedModelEditor editor} itself.
   *
   * @param source
   * @param child child model
   */
  protected void afterChildPrepared(VersionSource source, EditItemModel child, EditPrepare editPrepare) {}

  /**
   * Creates nested model and editors set. This method is called during {@link FieldEditor#prepareModel(com.almworks.items.sync.VersionSource, com.almworks.items.gui.edit.EditItemModel, com.almworks.items.sync.EditPrepare) edit prepare}.
   * The implementation may perform additional preparations within this method (instead of overriding {@link com.almworks.items.gui.edit.FieldEditor#prepareModel(com.almworks.items.sync.VersionSource, com.almworks.items.gui.edit.EditItemModel, com.almworks.items.sync.EditPrepare)})
   * @return nested model and editors for it. null result means this editor should not participate in the edit - it is not
   * registered and not affects the edit.
   */
  @Nullable
  protected abstract Pair<DefaultEditModel.Child, ? extends List<? extends FieldEditor>> createNestedModel(VersionSource source, EditItemModel parent, EditPrepare editPrepare);

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    DefaultEditModel nested = getNestedModel(model);
    if (nested == null) return Collections.emptyList();
    List<ComponentControl> components = Collections15.arrayList();
    List<? extends FieldEditor> editors = getNestedEditors(model);
    if (editors == null) return Collections.emptyList();
    for (FieldEditor editor : editors) {
      List<? extends ComponentControl> slaveComponents = createEditorComponent(life, nested, editor);
      components.addAll(slaveComponents);
    }
    for (ComponentControl control : components) FieldEditorUtil.registerComponent(model, this, control.getComponent());
    return components;
  }

  protected List<? extends ComponentControl> createEditorComponent(Lifespan life, DefaultEditModel nested,
    FieldEditor editor)
  {
    return editor.createComponents(life, nested);
  }

  protected List<? extends FieldEditor> getNestedEditors(EditModelState model) {
    return model.getValue(myEditorsKey);
  }

  @Override
  public void afterModelFixed(EditItemModel model) {
    DefaultEditModel slaveModel = getNestedModel(model);
    if (slaveModel != null) slaveModel.saveInitialValues();
  }

  public DefaultEditModel.Child getNestedModel(EditModelState model) {
    return model != null ? model.getValue(mySlaveKey) : null;
  }

  @Override
  public void onItemsChanged(EditItemModel model, TLongObjectHashMap<ItemValues> newValues) {
    DefaultEditModel.Child nested = getNestedModel(model);
    if (nested.isNewItem()) return;
    EditItemModel.notifyItemsChanged(nested, newValues);
  }

  @Override
  public void afterModelCopied(EditItemModel copy) {
    DefaultEditModel.Child nested = getNestedModel(copy);
    if (nested == null) return;
    DefaultEditModel.Child nestedCopy = nested.copyState(copy);
    nestedCopy.addAWTChangeListener(Lifespan.FOREVER, copy);
    copy.putValue(mySlaveKey, nestedCopy);
  }

  @Override
  public boolean isChanged(EditItemModel model) {
    DefaultEditModel slaveModel = getNestedModel(model);
    return slaveModel != null && slaveModel.isChanged();
  }

  @Override
  public boolean hasDataToCommit(EditItemModel model) {
    EditItemModel slaveModel = getNestedModel(model);
    return slaveModel != null && slaveModel.hasDataToCommit();
  }

  @Override
  public boolean hasValue(EditModelState model) {
    EditItemModel slaveModel = getNestedModel(model);
    for (FieldEditor editor : slaveModel.getAllEditors()) if (editor.hasValue(slaveModel)) return true;
    return false;
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
    EditItemModel slaveModel = getNestedModel(verifyContext.getModel());
    if (slaveModel == null) return;
    slaveModel.verifyData(verifyContext.subContext(slaveModel));
  }

  /**
   * Utility method to commit all editors from nested model
   * @param parent outer commit context
   * @return commit context for nested model
   */
  protected final CommitContext commitNested(CommitContext parent) throws CancelCommitException {
    DefaultEditModel.Child nested = getNestedModel(parent.getModel());
    if (nested == null) return null;
    CommitContext nestedCommit = parent.subContext(nested);
    nestedCommit.commitEditors(getNestedEditors(parent.getModel()));
    return nestedCommit;
  }
}
