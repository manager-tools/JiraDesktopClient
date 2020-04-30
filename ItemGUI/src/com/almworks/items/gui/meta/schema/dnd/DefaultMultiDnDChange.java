package com.almworks.items.gui.meta.schema.dnd;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.ModelKey;
import com.almworks.explorer.qbuilder.filter.BaseEnumConstraintDescriptor;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.items.gui.edit.editors.enums.multi.BaseMultiEnumEditor;
import com.almworks.items.gui.edit.editors.enums.multi.CompactEnumSubsetEditor;
import com.almworks.items.gui.edit.editors.enums.multi.MultiEnumEditor;
import com.almworks.items.gui.edit.editors.enums.multi.MultiEnumValueKey;
import com.almworks.items.gui.edit.util.BaseFieldEditor;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.gui.meta.schema.applicability.Applicability;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.util.components.CompactSubsetEditor;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.text.TextUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

class DefaultMultiDnDChange extends DefaultDnDChange<List<ItemKey>> {
  private final DBAttribute<Set<Long>> myAttribute;

  public DefaultMultiDnDChange(DBAttribute<Set<Long>> attribute, DnDVariants variants, long modelKey, long constraint, Applicability applicability) {
    super(variants, modelKey, constraint, applicability);
    myAttribute = attribute;
  }

  @Override
  protected void prepare(DnDApplication application, TargetValues target, ModelKey<List<ItemKey>> modelKey, BaseEnumConstraintDescriptor constraint) {
    NameMnemonic labelText = NameMnemonic.rawText(constraint.getDisplayName());
    boolean allMatches = true;
    boolean canAdd = false;
    for (ItemWrapper wrapper : application.getItems()) {
      List<ItemKey> values = wrapper.getModelKeyValue(modelKey);
      if (target.matches(values)) {
        if (!canAdd && target.mayAddToValues(values)) canAdd = true;
        continue;
      }
      allMatches = false;
      break;
    }
    boolean isMove = application.isMove();
    if (allMatches && (!canAdd || isMove)) return;
    Long singlePositive = target.getSinglePositive();
    ItemKey empty = getMissingKey(constraint);
    if (singlePositive != null) {
      if (checkSinglePositive(application, labelText.getText(), singlePositive, empty)) {
        DnDFieldEditor editor = isMove ?
          new BaseNotification.Single(labelText, getVariants(), myAttribute, singlePositive, empty)
          : new BaseNotification.AddMulti(labelText, getVariants(), myAttribute, LongList.EMPTY, LongArray.create(singlePositive));
        application.addChangeEditor(editor, false);
      }
    } else {
      LongList included = target.getIncluded();
      if (included.isEmpty() && target.isAllowsEmpty())
        application.addChangeEditor(new BaseNotification.AddMulti(labelText, getVariants(), myAttribute, target.getExcluded(), included), false);
      else application.addChangeEditor(new Editor(labelText, this, target, !isMove), true);
    }
  }

  @Override
  protected DBAttribute<Set<Long>> getAttribute() {
    return myAttribute;
  }

  protected LoadedModelKey<List<ItemKey>> getModelKey(DnDApplication application, long modelKey) {
    return application.multiModelKey(modelKey);
  }

  private static class Editor extends BaseFieldEditor implements MultiEnumEditor, DnDFieldEditor {
    private final DefaultMultiDnDChange myChange;
    private final TypedKey<EnumVariantsSource> myVariantsKey;
    private final MultiEnumValueKey myKey;
    private final TargetValues myTarget;
    private final boolean myAddValues;

    private Editor(NameMnemonic labelText, DefaultMultiDnDChange change, TargetValues target, boolean addValues) {
      super(labelText);
      myAddValues = addValues;
      myVariantsKey = TypedKey.create(labelText + "/variants");
      myChange = change;
      myTarget = target;
      myKey = new MultiEnumValueKey(change.getAttribute());
    }

    @Override
    public String getDescription(ActionContext context, boolean full) throws CantPerformException {
      return getLabelText().getText();
    }

    @Override
    public void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
      if (!myChange.isApplicableToAnyItem(source.readItems(model.getEditingItems()))) return;
      DnDVariants variants = myChange.getVariants();
      variants.prepare(source, model);
      EnumVariantsSource variantsSource = variants.filterVariants(model, myTarget.getVariantsFilter(true));
      model.putHint(myVariantsKey, variantsSource);
      myKey.loadValue(source, model);
      model.registerEditor(this);
    }

    @Override
    public void afterModelFixed(EditItemModel model) {
      LongList included = myTarget.getIncluded();
      if (!included.isEmpty()) {
        myKey.setValue(model, included);
        myKey.setAllCommonValue(model, true);
      }
    }

    @NotNull
    @Override
    public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
      EnumVariantsSource variantsSource = model.getValue(myVariantsKey);
      if (variantsSource == null) return Collections.emptyList();
      CompactSubsetEditor<ItemKey> component = CompactEnumSubsetEditor.createComponent();
      component.setCanvasRenderer(myChange.getVariants().getRenderer(model));
      ComponentControl componentControl = CompactEnumSubsetEditor.attachComponent(life, this, variantsSource, model, component, ComponentControl.Enabled.ALWAYS_ENABLED, null);
      return Collections.singletonList(componentControl);
    }

    @Override
    public boolean isChanged(EditItemModel model) {
      return myKey.isChanged(model);
    }

    @Override
    public boolean hasValue(EditModelState model) {
      return myKey.hasValue(model);
    }

    @Override
    public void verifyData(DataVerification verifyContext) {
      if (!myChange.isApplicableTo(verifyContext.getModel())) return;
      EditModelState model = verifyContext.getModel();
      EnumVariantsSource variantsSource = model.getValue(myVariantsKey);
      if (variantsSource == null) return;
      List<ItemKey> items = myKey.getItemKeysValue(model, variantsSource);
      if (items.isEmpty()) {
        if (!myTarget.isAllowsEmpty()) verifyContext.addError(this, "Empty value does not satisfy query");
      } else {
        boolean hasIncluded = false;
        List<ItemKey> forbidden = Collections15.arrayList();
        for (ItemKey key : items) {
          long item = key.getItem();
          if (item <= 0) continue;
          if (!hasIncluded && myTarget.getIncluded().contains(item)) hasIncluded = true;
          if (myTarget.getExcluded().contains(item)) forbidden.add(key);
        }
        if (hasIncluded && forbidden.isEmpty()) return;
        StringBuilder message = new StringBuilder();
        if (!hasIncluded) message.append("No value satisfies query.");
        if (!forbidden.isEmpty()) {
          if (message.length() > 0) message.append(" ");
          message.append("Query rejects: ").append(TextUtil.separateToString(forbidden, ","));
        }
        verifyContext.addError(this, message.toString());
      }
    }

    @Override
    public void commit(CommitContext context) throws CancelCommitException {
      if (!myChange.isApplicableTo(context.readTrunk())) return;
      EditItemModel model = context.getModel();
      LongList newValue = myKey.getSelectedItems(model);
      if (myAddValues) {
        LongList current = context.readTrunk().getLongSet(myKey.getAttribute());
        LongArray copy = LongArray.copy(newValue);
        copy.addAll(current);
        copy.sortUnique();
        newValue = copy;
      }
      BaseMultiEnumEditor.commit(context, myKey.getAttribute(), newValue, myChange.getVariants().getVariants(model));
    }

    @Override
    public void onItemsChanged(EditItemModel model, TLongObjectHashMap<ItemValues> newValues) {
      FieldEditorUtil.assertNotChanged(model, newValues, myKey.getAttribute(), this);
    }

    @NotNull
    @Override
    public List<ItemKey> getValue(EditModelState model) {
      EnumVariantsSource variantsSource = model.getValue(myVariantsKey);
      if (variantsSource == null) return Collections.emptyList();
      List<ItemKey> values = myKey.getItemKeysValue(model, variantsSource);
      List<ItemKey> invalid = variantsSource.selectInvalid(model, values);
      if (!invalid.isEmpty()) {
        LongArray invalidItems = LongArray.create(ItemKey.GET_ITEM.collectList(invalid));
        invalidItems.sortUnique();
        values = Collections15.arrayList(values);
        for (Iterator<ItemKey> it = values.iterator(); it.hasNext(); ) {
          ItemKey itemKey = it.next();
          if (invalidItems.binarySearch(itemKey.getItem()) >= 0) it.remove();
        }
      }
      return values;
    }

    @Override
    public void setValue(EditModelState model, List<ItemKey> itemKeys) {
      myKey.setValue(model, itemKeys);
    }
  }
}
