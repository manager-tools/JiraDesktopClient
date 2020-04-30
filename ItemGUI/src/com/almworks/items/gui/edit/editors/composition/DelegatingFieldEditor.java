package com.almworks.items.gui.edit.editors.composition;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.text.NameMnemonic;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Base class for editor wrappers - class which adds new features to existing editors.<br>
 * It wraps the model to make it transparent editor registrations - outer model client sees the wrapper editor, but the
 * inner editor sees itself.<br>
 * The original model should not be passed to inner editor except methods which works only with model state {@link EditModelState}.
 * If the original model is passed to inner editor wrong behaviour is possible since the editor surely cannot find itself
 * in registered editors (also it cannot enable or disable itself and perform right similar operation).<br>
 */
public abstract class DelegatingFieldEditor<E extends FieldEditor> implements FieldEditor {
  private final TypedKey<ModelWrapper<E>> WRAPPER = TypedKey.create("wrapper");

  @Nullable
  protected abstract E getDelegate(VersionSource source, EditModelState model);

  @Override
  public final void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
    E delegate = getDelegate(source, model);
    if (delegate == null) return;
    ModelWrapper<E> wrapper = getWrapperModel(model);
    if (wrapper != null) {
      LogHelper.error("Duplicated prepare");
      return;
    }
    wrapper = new ModelWrapper<E>(model, this, delegate);
    model.putHint(WRAPPER, wrapper);
    prepareWrapper(source, wrapper, editPrepare);
  }

  protected void prepareWrapper(VersionSource source, ModelWrapper<E> wrapper, EditPrepare editPrepare) {
    wrapper.getEditor().prepareModel(source, wrapper, editPrepare);
  }

  @Override
  public void afterModelCopied(EditItemModel copy) {
    ModelWrapper<E> wrapper = getWrapperModel(copy);
    if (wrapper == null) return;
    ModelWrapper<E> wrapperCopy = wrapper.createCopy(copy);
    copy.putHint(WRAPPER, wrapperCopy);
    wrapperCopy.getEditor().afterModelCopied(wrapperCopy);
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    ModelWrapper<E> wrapper = getWrapperModel(model);
    if (wrapper == null) return Collections.emptyList();
    List<? extends ComponentControl> components = wrapper.getEditor().createComponents(life, wrapper);
    for (ComponentControl control : components) FieldEditorUtil.registerComponent(model, this, control.getComponent());
    return components;
  }

  @Override
  public void afterModelFixed(EditItemModel model) {
    ModelWrapper<E> wrapper = getWrapperModel(model);
    if (wrapper == null) return;
    wrapper.getEditor().afterModelFixed(wrapper);
  }

  @Override
  public boolean isChanged(EditItemModel model) {
    ModelWrapper<E> wrapper = getWrapperModel(model);
    return wrapper != null && wrapper.getEditor().isChanged(wrapper);
  }

  @Override
  public boolean hasDataToCommit(EditItemModel model) {
    ModelWrapper<E> wrapper = getWrapperModel(model);
    return wrapper != null && wrapper.getEditor().hasDataToCommit(wrapper);
  }

  @Override
  public boolean hasValue(EditModelState model) {
    ModelWrapper<E> wrapper = getWrapperModel(model);
    return wrapper != null && wrapper.getEditor().hasValue(wrapper);
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
    ModelWrapper<E> wrapper = getWrapperModel(verifyContext.getModel());
    if (wrapper == null) return;
    wrapper.getEditor().verifyData(verifyContext.subContext(wrapper));
  }

  @Override
  public void onItemsChanged(EditItemModel model, TLongObjectHashMap<ItemValues> newValues) {
    ModelWrapper<E> wrapper = getWrapperModel(model);
    wrapper.getEditor().onItemsChanged(wrapper, newValues);
  }

  @Override
  public void commit(CommitContext context) throws CancelCommitException {
    ModelWrapper wrapper = getWrapperModel(context.getModel());
    if (wrapper == null) return;
    wrapper.getEditor().commit(context.subContext(wrapper));
  }

  @Override
  public NameMnemonic getLabelText(EditModelState model) {
    ModelWrapper<E> wrapper = getWrapperModel(model);
    if (wrapper == null) return NameMnemonic.EMPTY;
    return wrapper.getEditor().getLabelText(wrapper);
  }

  public ModelWrapper<E> getWrapperModel(EditModelState model) {
    return model.getValue(WRAPPER);
  }

  public static class ModelWrapper<E extends FieldEditor> extends EditItemModel {
    private final EditItemModel myDelegate;
    private final E myModelEditor;
    private final DelegatingFieldEditor mySubstitution;

    private ModelWrapper(EditItemModel delegate, DelegatingFieldEditor substitution, E model) {
      myDelegate = delegate;
      myModelEditor = model;
      mySubstitution = substitution;
    }

    @NotNull
    @Override
    public EditItemModel getRootModel() {
      return myDelegate.getRootModel();
    }

    @Override
    public List<FieldEditor> getAllEditors() {
      return substituteEditors(myDelegate.getAllEditors());
    }

    @Override
    public void addChildModel(EditItemModel child) {
      myDelegate.addChildModel(child);
    }

    @Override
    public List<FieldEditor> getCommitEditors() {
      return substituteEditors(myDelegate.getCommitEditors());
    }

    @Override
    public LongList getEditingItems() {
      return myDelegate.getEditingItems();
    }

    @Override
    public List<FieldEditor> getEnabledEditors() {
      return substituteEditors(myDelegate.getEnabledEditors());
    }

    @Override
    public Long getSingleEnumValue(DBAttribute<Long> attribute) {
      return myDelegate.getSingleEnumValue(attribute);
    }

    @Override
    public Pair<LongList, LongList> getCubeAxis(DBAttribute<?> axis) {
      return myDelegate.getCubeAxis(axis);
    }

    @Override
    public <T> T getInitialValue(TypedKey<T> key) {
      return myDelegate.getInitialValue(key);
    }

    @Override
    public <T> T getValue(TypedKey<T> key) {
      return myDelegate.getValue(key);
    }

    @Override
    public boolean isNewItem() {
      return myDelegate.isNewItem();
    }

    @Override
    public <T> void putHint(TypedKey<T> key, @Nullable T hint) {
      myDelegate.putHint(key, hint);
    }

    @Override
    public <T> void putValue(TypedKey<T> key, T value) {
      myDelegate.putValue(key, value);
    }

    @Override
    public <A, B> void putValues(TypedKey<A> key1, A value1, TypedKey<B> key2, B value2) {
      myDelegate.putValues(key1, value1, key2, value2);
    }

    @Override
    public <A, B, C> void putValues(TypedKey<A> key1, A value1, TypedKey<B> key2, B value2, TypedKey<C> key3, C value3)
    {
      myDelegate.putValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public void registerSingleEnum(DBAttribute<Long> attribute,Convertor<EditModelState, LongList> getter) {
      myDelegate.registerSingleEnum(attribute, getter);
    }

    @Override
    public void registerMultiEnum(DBAttribute<? extends Collection<Long>> attribute, Convertor<EditModelState, LongList> getter) {
      myDelegate.registerMultiEnum(attribute, getter);
    }

    @Override
    public void registerAttributeSource(Lifespan life, AttributeValueSource source) {
      myDelegate.registerAttributeSource(Lifespan.FOREVER, source);
    }

    @Override
    public void registerEditor(FieldEditor editor) {
      FieldEditor subst = editor == myModelEditor ? mySubstitution : editor;
      myDelegate.registerEditor(subst != null ? subst : editor);
    }

    @Override
    public void setEditorEnabled(FieldEditor editor, boolean enable) {
      FieldEditor subst = editor == myModelEditor ? mySubstitution : editor;
      myDelegate.setEditorEnabled(subst != null ? subst : editor, enable);
    }

    @Override
    public boolean isEnabled(FieldEditor editor) {
      FieldEditor subst = editor == myModelEditor ? mySubstitution : editor;
      return myDelegate.isEnabled(subst);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
      myDelegate.removeChangeListener(listener);
    }

    @Override
    public void addAWTChangeListener(Lifespan life, ChangeListener listener) {
      myDelegate.addAWTChangeListener(life, listener);
    }

    @Override
    @Deprecated
    public Detach addAWTChangeListener(ChangeListener listener) {
      return myDelegate.addAWTChangeListener(listener);
    }

    @Override
    public void addChangeListener(Lifespan life, ThreadGate gate, ChangeListener listener) {
      myDelegate.addChangeListener(life, gate, listener);
    }

    @Override
    public void addChangeListener(Lifespan life, ChangeListener listener) {
      myDelegate.addChangeListener(life, listener);
    }

    @Override
    public void onChange() {
      myDelegate.onChange();
    }

    @Override
    public void fireChanged() {
      myDelegate.fireChanged();
    }

    private List<FieldEditor> substituteEditors(List<FieldEditor> result) {
      for (int i = 0; i < result.size(); i++) {
        FieldEditor editor = result.get(i);
        if (editor == mySubstitution) result.set(i, myModelEditor);
      }
      return result;
    }

    public ModelWrapper<E> createCopy(EditItemModel targetModel) {
      return new ModelWrapper<E>(targetModel, mySubstitution, myModelEditor);
    }

    public E getEditor() {
      return myModelEditor;
    }

    public EditItemModel getOriginalModel() {
      return myDelegate;
    }
  }
}
