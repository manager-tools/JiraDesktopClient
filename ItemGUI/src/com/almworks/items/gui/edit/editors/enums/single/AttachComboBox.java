package com.almworks.items.gui.edit.editors.enums.single;

import com.almworks.api.application.ItemKey;
import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.editors.enums.DefaultItemSelector;
import com.almworks.items.gui.edit.editors.enums.VariantsAcceptor;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.edit.util.SimpleComponentControl;
import com.almworks.util.advmodel.AComboboxModel;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.Equality;
import com.almworks.util.collections.JointChangeListener;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Factory1;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.completion.CompletingComboBox;
import com.almworks.util.components.completion.CompletingComboBoxController;
import com.almworks.util.components.recent.RecentController;
import com.almworks.util.components.recent.UnwrapCombo;
import com.almworks.util.config.Configuration;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class AttachComboBox {

  private static final Factory1<Condition<ItemKey>, String> DEFAULT_USER_FILTER =
    new Factory1<Condition<ItemKey>, String>() {
      public Condition<ItemKey> create(String argument) {
        final String typed = Util.lower(argument);
        return new Condition<ItemKey>() {
          public boolean isAccepted(ItemKey value) {
            return value != null
              && (Util.lower(value.getId()).indexOf(typed) != -1
              || Util.lower(value.getDisplayName()).indexOf(typed) != -1);
          }
        };
      }
    };

  static ComponentControl attach(final SingleEnumFieldEditor editor, final Lifespan life, final EditItemModel model, CompletingComboBox<ItemKey> combo,
    Convertor<ItemKey, String> toString, Convertor<String, ItemKey> fromString, CanvasRenderer<? super ItemKey> renderer, ComponentControl.Enabled enableState, DefaultItemSelector defaultItem) {
    final CompletingComboBoxController<ItemKey> controller = combo.getController();
    controller.setFilterFactory(DEFAULT_USER_FILTER);
    controller.setConvertors(toString, fromString, Equality.GENERAL);
    controller.setCanvasRenderer(renderer);
    editor.getVariants().configure(life, model, new VariantsAcceptor<ItemKey>() {
      @Override
      public void accept(AListModel<? extends ItemKey> variants, @Nullable Configuration recentConfig) {
        controller.setVariantsModel(recentConfig, variants);
      }
    });
    controller.setIdentityConvertor(BaseSingleEnumEditor.RECENT_GET_ID);
    PopupMenuListener popupListener = connect(editor, life, model, combo.getAModel(), combo.getRecents(), defaultItem);
    combo.addPopupMenuListener(popupListener);
    FieldEditorUtil.registerComponent(model, editor, combo);
    return SimpleComponentControl.singleLine(combo, editor, model, enableState);
  }

  /**
   * @return popup listener. Caller MUST add this listener to it's combobox - otherwise changes does not reach the model.
   */
  static PopupMenuListener connect(final SingleEnumFieldEditor editor, Lifespan life, final EditItemModel model, final AComboboxModel<?> cbModel, RecentController<ItemKey> recents,
    DefaultItemSelector defaultItem) {
    recents.setWrapRecents(true);
    ItemKey initialItem = editor.getValue(model);
    if (initialItem == null) initialItem = defaultItem.selectDefaultItem(model, recents.getSourceModel());
    if (initialItem != null) {
      recents.setInitial(initialItem);
      UnwrapCombo.selectRecent(cbModel, initialItem);
    }
    editor.setValue(model, initialItem);
    JointChangeListener userListener = new JointChangeListener() {
      @Override
      protected void processChange() {
        ItemKey enumKey = RecentController.unwrap(cbModel.getSelectedItem());
        editor.setValue(model, enumKey);
      }
    };
    ComboListener comboListener = new ComboListener(userListener);
    cbModel.addSelectionChangeListener(life, comboListener);
    model.addAWTChangeListener(life, new JointChangeListener(userListener.getUpdateFlag()) {
      @Override
      protected void processChange() {
        ItemKey modelValue = editor.getValue(model);
        UnwrapCombo.selectRecent(cbModel, modelValue);
      }
    });
    return comboListener;
  }

  private static class ComboListener implements PopupMenuListener, ChangeListener {
    private boolean myPopupVisible = false;
    private boolean myFireWhenHidden = false;
    private final ChangeListener myChained;

    private ComboListener(ChangeListener chained) {
      myChained = chained;
    }

    @Override
    public void onChange() {
      if (!myPopupVisible) myChained.onChange();
      else myFireWhenHidden = true;
    }

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
      myPopupVisible = true;
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
      myPopupVisible = false;
      if (myFireWhenHidden) myChained.onChange();
    }

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {
      popupMenuWillBecomeInvisible(e);
    }
  }
}
