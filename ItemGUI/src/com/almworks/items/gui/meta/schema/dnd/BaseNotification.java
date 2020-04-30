package com.almworks.items.gui.meta.schema.dnd;

import com.almworks.api.application.ItemKey;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.integers.LongListIterator;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.items.gui.edit.util.BaseFieldEditor;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.edit.util.SimpleComponentControl;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.BadUtil;
import com.almworks.util.Pair;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.PlainTextCanvas;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

abstract class BaseNotification extends BaseFieldEditor implements DnDFieldEditor {
  private final DnDVariants myVariants;

  BaseNotification(NameMnemonic labelText, DnDVariants variants) {
    super(labelText);
    myVariants = variants;
  }

  @Override
  public String getDescription(ActionContext context, boolean full) throws CantPerformException {
    if (!full) return getLabelText().getText();
    StringBuilder builder = new StringBuilder(getLabelText().getText());
    Pair<LongList, LongList> variants = getVariants();
    EnumTypesCollector.Loaded enumType = context.getSourceObject(GuiFeaturesManager.ROLE).getEnumTypes().getType(myVariants.getEnumType());
    if (enumType != null) {
      String add = getVariantsNames(enumType, variants.getFirst());
      String remove = getVariantsNames(enumType, variants.getSecond());
      if (add == null && remove == null) return builder.toString();
      builder.append(" (");
      if (remove != null && add != null) builder.append("+:");
      if (add != null) {
        builder.append(add);
        if (remove != null) builder.append(". ");
      }
      if (remove != null) builder.append("-:").append(remove);
      builder.append(")");
    }
    return builder.toString();
  }
  
  @Nullable
  private String getVariantsNames(EnumTypesCollector.Loaded enumType, LongList variants) {
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (LongIterator cursor : variants) {
      long item = cursor.value();
      LoadedItemKey key = enumType.getResolvedItem(item);
      if (key == null) continue;
      if (!first) builder.append(",");
      first = false;
      builder.append(key.getDisplayName());
    }
    String result = builder.toString();
    return result.isEmpty() ? null : result;
  }

  protected abstract Pair<LongList, LongList> getVariants();

  @Override
  public void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
    myVariants.prepare(source, model);
    model.registerEditor(this);
  }

  @Nullable
  protected ItemKey getItemKey(EditItemModel model, long item) {
    if (item > 0) {
      EnumVariantsSource variants = myVariants.getVariants(model);
      if (variants != null) {
        LoadedItemKey key = variants.getResolvedItem(model, item);
        if (key != null) return key;
      }
    }
    return null;
  }

  @Override
  public boolean isChanged(EditItemModel model) {
    return true;
  }

  @Override
  public boolean hasValue(EditModelState model) {
    return true;
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
  }

  @Override
  public void onItemsChanged(EditItemModel model, TLongObjectHashMap<ItemValues> newValues) {
  }

  protected List<ComponentControl> createLabel(EditItemModel model, String text) {
    return Collections.singletonList(createLine(model, text));
  }

  protected ComponentControl createLine(EditItemModel model, String text) {
    JTextField field = new JTextField(text);
    field.setEditable(false);
    return SimpleComponentControl.singleLine(field, this, model, ComponentControl.Enabled.ALWAYS_ENABLED);
  }

  protected EnumVariantsSource getVariantsSource(EditModelState model) {
    return myVariants.getVariants(model);
  }

  static class Single extends BaseNotification {
    private final DBAttribute<?> myAttribute;
    private final long mySetValue;
    private final ItemKey myEmpty;

    Single(NameMnemonic labelText, DnDVariants variants, DBAttribute<?> attribute, long setValue, ItemKey empty) {
      super(labelText, variants);
      myAttribute = attribute;
      mySetValue = setValue;
      myEmpty = empty;
    }

    @Override
    public String getDescription(ActionContext context, boolean full) throws CantPerformException {
      if (!full || mySetValue > 0) return super.getDescription(context, full);
      return getLabelText() + " (<none>)";
    }

    @NotNull
    @Override
    public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
      CanvasRenderable key = getItemKey(model, mySetValue);
      if (key == null) key = myEmpty;
      String text;
      text = key == null ? "" : PlainTextCanvas.renderText(key);
      return createLabel(model, text);
    }

    @Override
    public void commit(CommitContext context) {
      if (mySetValue != 0 && SyncUtils.isRemoved(SyncUtils.readTrunk(context.getReader(), mySetValue))) return;
      EnumVariantsSource variantsSource = getVariantsSource(context.getModel());
      if (mySetValue > 0 && !variantsSource.isValidValueFor(context, mySetValue)) return;
      DBAttribute<Long> single = BadUtil.castScalar(Long.class, myAttribute);
      if (single != null) commitSingle(context.getCreator(), single, mySetValue);
      else {
        DBAttribute<? extends Collection<Long>> collection = BadUtil.castCollectionAttribute(Long.class, myAttribute);
        if (collection != null) commitMulti(context.getCreator(), collection, mySetValue);
      }
    }

    private static void commitMulti(ItemVersionCreator creator, DBAttribute<? extends Collection<Long>> collection, long setValue) {
      LongList value = creator.getLongSet(collection);
      LongList newValue;
      if (setValue <= 0) {
        if (value.isEmpty()) return;
        newValue = LongList.EMPTY;
      } else {
        if (value.size() == 1 && value.get(0) == setValue) return;
        newValue = setValue > 0 ? LongArray.create(setValue) : LongList.EMPTY;
      }
      creator.setSet(collection, newValue);
    }

    private static void commitSingle(ItemVersionCreator creator, DBAttribute<Long> single, long setValue) {
      long value = creator.getNNValue(single, 0l);
      if (setValue == value) return;
      creator.setValue(single, setValue > 0 ? setValue : null);
    }

    @Override
    protected Pair<LongList, LongList> getVariants() {
      return Pair.create((LongList)LongArray.create(mySetValue), LongList.EMPTY);
    }
  }

  static class AddMulti extends BaseNotification {
    private final DBAttribute<? extends Collection<Long>> myAttribute;
    private final LongList myClear;
    private final LongList myAdd;

    AddMulti(NameMnemonic labelText, DnDVariants variants, DBAttribute<? extends Collection<Long>> attribute, LongList clear, LongList add) {
      super(labelText, variants);
      myAttribute = attribute;
      myClear = clear;
      myAdd = add;
    }

    @NotNull
    @Override
    public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
      String remove = collectText(model, "Remove ", myClear);
      String add = collectText(model, "Add ", myAdd);
      boolean noRemove = remove.isEmpty();
      boolean noAdd = add.isEmpty();
      if (noRemove && noAdd) return Collections.emptyList();
      else if (noRemove || noAdd) return createLabel(model, noRemove ? add : remove);
      else {
        ComponentControl addLine = createLine(model, add);
        ComponentControl removeLine = createLine(model, remove);
        return Collections15.unmodifiableListCopy(addLine, removeLine);
      }
    }

    @Override
    protected Pair<LongList, LongList> getVariants() {
      return Pair.create(myAdd, myClear);
    }

    private String collectText(EditItemModel model, String label, LongList items) {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < items.size(); i++) {
        ItemKey key = getItemKey(model, items.get(i));
        if (builder.length() > 0) builder.append(",");
        if (key != null) builder.append(PlainTextCanvas.renderText(key));
      }
      if (builder.length() == 0) return "";
      builder.insert(0, label);
      return builder.toString();
    }

    @Override
    public void commit(CommitContext context) {
      EnumVariantsSource variantsSource = getVariantsSource(context.getModel());
      LongList current = context.readTrunk().getLongSet(myAttribute);
      LongArray newValue = LongArray.copy(current);
      newValue.addAll(myAdd);
      newValue.removeAll(myClear);
      LongList filtered = FieldEditorUtil.filterOutRemoved(context.getReader(), newValue);
      if (variantsSource != null) {
        LongListIterator it = filtered.iterator();
        while (it.hasNext()) {
          long value = it.nextValue();
          if (!variantsSource.isValidValueFor(context, value)) it.remove();
        }
      }
      if (!filtered.equals(current)) context.getCreator().setSet(myAttribute, filtered);
    }
  }
}
