package com.almworks.items.gui.edit.editors.enums.multi;

import com.almworks.api.application.ItemKey;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.items.gui.edit.editors.enums.VariantsAcceptor;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.edit.util.NestedComponent;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.JointChangeListener;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CheckBoxListEditor extends BaseMultiEnumEditor {

  private final CanvasRenderer<ItemKey> myRenderer;

  public CheckBoxListEditor(NameMnemonic labelText, DBAttribute<Set<Long>> attribute, EnumVariantsSource variants,
    @Nullable CanvasRenderer<ItemKey> renderer) {
    super(labelText, attribute, variants);
    myRenderer = Util.NN(renderer, Renderers.<ItemKey>defaultCanvasRenderer());
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    final ACheckboxList<ItemKey> list = ACheckboxList.newCheckBoxList();
    list.setVisibleRowCount(4);
    list.setCanvasRenderer(myRenderer);
    final ListSpeedSearch<ItemKey> ssc = ListSpeedSearch.install(list);
    final JListAdapter adapter = list.getScrollable();
    adapter.setFocusable(true);
    adapter.addFocusListener(SelectionStashingListFocusHandler.speedSearchAware(list.getSelectionAccessor(), ssc));
    return Collections.singletonList(attachComponent(life, model, new JScrollPane(list), list));
  }

  public ComponentControl attachComponent(Lifespan life, final EditItemModel model, JScrollPane scrollPane,
    final ACheckboxList<ItemKey> list)
  {
    if (scrollPane == null || list == null) return null;
    LogHelper.assertError(scrollPane.getViewport().getView() == list, "Wrong tree", this);
    getVariants().configure(life, model, new VariantsAcceptor<ItemKey>() {
      @Override
      public void accept(AListModel<? extends ItemKey> variants, @Nullable Configuration recentConfig) {
        list.setVisibleRowCount(Util.bounded(4, variants.getSize(), 7));
        list.setCollectionModel(variants, true);
      }
    });
    final SelectionAccessor<ItemKey> accessor = list.getCheckedAccessor();
    accessor.setSelected(getValue(model));
    JointChangeListener userListener = new JointChangeListener() {
      @Override
      protected void processChange() {
        setValue(model, accessor.getSelectedItems());
      }
    };
    accessor.addAWTChangeListener(life, userListener);
    model.addAWTChangeListener(life, new JointChangeListener(userListener.getUpdateFlag()) {
      @Override
      protected void processChange() {
        List<ItemKey> value = getValue(model);
        accessor.setSelected(value);
      }
    });
    FieldEditorUtil.registerComponent(model, this, list);
    return new NestedComponent(scrollPane, list, ComponentControl.Dimensions.TALL, this, model,
      getComponentEnabledState(model));
  }
}
