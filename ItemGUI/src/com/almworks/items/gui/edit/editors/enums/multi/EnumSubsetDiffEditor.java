package com.almworks.items.gui.edit.editors.enums.multi;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemKeyStub;
import com.almworks.api.application.ItemOrder;
import com.almworks.api.engine.Connection;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.api.syncreg.ItemHypercubeUtils;
import com.almworks.integers.LongList;
import com.almworks.integers.LongListIterator;
import com.almworks.integers.LongOpenHashSet;
import com.almworks.integers.LongSetBuilder;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.enums.EnumItemCreator;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.items.gui.edit.editors.enums.VariantsAcceptor;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.util.BaseFieldEditor;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.edit.util.SimpleComponentControl;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.SegmentedListModel;
import com.almworks.util.advmodel.SubsetModel;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.PrimitiveUtils;
import com.almworks.util.commons.Function;
import com.almworks.util.commons.Function2;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.SubsetEditor;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.NameMnemonic;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.almworks.util.collections.Functional.foldl;
import static java.util.Collections.singletonList;
import static org.almworks.util.Collections15.arrayList;

public class EnumSubsetDiffEditor extends BaseFieldEditor {
  private final DBAttribute<Set<Long>> myAttribute;
  private final EnumVariantsSource myVariants;

  private final TypedKey<TLongObjectHashMap<LongList>> myOriginalDbValues;
  private final TypedKey<List<ItemKey>> myAdded;
  private final TypedKey<List<ItemKey>> myRemoved;

  @NotNull
  private final EnumItemCreator myEnumItemCreator;
  @Nullable
  private final CanvasRenderer<ItemKey> myOverrideRenderer;

  public EnumSubsetDiffEditor(NameMnemonic labelText, DBAttribute<Set<Long>> attribute, EnumVariantsSource variants, @NotNull EnumItemCreator enumItemCreator, @Nullable CanvasRenderer<ItemKey> overrideRenderer) {
    super(labelText);
    myAttribute = attribute;
    myVariants = variants;
    myEnumItemCreator = enumItemCreator;
    myOverrideRenderer = overrideRenderer;
    myOriginalDbValues = TypedKey.create(attribute.getName() + "/db");
    myAdded = TypedKey.create(attribute.getName() + "/added");
    myRemoved = TypedKey.create(attribute.getName() + "/removed");
  }

  protected DBAttribute<?> getAttribute() {
    return myAttribute;
  }

  @Override
  public void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
    model.registerEditor(this);
    myVariants.prepare(source, model);
    LongList items = model.getEditingItems();
    TLongObjectHashMap<LongList> originalDbValues = new TLongObjectHashMap<LongList>(items.size());
    for (ItemVersion item : source.readItems(items)) {
      originalDbValues.put(item.getItem(), PrimitiveUtils.collect(Convertor.<Long>identity(), item.getValue(myAttribute)));
    }
    model.putHint(myOriginalDbValues, originalDbValues);
    model.putHint(myAdded, new ArrayList<ItemKey>());
    model.putHint(myRemoved, new ArrayList<ItemKey>());
  }

  @NotNull
  protected List<ItemKey> getAdded(EditModelState model) {
    return getList(model, myAdded);
  }

  @NotNull
  protected List<ItemKey> getRemoved(EditModelState model) {
    return getList(model, myRemoved);
  }

  @NotNull
  private List<ItemKey> getList(EditModelState model, TypedKey<List<ItemKey>> key) {
    List<ItemKey> list = model.getValue(key);
    return list != null ? Collections.unmodifiableList(list) : Collections.<ItemKey>emptyList();
  }

  @Override
  public boolean isChanged(EditItemModel model) {
    return !getAdded(model).isEmpty() || !getRemoved(model).isEmpty();
  }

  @Override
  public boolean hasValue(EditModelState model) {
    TLongObjectHashMap<LongList> orig = model.getValue(myOriginalDbValues);
    return orig != null && orig.size() > 0;
  }

  @Override
  public void onItemsChanged(EditItemModel model, TLongObjectHashMap<ItemValues> newValues) {
    FieldEditorUtil.assertNotChanged(model, newValues, myAttribute, this);
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
    EditModelState model = verifyContext.getModel();
    List<ItemKey> added = getAdded(model);
    List<ItemKey> invalids = myVariants.selectInvalid(model, added);
    String invalidText = createInvalidValuesText(model, invalids);
    if (!invalidText.isEmpty()) verifyContext.addError(this, invalidText);
  }

  private String createInvalidValuesText(final EditModelState model, List<ItemKey> invalidValues) {
    return foldl(invalidValues, new StringBuilder(), new Function2<StringBuilder, ItemKey, StringBuilder>() {
      @Override
      public StringBuilder invoke(StringBuilder sb, ItemKey itemKey) {
        if (sb.length() == 0) {
          NameMnemonic editorName = getLabelText(model);
          sb.append("The following values for ").append(editorName.getText()).append(" are invalid: ");
        } else sb.append(", ");
        return sb.append(itemKey.getDisplayName());
      }
    }).toString();
  }

  @Override
  public void commit(CommitContext context) throws CancelCommitException {
    LongOpenHashSet resultSet = new LongOpenHashSet();
    resultSet.addAll(context.readTrunk().getLongSet(myAttribute));
    for (ItemKey key : getAdded(context.getModel())) {
      resultSet.add(resolve(context, key, true));
    }
    for (ItemKey key : getRemoved(context.getModel())) {
      resultSet.remove(resolve(context, key, false));
    }
    resultSet.remove(0);
    context.getCreator().setSet(myAttribute, resultSet.toArray());
  }

  private long resolve(CommitContext context, ItemKey key, boolean createMissing) throws CancelCommitException {
    long item = key.getItem();
    if (item > 0) return item;
    if (!createMissing) return 0;
    return myEnumItemCreator.createItem(context, key.getId());
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, final EditItemModel model) {
    TLongObjectHashMap<LongList> originalDbValues = model.getValue(myOriginalDbValues);
    LongSetBuilder allValues = new LongSetBuilder();
    for (Object obj : originalDbValues.getValues()) {
      LongList list = (LongList) obj;
      allValues.addAll(list);
    }
    // todo JC-20 (SNOW)
    Connection connection = model.getValue(EngineConsts.VALUE_CONNECTION);
    ItemHypercubeImpl connectionCube = connection != null ? ItemHypercubeUtils.createConnectionCube(connection) : new ItemHypercubeImpl();
//    AListModel<LoadedItemKey> allVariants = SortedListDecorator.create(life, myVariants.getValueModel(life, model, connectionCube), ItemKey.DISPLAY_NAME_ORDER);
    final SegmentedListModel<ItemKey> allVariants = SegmentedListModel.create(life, (AListModel<ItemKey>)AListModel.EMPTY);
    myVariants.configure(life, model, new VariantsAcceptor<ItemKey>() {
      @Override
      public void accept(AListModel<? extends ItemKey> variants, @Nullable Configuration recentConfig) {
        allVariants.setSegment(0, variants);
      }
    });
    final SubsetModel<ItemKey> subset = SubsetModel.<ItemKey>create(life, allVariants, false);
    subset.addFromComplementSet(loadKeys(model, myVariants, allValues.commitToArray()));
    life.add(subset.addListener(new AListModel.Listener<ItemKey>() {
      @Override
      public void onInsert(int index, int length) {
        List<ItemKey> added = Collections15.arrayList(getAdded(model));
        List<ItemKey> removed = Collections15.arrayList(getRemoved(model));
        for (int i = 0; i < length; ++i) {
          ItemKey key = subset.getAt(index + i);
          added.add(key);
          removed.remove(key);
        }
        model.putValues(myAdded, added, myRemoved, removed);
      }

      @Override
      public void onRemove(int index, int length, AListModel.RemovedEvent<ItemKey> event) {
        List<ItemKey> added = Collections15.arrayList(getAdded(model));
        List<ItemKey> removed = Collections15.arrayList(getRemoved(model));
        for (ItemKey key : event.getAllRemoved()) {
          added.remove(key);
          removed.add(key);
        }
        model.putValues(myAdded, added, myRemoved, removed);
      }

      @Override
      public void onListRearranged(AListModel.AListEvent event) {}
      @Override
      public void onItemsUpdated(AListModel.UpdateEvent event) {}
    }));
    SubsetEditor<ItemKey> editor = SubsetEditor.create(subset, false, null, ItemKey.DISPLAY_NAME, new Function<String, ItemKey>() {
      @Override
      public ItemKey invoke(String id) {
        return new ItemKeyStub(id, id, ItemOrder.byString(id));
      }
    });
    if (myOverrideRenderer != null) editor.setCanvasRenderer(myOverrideRenderer);
    FieldEditorUtil.registerComponent(model, this, editor.getComponent());
    return singletonList(SimpleComponentControl.create(editor.getComponent(), ComponentControl.Dimensions.WIDE, this, model, ComponentControl.Enabled.ALWAYS_ENABLED));
  }

  private static List<ItemKey> loadKeys(EditItemModel model, EnumVariantsSource variants, LongList items) {
    List<ItemKey> result = arrayList(items.size());
    for (LongListIterator i = items.iterator(); i.hasNext();) {
      LoadedItemKey key = variants.getResolvedItem(model, i.nextValue());
      if (key != null) result.add(key);
    }
    return result;
  }
}
