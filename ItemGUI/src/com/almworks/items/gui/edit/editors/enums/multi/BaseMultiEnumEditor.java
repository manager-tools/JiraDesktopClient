package com.almworks.items.gui.edit.editors.enums.multi;

import com.almworks.api.application.ItemKey;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.items.gui.edit.util.BaseFieldEditor;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.items.util.AttributeMap;
import com.almworks.util.text.NameMnemonic;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class BaseMultiEnumEditor extends BaseFieldEditor implements MultiEnumEditor {
  private final MultiEnumValueKey myKey;
  private final EnumVariantsSource myVariants;

  public BaseMultiEnumEditor(NameMnemonic labelText, DBAttribute<Set<Long>> attribute, EnumVariantsSource variants) {
    super(labelText);
    myVariants = variants;
    myKey = new MultiEnumValueKey(attribute);
  }

  public DBAttribute<Set<Long>> getAttribute() {
    return myKey.getAttribute();
  }

  public EnumVariantsSource getVariants() {
    return myVariants;
  }

  @Override
  public void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
    model.registerEditor(this);
    myVariants.prepare(source, model);
    myKey.loadValue(source, model);
  }

  protected boolean isAllCommonValues(EditModelState model) {
    return myKey.isAllCommonValues(model);
  }

  protected ComponentControl.Enabled getComponentEnabledState(EditModelState model) {
    if (model.getEditingItems().size() < 2) return ComponentControl.Enabled.NOT_APPLICABLE;
    return isAllCommonValues(model) ? ComponentControl.Enabled.ENABLED : ComponentControl.Enabled.DISABLED;
  }

  public List<ItemKey> getSelectedItemKeys(EditModelState model) {
    return myKey.getSelectedItemKeys(model);
  }

  @NotNull
  public List<ItemKey> getValue(EditModelState editModel) {
    return myKey.getItemKeysValue(editModel, myVariants);
  }

  public void setValue(EditModelState model, List<ItemKey> itemKeys) {
    myKey.setValue(model, itemKeys);
  }

  public void setItemValue(EditModelState model, LongList items) {
    myKey.setValue(model, items);
  }

  @Override
  public boolean isChanged(EditItemModel model) {
    return myKey.isChanged(model);
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
    EditModelState model = verifyContext.getModel();
    List<ItemKey> items = myKey.getItemKeysValue(model, myVariants);
    List<ItemKey> invalids = getVariants().selectInvalid(model, items);
    if (invalids.isEmpty()) return;
    StringBuilder builder = new StringBuilder();
    builder.append("Not allowed ");
    builder.append(invalids.size() == 1 ? "value" : "values");
    builder.append(": ");
    String sep = "";
    for (ItemKey invalid : invalids) {
      builder.append(sep);
      sep = ",";
      builder.append(invalid.getDisplayName());
    }
    verifyContext.addError(this, builder.toString());
  }

  @Override
  public void onItemsChanged(EditItemModel model, TLongObjectHashMap<ItemValues> newValues) {
    FieldEditorUtil.assertNotChanged(model, newValues, getAttribute(), this);
  }

  @Override
  public final void commit(CommitContext context) throws CancelCommitException {
    commit(context, getAttribute(), prepareCommitValue(context), myVariants);
  }

  public static void commit(CommitContext context, DBAttribute<Set<Long>> attribute, LongList value, @Nullable EnumVariantsSource variants) {
    value = FieldEditorUtil.filterEnumValues(context, variants, value);
    context.getCreator().setSet(attribute, value);
    AttributeMap defaults = context.getModel().getValue(EditItemModel.DEFAULT_VALUES);
    if (defaults != null) {
      Set<Long> setValue = value.isEmpty() ? Collections.singleton(0l) : Collections15.hashSet(value.toList());
      defaults.put(attribute, setValue);
    }
  }

  protected LongList prepareCommitValue(CommitContext context) throws CancelCommitException {
    return myKey.getSelectedItems(context.getModel());
  }

  @Override
  public boolean hasValue(EditModelState model) {
    return myKey.hasValue(model);
  }
}
