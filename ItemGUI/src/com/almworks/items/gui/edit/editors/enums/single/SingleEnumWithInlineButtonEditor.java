package com.almworks.items.gui.edit.editors.enums.single;

import com.almworks.api.application.ItemKey;
import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.editors.composition.SingleEnumDelegatingEditor;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.util.CompositeComponentControl;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.edit.util.SimpleComponentControl;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.util.Pair;
import com.almworks.util.components.AToolbarButton;
import com.almworks.util.components.FieldWithMoreButton;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

/**
 * Designed to wrap single line single enum editors and add additional button to set specific value (such as add "set me" to assignee editor). Lets {@link SingleEnumDefaultValue} to prepare
 * for edit - check that special value makes sense. Then it adds the button if the special value can be set.
 */
public class SingleEnumWithInlineButtonEditor<E extends SingleEnumFieldEditor> extends SingleEnumDelegatingEditor<E> {
  private final E myEditor;
  private final SingleEnumDefaultValue myValue;
  private DelegateToLocalAction myActionDelegate;
  private Icon myIcon;
  private String myActionName;

  public SingleEnumWithInlineButtonEditor(E editor, SingleEnumDefaultValue value) {
    super(editor.getAttribute(), editor.getVariants());
    myEditor = editor;
    myValue = value;
  }

  public void setActionDelegate(DelegateToLocalAction actionDelegate) {
    myActionDelegate = actionDelegate;
  }

  public static <E extends SingleEnumFieldEditor> SingleEnumWithInlineButtonEditor<E> create(E editor, SingleEnumDefaultValue value) {
    return new SingleEnumWithInlineButtonEditor<E>(editor, value);
  }

  public static Pair<? extends ItemKey, Long> getItemValue(EditItemModel model, TypedKey<Long> itemKey, DBStaticObject enumType) {
    Long thisUser = model.getValue(itemKey);
    if (thisUser == null) return null;
    GuiFeaturesManager manager = EngineConsts.getGuiFeaturesManager(model);
    if (manager == null) return null;
    EnumTypesCollector.Loaded users = manager.getEnumTypes().getType(enumType);
    if (users == null) return null;
    LoadedItemKey thisUserKey = users.getResolvedItem(thisUser);
    return thisUserKey != null ? Pair.create(thisUserKey, thisUser) : null;
  }

  @Override
  protected E getDelegate(VersionSource source, EditModelState model) {
    return myEditor;
  }

  @Override
  protected void prepareWrapper(VersionSource source, ModelWrapper<E> wrapper, EditPrepare editPrepare) {
    super.prepareWrapper(source, wrapper, editPrepare);
    EngineConsts.ensureGuiFeatureManager(source, wrapper);
    myValue.prepare(source, wrapper.getOriginalModel());
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    List<? extends ComponentControl> components = super.createComponents(life, model);
    if (!myValue.isEnabled(model)) return components;
    ComponentControl control = null;
    int controlIndex = -1;
    for (int i = 0, componentsSize = components.size(); i < componentsSize; i++) {
      ComponentControl c = components.get(i);
      if (c.getDimension().isFixedHeight()) {
        control = c;
        controlIndex = i;
        break;
      }
    }
    if (control == null) return components;
    FieldWithMoreButton<JComponent> panel = new FieldWithMoreButton<>();
    ComponentControl.Enabled enabled = control.getEnabled();
    boolean enable = true;
    if (enabled.isDisabled()) enable = false;
    else if (!enabled.isEnabled()) enable = control.getComponent().isEnabled();
    attachField(model, panel, control.getComponent());
    panel.setEnabled(enable);
    List<ComponentControl> replaced = Collections15.arrayList(components);
    CompositeComponentControl replacement = CompositeComponentControl.create(control.getDimension(), this, model, control.getLabel(),
      enabled, panel, control,
      SimpleComponentControl.singleLine(panel, this, model, enabled));
    replaced.set(controlIndex, replacement);
    FieldEditorUtil.registerComponent(model, this, replacement.getComponent());
    return replaced;
  }

  public <C extends JComponent> void attachField(EditItemModel model, FieldWithMoreButton<C> field, C component) {
    field.setField(component);
    SetValueAction action = new SetValueAction(model);
    if (myActionDelegate != null) {
      myActionDelegate.provideAction(field, action);
      field.setAction(myActionDelegate.getProxy());
    } else {
      field.setAction(action);
      field.setIcon(myIcon);
      field.setActionName(myActionName);
    }
    field.setButton(new AToolbarButton());
  }

  public void setIcon(Icon icon) {
    myIcon = icon;
  }

  public void setActionName(String actionName) {
    myActionName = actionName;
  }


  private class SetValueAction extends SimpleAction {
    private final EditItemModel myModel;

    public SetValueAction(EditItemModel model) {
      myModel = model;
    }

    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      if (myIcon != null) {
        context.putPresentationProperty(PresentationKey.SMALL_ICON, myIcon);
        context.putPresentationProperty(PresentationKey.NAME, "");
      } else context.putPresentationProperty(PresentationKey.NAME, "\u2026");
      context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION, myActionName);
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      Pair<? extends ItemKey, Long> value = myValue.getValue(myModel);
      ModelWrapper<E> wrapper = getWrapperModel(myModel);
      E editor = wrapper.getEditor();
      long item = value == null ? 0 : Math.max(0, Util.NN(value.getSecond(), 0l));
      if (value != null && value.getFirst() == null && item == 0) value = null;
      if (value == null) {
        editor.setValue(wrapper, null);
        return;
      }
      ItemKey key = value.getFirst();
      if (key != null) editor.setValue(wrapper, key);
      else editor.setValueItem(wrapper, item);
    }
  }
}
