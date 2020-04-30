package com.almworks.items.gui.edit.editors.enums.multi;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemKeyStub;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.items.gui.edit.editors.enums.VariantsAcceptor;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.edit.util.SimpleComponentControl;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.JointChangeListener;
import com.almworks.util.commons.Condition;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.CompactSubsetEditor;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.GlobalColors;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CompactEnumSubsetEditor extends BaseMultiEnumEditor {
  private static final ItemKeyStub PROTO_ITEM = new ItemKeyStub("someItem123");
  private final CanvasRenderer<ItemKey> myRenderer;
  private final Condition<ItemKey> myFilter;

  public CompactEnumSubsetEditor(NameMnemonic label, DBAttribute<Set<Long>> attribute, EnumVariantsSource variants,
    @Nullable CanvasRenderer<ItemKey> renderer) {
    this(label, attribute, variants, renderer, null);
  }

  public CompactEnumSubsetEditor(NameMnemonic label, DBAttribute<Set<Long>> attribute, EnumVariantsSource variants,
                                 @Nullable CanvasRenderer<ItemKey> renderer, Condition<ItemKey> filter) {
    super(label, attribute, variants);
    myRenderer = Util.NN(renderer, ItemKey.ICON_NAME_RENDERER);
    myFilter = filter;
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    CompactSubsetEditor<ItemKey> editor = createComponent();
    return Collections.singletonList(attachComponent(life, model, editor));
  }

  public static CompactSubsetEditor<ItemKey> createComponent() {
    CompactSubsetEditor<ItemKey> editor = new CompactSubsetEditor<ItemKey>();
    editor.setVisibleRowCount(4);
    editor.setAdaptiveVerticalScroll(true);
    editor.setPrototypeValue(PROTO_ITEM);
    return editor;
  }

  public ComponentControl attachComponent(Lifespan life, final EditItemModel model, final CompactSubsetEditor<ItemKey> component) {
    component.setCanvasRenderer(myRenderer);
    return attachComponent(life, this, getVariants(), model, component, getComponentEnabledState(model), myFilter);
  }

  public static ComponentControl attachComponent(Lifespan life, final MultiEnumEditor editor, EnumVariantsSource variants, final EditItemModel model,
    final CompactSubsetEditor<ItemKey> component, ComponentControl.Enabled enabledState, @Nullable Condition<ItemKey> filter) {
    component.setUnknownSelectionItemColor(GlobalColors.ERROR_COLOR);
    component.setIdentityConvertor(ItemKey.GET_ID);
    component.setAddFilter(filter);
    variants.configure(life, model, new VariantsAcceptor<ItemKey>() {
      @Override
      public void accept(AListModel<? extends ItemKey> variants, @Nullable Configuration recentConfig) {
        component.setFullModel(variants, recentConfig);
      }
    });
    final Collection<? extends ItemKey> selected = editor.getValue(model);
    component.setSelected(selected);
    component.setVisibleRowCount(Util.bounded(4, selected.size(), 7));
    JointChangeListener userListener = new JointChangeListener() {
      @Override
      protected void processChange() {
        editor.setValue(model, component.getSelectedItems());
      }
    };
    component.getSubsetModel().addAWTChangeListener(life, userListener);
    model.addAWTChangeListener(life, new JointChangeListener(userListener.getUpdateFlag()) {
      @Override
      protected void processChange() {
        List<ItemKey> value = editor.getValue(model);
        component.setSelected(value);
      }
    });
    FieldEditorUtil.registerComponent(model, editor, component);
    return SimpleComponentControl.create(component, ComponentControl.Dimensions.TALL, editor, model, enabledState);
  }
}
