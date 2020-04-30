package com.almworks.items.gui.edit.editors.enums.single;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemKeyStub;
import com.almworks.api.application.ItemOrder;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.composition.DelegatingFieldEditor;
import com.almworks.items.gui.edit.editors.enums.DefaultItemSelector;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.items.gui.edit.editors.enums.VariantsAcceptor;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.util.BaseFieldEditor;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.util.AttributeMap;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.advmodel.SegmentedListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.recent.RecentController;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.NameMnemonic;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.PopupMenuListener;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class BaseSingleEnumEditor extends BaseFieldEditor implements SingleEnumFieldEditor {
  static final ItemKey NULL_ITEM = new ItemKeyStub("_NULL_ITEM_PLACE_HOLDER_", "", ItemOrder.byOrder(0));
  static final Convertor<ItemKey, String> RECENT_GET_ID = new Convertor<ItemKey, String>() {
    @Override
    public String convert(ItemKey value) {
      if (value == null || NULL_ITEM == value) return null;
      return value.getId();
    }
  };
  private final TypedKey<Long> myExternalValue;
  private final MyDecoratedVariants myDecoratedItems;
  private final EnumValueKey myValueKey;
  private final boolean myVerify;

  public BaseSingleEnumEditor(NameMnemonic labelText, EnumVariantsSource variants, @Nullable DefaultItemSelector defaultItem,
    boolean appendNull, EnumValueKey valueKey, boolean verify)
  {
    super(labelText);
    myValueKey = valueKey;
    myDecoratedItems = new MyDecoratedVariants(variants, defaultItem, appendNull);
    myVerify = verify;
    myExternalValue = TypedKey.create(myValueKey.getDebugName() + "/external");
  }

  @Override
  public DBAttribute<Long> getAttribute() {
    return myValueKey.getAttribute();
  }

  @Override
  public String toString() {
    return "SingleEnumEditor: " + myValueKey.getDebugName();
  }

  @Override
  public void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
    prepareModel(source, model, model.getEditingItems());
  }

  public void prepareModel(VersionSource source, EditItemModel model, LongList editItems) {
    EngineConsts.ensureGuiFeatureManager(source, model);
    model.registerEditor(this);
    myValueKey.registerAttribute(model);
    myDecoratedItems.prepare(source, model);
    if (editItems.isEmpty()) {
      AttributeMap defaults = model.getValue(EditItemModel.DEFAULT_VALUES);
      Long defaultValue = defaults != null ? defaults.get(myValueKey.getAttribute()) : null;
      if (defaultValue == null) {
        myDecoratedItems.readDB(source, model);
        myValueKey.setNoInitialValue(model);
      } else myValueKey.loadValueCommonValue(model, defaultValue);
    } else myValueKey.loadValue(source, editItems, model, isForbidNull());
  }

  @NotNull
  public EnumVariantsSource getVariants() {
    return myDecoratedItems;
  }

  @Override
  public boolean isChanged(EditItemModel model) {
    if (!myValueKey.isChanged(model)) return false;
    Long extValue = model.getValue(myExternalValue);
    return extValue == null || extValue != myValueKey.getItemValue(model);
  }

  @Override
  public boolean hasDataToCommit(EditItemModel model) {
    return super.hasDataToCommit(model) || myValueKey.isChanged(model);
  }

  @Override
  public void onItemsChanged(EditItemModel model, TLongObjectHashMap<ItemValues> newValues) {
    FieldEditorUtil.assertNotChanged(model, newValues, myValueKey.getAttribute(), this);
  }

  @Override
  public final void commit(CommitContext context) throws CancelCommitException {
    myValueKey.commit(context, getVariants());
    updateDefaults(context);
  }

  public void updateDefaults(CommitContext context) throws CancelCommitException {
    AttributeMap defaults = context.getModel().getValue(EditItemModel.DEFAULT_VALUES);
    if (defaults != null) {
      long item = myValueKey.findOrCreate(context);
      defaults.put(myValueKey.getAttribute(), item > 0 ? item : 0);
    }
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
    EditModelState model = verifyContext.getModel();
    ItemKey value = myValueKey.getCurrentValue(model);
    if (myVerify) {
      if (value == null) {
        if (!myDecoratedItems.isAppendNull()) verifyContext.addError(this, "Value is empty");
        return;
      }
      long item = value.getItem();
      if (item <= 0) return;
    }
    List<ItemKey> invalids = myDecoratedItems.selectInvalid(model, Collections.singleton(value));
    if (invalids.isEmpty()) return;
    verifyContext.addError(this, invalids.get(0).getDisplayName() + " is not allowed");
  }

  protected ItemKey getInitialItem(EditModelState editModel, AListModel<? extends ItemKey> listModel) {
    ItemKey enumItem = myValueKey.getValue(editModel, myDecoratedItems);
    return enumItem != null ? enumItem : myDecoratedItems.selectDefaultItem(editModel, listModel);
  }

  protected DefaultItemSelector getDefaultItem() {
    return myDecoratedItems;
  }

  public ItemKey getValue(EditModelState model) {
    ItemKey enumItem = myValueKey.getValue(model, myDecoratedItems);
    if(enumItem == null && myDecoratedItems.isAppendNull()) enumItem = NULL_ITEM;
    return enumItem;
  }

  protected ComponentControl.Enabled getComponentEnableState(EditModelState model) {
    return myValueKey.getComponentEnableState(model, isForbidNull());
  }

  private boolean isForbidNull() {
    return myVerify && !myDecoratedItems.isAppendNull();
  }
  
  @SuppressWarnings("SimplifiableIfStatement")
  public boolean isDifferentNotNullInitial(EditModelState model) {
    if (model.getEditingItems().size() < 2) return false;
    if (myValueKey.hasInitialNull(model)) return false;
    return myValueKey.getInitialCommonValue(model) == 0;
  }

  public void connectCB(Lifespan life, final EditItemModel model, final AComboBox<?> combo, final RecentController<ItemKey> recents) {
    PopupMenuListener listener = AttachComboBox.connect(this, life, model, combo.getModel(), recents, myDecoratedItems);
    combo.getCombobox().addPopupMenuListener(listener);
  }

  public void setValue(EditModelState model, @Nullable ItemKey enumItem) {
    myValueKey.setValue(model, enumItem);
  }

  @Nullable
  public ItemKey getCurrentValue(EditModelState model) {
    return myValueKey.getCurrentValue(model);
  }

  /**
   * Set the value and assume it not changed. This method sets the value so the editor {@link FieldEditor#hasDataToCommit(com.almworks.items.gui.edit.EditItemModel) has data to commit}, but
   * it is not {@link FieldEditor#isChanged(com.almworks.items.gui.edit.EditItemModel) changed}
   * @see #setValue(com.almworks.items.gui.edit.EditModelState, com.almworks.api.application.ItemKey)
   * @see #setValueItem(com.almworks.items.gui.edit.EditModelState, Long)
   */
  public void setExternalValue(EditModelState model, Long item, @Nullable ItemKey enumItem) {
    if (model == null) {
      LogHelper.error("Null model", this, enumItem);
      return;
    }
    if (item == null || item <= 0) item = 0l;
    if (item == myValueKey.getItemValue(model)) return;
    myValueKey.setValue(model, item, enumItem);
    model.putHint(myExternalValue, item);
  }

  public void setValueItem(EditModelState model, Long item) {
    if (item == null || item <= 0) {
      myValueKey.setValue(model, null);
      return;
    }
    LoadedItemKey itemKey = myDecoratedItems.getResolvedItem(model, item);
    myValueKey.setValue(model, item, itemKey);
  }

  @Override
  public boolean hasValue(EditModelState model) {
    return myValueKey.hasValue(model);
  }

  public void syncValue(Lifespan life, final EditItemModel model, final Configuration config, final String setting, @Nullable ItemKey defaultItem, DBStaticObject ... types) {
    String strItemKey = config.getSetting(setting, "");
    ItemKey configItem = null;
    try {
      long itemKey = Long.parseLong(strItemKey);
      EnumTypesCollector enumTypes = EngineConsts.getGuiFeaturesManager(model).getEnumTypes();
      for (DBStaticObject type : types) {
        configItem = enumTypes.getType(type).getResolvedItem(itemKey);
        if (configItem != null) break;
      }
    } catch (NumberFormatException e) {
      // ignore
    }
    if (configItem == null) configItem = defaultItem;
    setValue(model, configItem);
    final ItemKey finalConfigItem = configItem;
    model.addAWTChangeListener(life, new ChangeListener() {
      private long myLastItem = finalConfigItem != null ? finalConfigItem.getItem() : 0;

      @Override
      public void onChange() {
        ItemKey current = getCurrentValue(model);
        long item = current != null ? current.getItem() : 0;
        item = Math.max(0, item);
        if (item == myLastItem) return;
        if (!model.isEnabled(BaseSingleEnumEditor.this) || hasErrors(model)) return;
        config.setSetting(setting, item);
        myLastItem = item;
      }
    });
  }

  public static void wrapperUpdateDefaults(CommitContext context, DelegatingFieldEditor<? extends BaseSingleEnumEditor> editor) throws CancelCommitException {
    DelegatingFieldEditor.ModelWrapper<? extends BaseSingleEnumEditor> wrapper = editor.getWrapperModel(context.getModel());
    if (wrapper != null) wrapper.getEditor().updateDefaults(context);
  }

  private class MyDecoratedVariants implements EnumVariantsSource, DefaultItemSelector {
    private final EnumVariantsSource myVariants;
    private final DefaultItemSelector myDefaultItem;
    private final boolean myAppendNull;

    public MyDecoratedVariants(EnumVariantsSource variants, @Nullable DefaultItemSelector defaultItem, boolean appendNull) {
      myVariants = variants;
      myDefaultItem = defaultItem != null ? defaultItem : DefaultItemSelector.ALLOW_EMPTY;
      myAppendNull = appendNull;
    }

    @Override
    public void configure(final Lifespan life, EditItemModel model, final VariantsAcceptor<ItemKey> acceptor) {
      myVariants.configure(life, model, new VariantsAcceptor<ItemKey>() {
        @Override
        public void accept(AListModel<? extends ItemKey> variants, @Nullable Configuration recentConfig) {
          if (variants != null) {
            LogHelper.assertError(variants.indexOf(null) < 0, "Null variant", myValueKey);
            if (myAppendNull) variants = SegmentedListModel.create(life, FixedListModel.create(NULL_ITEM), variants);
          }
          acceptor.accept(variants, recentConfig);
        }
      });
    }

    public boolean isAppendNull() {
      return myAppendNull;
    }

    @Override
    public ItemKey selectDefaultItem(EditModelState model, AListModel<? extends ItemKey> variants) {
      ItemKey enumItem = myDefaultItem.selectDefaultItem(model, variants);
      if(enumItem == null && myAppendNull) enumItem = NULL_ITEM;
      return enumItem;
    }

    @Override
    public void readDB(VersionSource source, EditModelState model) {
      myDefaultItem.readDB(source, model);
    }

    @Override
    public void prepare(VersionSource source, EditModelState model) {
      myVariants.prepare(source, model);
    }

    @Override
    public LoadedItemKey getResolvedItem(EditModelState model, long item) {
      return myVariants.getResolvedItem(model, item);
    }

    @NotNull
    @Override
    public List<ItemKey> selectInvalid(EditModelState model, Collection<? extends ItemKey> items) {
      return myVariants.selectInvalid(model, items);
    }

    @Override
    public boolean isValidValueFor(CommitContext context, long itemValue) {
      return myVariants.isValidValueFor(context, itemValue);
    }
  }
}
